package net.oldschoolminecraft.cc.data;

import net.oldschoolminecraft.cc.api.BusinessAccount;

import java.util.ArrayList;

public class BankAccount implements BusinessAccount
{
    public Business accountHolder;
    public String accountName;

    public double balance;
    public double withdrawLimit;

    /**
     * This serves as both a list of trustees, and a map of their permission grants
     */
    public ArrayList<Trustee> trustees;

    public BankAccount(Business holder, String accountName)
    {
        this.accountHolder = holder;
        this.accountName = accountName;
        this.balance = 0;
        this.withdrawLimit = 10_000;
        this.trustees = new ArrayList<>();
    }

    public Business getHolder()
    {
        return accountHolder;
    }

    public String getAccountName()
    {
        return accountName;
    }

    public double balance()
    {
        return balance;
    }

    public boolean set(double v)
    {
        balance = v;
        return true;
    }

    public boolean add(double v)
    {
        balance += v;
        return true;
    }

    public boolean subtract(double v)
    {
        balance -= v;
        return true;
    }

    public boolean multiply(double v)
    {
        balance *= v;
        return true;
    }

    public boolean divide(double v)
    {
        balance /= v;
        return true;
    }

    public boolean hasEnough(double v)
    {
        return balance >= v;
    }

    public boolean hasOver(double v)
    {
        return balance > v;
    }

    public boolean hasUnder(double v)
    {
        return balance < v;
    }

    public boolean isNegative()
    {
        return balance < 0;
    }
}
