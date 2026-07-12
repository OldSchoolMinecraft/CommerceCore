package net.oldschoolminecraft.cc.contracts;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.oldschoolminecraft.OSMEss.Handlers.PlayerDataHandler;
import net.oldschoolminecraft.cc.CommerceCore;
import net.oldschoolminecraft.cc.api.MutableBalance;
import net.oldschoolminecraft.cc.api.NamedMutableBalance;
import net.oldschoolminecraft.cc.util.PTUtil;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * A loan between two players: the lender funds a principal, the borrower owes
 * back a (possibly larger) repayment amount by a deadline. Auto-completes as
 * soon as the full repayment amount has been paid in, and auto-defaults once
 * the deadline passes with an outstanding balance.
 *
 * <p>Amounts are in the smallest unit of the server's currency (e.g. cents),
 * as {@code long}, to avoid floating point rounding issues.
 *
 * <p>Note: {@link #evaluate()} runs automatically after {@link #repay(double)},
 * so full repayment is always caught immediately. A missed deadline with
 * <em>no</em> further repayment activity won't trigger anything on its own —
 * pair this with a periodic sweep (e.g. a repeating Bukkit task that calls
 * {@code evaluate()} on all active loans) if you want defaults detected the
 * moment the deadline passes rather than the next time someone touches it.
 */
public final class LoanContract extends AbstractContract
{
    private final NamedMutableBalance lender;
    private final NamedMutableBalance borrower;
    private final double principal;
    private final double repaymentAmount;
    private final Instant repaymentDeadline;

    private boolean fundedByLender;
    private boolean repaidByBorrower;
    private double amountRepaid;

    public LoanContract(NamedMutableBalance lender, NamedMutableBalance borrower, double principal, double repaymentAmount, Instant repaymentDeadline)
    {
        super(ContractType.LOAN);

        if (principal <= 0)
            throw new IllegalArgumentException("principal must be positive");
        if (repaymentAmount < principal)
            throw new IllegalArgumentException("repaymentAmount cannot be less than principal");
        if (repaymentDeadline.isBefore(Instant.now()))
            throw new IllegalArgumentException("repaymentDeadline must be in the future");
        if (lender.equals(borrower))
            throw new IllegalArgumentException("lender and borrower must be different players");

        this.lender = lender;
        this.borrower = borrower;
        this.principal = principal;
        this.repaymentAmount = repaymentAmount;
        this.repaymentDeadline = repaymentDeadline;
        this.fundedByLender = false;
        this.repaidByBorrower = false;
        this.amountRepaid = 0D;
    }

    /**
     * Marks the loan as funded by the lender and activates it. Call this once
     * the principal has actually been transferred to the borrower.
     */
    public void fund()
    {
        requireStatus(ContractStatus.PENDING);
        this.fundedByLender = true;
        transitionTo(ContractStatus.ACTIVE);
    }

    /**
     * Records a repayment from the borrower. Automatically completes the loan
     * if this payment brings the total repaid to (or past) the repayment
     * amount, and automatically marks it defaulted if the deadline has
     * already passed.
     *
     * @param amount amount repaid, must be positive
     */
    public void repay(double amount)
    {
        requireStatus(ContractStatus.ACTIVE);
        if (amount <= 0)
            throw new IllegalArgumentException("repayment amount must be positive");
        amountRepaid += amount;
        evaluate();
    }

    /**
     * Re-checks completion/default conditions. Safe to call repeatedly and
     * from a scheduled task; a no-op once the loan is in a terminal state.
     */
    @Override
    public void evaluate()
    {
        if (getStatus() != ContractStatus.ACTIVE)
            return;
        if (amountRepaid >= repaymentAmount)
        {
            repaidByBorrower = true;
            transitionTo(ContractStatus.COMPLETED);
            return;
        }
        if (Instant.now().isAfter(repaymentDeadline))
            transitionTo(ContractStatus.DEFAULTED);

        // handle what happens when borrower defaults on their loan
        if (getStatus() == ContractStatus.DEFAULTED)
        {
            long lastLogin = PTUtil.getLastLogin(getBorrower());

            if (lastLogin == -1) // failed to fetch playtime data, abort for now.
            {
                System.err.println("[CommerceCore] Failed to fetch playtime data when attempting to service loan for borrower: " + getBorrower() + "(ID: " + getContractId() + ")");
                return;
            }

            // if borrowers time since last login exceeds 30 days:
            // deduct the full amount (even if borrower goes negative)
            if (TimeUnit.MILLISECONDS.toDays((System.currentTimeMillis() - lastLogin)) >= 30)
            {
                try
                {
                    Economy.subtract(getBorrower(), getRemainingBalance());
                    repay(getRemainingBalance());
                    transitionTo(ContractStatus.COMPLETED);
                    return;
                } catch (Exception ex) {
                    System.err.println("[CommerceCore] Weird issue with processing loan repayment: " + ex.getMessage());
                }
            }

            try // attempt to deduct any incoming money from borrower balance util debt is repaid
            {
                double borrowerBalance = Economy.getMoney(getBorrower());
                if (borrowerBalance > 0.0D)
                {
                    Economy.setMoney(getBorrower(), 0);
                    repay(borrowerBalance);
                }
            } catch (UserDoesNotExistException | NoLoanPermittedException ignored) {}
        }
    }

    public String getLender()
    {
        return lender.getAccountName();
    }

    public String getBorrower()
    {
        return borrower.getAccountName();
    }

    public double getPrincipal()
    {
        return principal;
    }

    public double getRepaymentAmount()
    {
        return repaymentAmount;
    }

    public Instant getRepaymentDeadline()
    {
        return repaymentDeadline;
    }

    public boolean isFundedByLender()
    {
        return fundedByLender;
    }

    public boolean isRepaidByBorrower()
    {
        return repaidByBorrower;
    }

    public double getAmountRepaid()
    {
        return amountRepaid;
    }

    public double getRemainingBalance()
    {
        return Math.max(0L, repaymentAmount - amountRepaid);
    }

    public boolean isOverdue()
    {
        return getStatus() == ContractStatus.ACTIVE && Instant.now().isAfter(repaymentDeadline);
    }

    @Override
    public String toString()
    {
        return "LoanContract{" +
                "id=" + getContractId() +
                ", status=" + getStatus() +
                ", lender=" + lender +
                ", borrower=" + borrower +
                ", principal=" + principal +
                ", repaymentAmount=" + repaymentAmount +
                ", amountRepaid=" + amountRepaid +
                ", deadline=" + repaymentDeadline +
                '}';
    }
}