package net.oldschoolminecraft.cc.api;

import java.io.Serializable;
import java.util.Objects;

/**
 * A lightweight, serializable pointer to an account holder (a player or a
 * business), decoupled from any live balance-manipulation implementation.
 *
 * <p>Contracts should store AccountRefs, not NamedMutableBalance instances.
 * NamedMutableBalance implementations (EssentialsAccount, BankAccount) wrap
 * live behavior (Economy calls, a Business reference) and aren't meaningful
 * once frozen to disk. An AccountRef is just "which account" — resolve it
 * back to a live NamedMutableBalance via an AccountResolver when you
 * actually need to read or mutate a balance.
 */
public final class AccountRef implements Serializable
{
    public enum Kind { PLAYER, BUSINESS }

    private final Kind kind;
    private final String name;

    private AccountRef(Kind kind, String name)
    {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.name = Objects.requireNonNull(name, "name");
    }

    public static AccountRef player(String playerName)
    {
        return new AccountRef(Kind.PLAYER, playerName);
    }

    public static AccountRef business(String businessAccountName)
    {
        return new AccountRef(Kind.BUSINESS, businessAccountName);
    }

    /**
     * Derives a ref from a live account, based on its runtime type. Lets
     * contract constructors keep accepting a plain NamedMutableBalance while
     * only ever persisting the ref.
     */
    public static AccountRef of(NamedMutableBalance account)
    {
        if (account instanceof BusinessAccount business)
            return business(business.getHolder().name + ":" + business.getAccountName());
        return player(account.getAccountName());
    }

    public Kind getKind()
    {
        return kind;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof AccountRef other)) return false;
        return kind == other.kind && name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(kind, name.toLowerCase());
    }

    @Override
    public String toString()
    {
        return kind + ":" + name;
    }
}