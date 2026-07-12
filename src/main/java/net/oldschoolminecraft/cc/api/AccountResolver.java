package net.oldschoolminecraft.cc.api;

/**
 * Turns a persisted {@link AccountRef} back into a live, balance-mutable
 * account. Implementations decide how PLAYER and BUSINESS refs get resolved
 * (typically: wrap a name in an EssentialsAccount, or look up a BankAccount
 * from a business manager by name).
 */
public interface AccountResolver
{
    NamedMutableBalance resolve(AccountRef ref);
}