package net.oldschoolminecraft.cc.contracts;

import net.oldschoolminecraft.cc.api.AccountResolver;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Base type for any bilateral agreement tracked by the server (loans, escrow,
 * commissioned builds, etc.). Handles identity, timestamps, and status
 * transitions; subclasses implement the domain-specific rules that decide
 * when a contract completes, defaults, or otherwise changes state.
 */
public abstract class AbstractContract
{
    private final ContractType contractType;
    private final UUID contractId;
    private final long createdAt;
    private ContractStatus status;

    /**
     * Not persisted. Contracts store {@code AccountRef}s (plain, serializable
     * identity), never live {@code NamedMutableBalance} instances. This gets
     * attached by ContractManager after construction/deserialization so
     * subclasses can resolve their refs into live accounts on demand.
     */
    private transient AccountResolver accountResolver;

    protected AbstractContract(ContractType type)
    {
        this.contractType = type;
        this.contractId = UUID.randomUUID();
        this.createdAt = Instant.now().toEpochMilli();
        this.status = ContractStatus.PENDING;
    }

    public final ContractType getContractType()
    {
        return contractType;
    }

    public final UUID getContractId()
    {
        return contractId;
    }

    public final Instant getCreatedAt()
    {
        return Instant.ofEpochMilli(createdAt);
    }

    public final ContractStatus getStatus()
    {
        return status;
    }

    public final boolean isTerminal()
    {
        return switch (status)
        {
            case COMPLETED, DEFAULTED, CANCELLED -> true;
            case PENDING, ACTIVE -> false;
        };
    }

    /**
     * Re-checks this contract's own conditions and transitions status if
     * warranted (e.g. fully repaid -> COMPLETED, deadline passed -> DEFAULTED).
     * Subclasses should call this after any state-changing action, and callers
     * should also invoke it periodically for time-based conditions that no
     * action would otherwise trigger (e.g. a missed deadline with no further
     * activity).
     */
    public abstract void evaluate();

    /**
     * Called by ContractManager once, right after this contract is created
     * or loaded from disk. Must be called before any subclass logic that
     * needs to resolve an AccountRef into a live account.
     */
    public final void attachAccountResolver(AccountResolver resolver)
    {
        this.accountResolver = Objects.requireNonNull(resolver, "resolver");
    }

    protected final AccountResolver resolver()
    {
        if (accountResolver == null)
            throw new IllegalStateException("AccountResolver not attached to contract " + contractId
                    + " — did ContractManager forget to call attachAccountResolver()?");
        return accountResolver;
    }

    /**
     * Called off by mutual agreement or admin action. Not valid once the
     * contract has already reached a terminal state.
     */
    public void cancel()
    {
        requireNotTerminal("cancel");
        transitionTo(ContractStatus.CANCELLED);
    }

    protected final void transitionTo(ContractStatus newStatus)
    {
        this.status = Objects.requireNonNull(newStatus, "newStatus");
    }

    protected final void requireStatus(ContractStatus expected)
    {
        if (this.status != expected)
            throw new IllegalStateException("Expected contract " + contractId + " to be " + expected + " but was " + status);
    }

    protected final void requireStatus(ContractStatus... allowedStates)
    {
        boolean pass = false;
        for (ContractStatus expected : allowedStates)
        {
            if (status == expected)
            {
                pass = true;
                break;
            }
        }
        if (!pass) throw new IllegalStateException("Expected contract " + contractId + " to be " + Arrays.toString(allowedStates) + " but was " + status);
    }

    protected final void requireNotTerminal(String action)
    {
        if (isTerminal())
            throw new IllegalStateException("Cannot " + action + " contract " + contractId + ": already " + status);
    }
}