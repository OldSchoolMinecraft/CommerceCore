package net.oldschoolminecraft.cc.contracts;

import net.oldschoolminecraft.cc.api.AccountRef;
import net.oldschoolminecraft.cc.api.NamedMutableBalance;
import net.oldschoolminecraft.cc.util.PTUtil;
import org.apache.poi.ss.formula.functions.Finance;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class LoanContract extends AbstractContract
{
    private final AccountRef lender;
    private final AccountRef borrower;
    private final double principal;       // fixed, original loan size -- for display only
    private final double interestRate;    // fraction over full term, e.g. 0.02
    private final Instant repaymentDeadline;

    private boolean fundedByLender;
    private boolean repaidByBorrower;
    private double amountRepaid;          // cumulative, for display/stats only
    private Instant fundedAt;

    private double outstandingBalance;    // what's actually still owed -- shrinks on repay(), grows on accrual
    private long daysAccruedSoFar;        // whole days of compounding already folded into outstandingBalance

    public LoanContract(NamedMutableBalance lender, NamedMutableBalance borrower, double principal, double interestRate, Instant repaymentDeadline)
    {
        super(ContractType.LOAN);

        if (principal <= 0)
            throw new IllegalArgumentException("principal must be positive");
        if (interestRate < 0)
            throw new IllegalArgumentException("interestRate cannot be negative");
        if (repaymentDeadline.isBefore(Instant.now()))
            throw new IllegalArgumentException("repaymentDeadline must be in the future");
        if (lender.equals(borrower))
            throw new IllegalArgumentException("lender and borrower must be different players");

        this.lender = AccountRef.of(lender);
        this.borrower = AccountRef.of(borrower);
        this.principal = principal;
        this.interestRate = interestRate;
        this.repaymentDeadline = repaymentDeadline;
        this.fundedByLender = false;
        this.repaidByBorrower = false;
        this.amountRepaid = 0D;
        this.outstandingBalance = principal;
        this.daysAccruedSoFar = 0L;
    }

    public void fund()
    {
        requireStatus(ContractStatus.PENDING);
        this.fundedByLender = true;
        this.fundedAt = Instant.now();
        transitionTo(ContractStatus.ACTIVE);
    }

    /**
     * Records a repayment of any size, at any time. Interest owed up to this
     * moment is folded into the outstanding balance FIRST, then the
     * repayment is subtracted -- so paying early or paying more than the
     * minimum genuinely reduces future interest, rather than being netted
     * off only at the deadline.
     */
    public void repay(double amount)
    {
        requireStatus(ContractStatus.ACTIVE, ContractStatus.DEFAULTED);
        if (amount <= 0)
            throw new IllegalArgumentException("repayment amount must be positive");

        accrueToNow();
        outstandingBalance -= amount;
        amountRepaid += amount;
        evaluate();
    }

    /** Nth root of the full-term multiplier -- constant for the loan's life. */
    private double dailyCompoundRate(long totalDays)
    {
        if (totalDays <= 0)
            return 0D;
        return Math.pow(1.0 + interestRate, 1.0 / totalDays) - 1.0;
    }

    private long daysSinceFundingCapped(long totalDays)
    {
        long daysSinceFunding = Duration.between(fundedAt, Instant.now()).toDays();
        if (daysSinceFunding < 0) daysSinceFunding = 0;
        return Math.min(daysSinceFunding, totalDays);
    }

    /** Pure read -- projects the balance with pending interest folded in, without mutating state. */
    private double projectCurrentBalance()
    {
        if (fundedAt == null || interestRate == 0D)
            return outstandingBalance;

        long totalDays = Duration.between(fundedAt, repaymentDeadline).toDays();
        if (totalDays <= 0)
            return outstandingBalance;

        long targetDays = daysSinceFundingCapped(totalDays);
        long newDays = targetDays - daysAccruedSoFar;
        if (newDays <= 0)
            return outstandingBalance;

        double dailyRate = dailyCompoundRate(totalDays);
        return Finance.fv(dailyRate, (int) newDays, 0D, -outstandingBalance, 0);
    }

    /** Mutating -- folds interest since the last checkpoint into outstandingBalance. */
    private void accrueToNow()
    {
        if (fundedAt == null || interestRate == 0D)
            return;

        long totalDays = Duration.between(fundedAt, repaymentDeadline).toDays();
        if (totalDays <= 0)
            return;

        long targetDays = daysSinceFundingCapped(totalDays);
        long newDays = targetDays - daysAccruedSoFar;
        if (newDays <= 0)
            return;

        double dailyRate = dailyCompoundRate(totalDays);
        outstandingBalance = Finance.fv(dailyRate, (int) newDays, 0D, -outstandingBalance, 0);
        daysAccruedSoFar = targetDays;
    }

    public double getCurrentAmountOwed()
    {
        return projectCurrentBalance();
    }

    public double getAccruedInterest()
    {
        double principalOnlyRemaining = Math.max(0D, principal - amountRepaid);
        return Math.max(0D, getRemainingBalance() - principalOnlyRemaining);
    }

    /** Worst case if the loan runs its full term with zero repayments -- unaffected by actual repayment activity. */
    public double getFullTermAmountOwed()
    {
        return principal * (1.0 + interestRate);
    }

    /**
     * ADVISORY ONLY -- never enforced. Suggests a fixed payment every
     * {@code intervalDays} days, against the CURRENT remaining balance, that
     * would clear the loan by the deadline, via Finance.pmt(). Repayment
     * behavior in this system is entirely unaffected by this number.
     */
    public double getSuggestedPeriodicPayment(int intervalDays)
    {
        if (intervalDays <= 0)
            throw new IllegalArgumentException("intervalDays must be positive");

        double remaining = getRemainingBalance();
        if (remaining <= 0D)
            return 0D;

        long daysLeft = Duration.between(Instant.now(), repaymentDeadline).toDays();
        if (daysLeft <= 0)
            return remaining;

        int numberOfPayments = (int) Math.max(1, Math.ceil((double) daysLeft / intervalDays));

        long totalDays = Duration.between(fundedAt != null ? fundedAt : Instant.now(), repaymentDeadline).toDays();
        double dailyRate = dailyCompoundRate(Math.max(totalDays, 1));
        double intervalRate = Math.pow(1.0 + dailyRate, intervalDays) - 1.0;

        if (intervalRate == 0D)
            return remaining / numberOfPayments; // same 0%-rate divide-by-zero Finance.fv() has

        return -Finance.pmt(intervalRate, numberOfPayments, remaining);
    }

    @Override
    public void evaluate()
    {
        if (getStatus() != ContractStatus.ACTIVE)
            return;

        accrueToNow();

        if (outstandingBalance <= 0D)
        {
            repaidByBorrower = true;
            transitionTo(ContractStatus.COMPLETED);
            return;
        }
        if (Instant.now().isAfter(repaymentDeadline))
            transitionTo(ContractStatus.DEFAULTED);
    }

    private void handleDefault()
    {
        requireStatus(ContractStatus.DEFAULTED);
        NamedMutableBalance lenderAccount = resolver().resolve(lender);
        NamedMutableBalance borrowerAccount = resolver().resolve(borrower);

        if (borrower.getKind() == AccountRef.Kind.PLAYER)
        {
            long lastLogin = PTUtil.getLastLogin(getBorrower());

            if (lastLogin == -1)
            {
                System.err.println("[CommerceCore] Failed to fetch playtime data when attempting to service loan for borrower: " + getBorrower() + "(ID: " + getContractId() + ")");
                return;
            }

            if (TimeUnit.MILLISECONDS.toDays((System.currentTimeMillis() - lastLogin)) >= 30)
            {
                try
                {
                    lenderAccount.add(getRemainingBalance());
                    borrowerAccount.subtract(getRemainingBalance());
                    repay(getRemainingBalance());
                    transitionTo(ContractStatus.COMPLETED);
                    return;
                } catch (Exception ex) {
                    System.err.println("[CommerceCore] Weird issue with processing loan repayment: " + ex.getMessage());
                }
            }
        }

        double borrowerBalance = borrowerAccount.balance();
        if (borrowerBalance > 0.0D)
        {
            lenderAccount.add(borrowerBalance);
            borrowerAccount.set(0);
            repay(borrowerBalance);
        }
    }

    public String getLender() { return lender.getName(); }
    public String getBorrower() { return borrower.getName(); }
    public NamedMutableBalance getResolvedLender() { return resolver().resolve(lender); }
    public NamedMutableBalance getResolvedBorrower() { return resolver().resolve(borrower); }
    public double getPrincipal() { return principal; }
    public double getInterestRate() { return interestRate; }
    public Instant getRepaymentDeadline() { return repaymentDeadline; }
    public boolean isFundedByLender() { return fundedByLender; }
    public boolean isRepaidByBorrower() { return repaidByBorrower; }
    public double getAmountRepaid() { return amountRepaid; }
    public double getRemainingBalance() { return Math.max(0D, projectCurrentBalance()); }
    public boolean isOverdue() { return getStatus() == ContractStatus.ACTIVE && Instant.now().isAfter(repaymentDeadline); }

    @Override
    public String toString()
    {
        return "LoanContract{" +
                "id=" + getContractId() +
                ", status=" + getStatus() +
                ", lender=" + lender +
                ", borrower=" + borrower +
                ", principal=" + principal +
                ", interestRate=" + interestRate +
                ", outstandingBalance=" + getRemainingBalance() +
                ", amountRepaid=" + amountRepaid +
                ", deadline=" + repaymentDeadline +
                '}';
    }
}