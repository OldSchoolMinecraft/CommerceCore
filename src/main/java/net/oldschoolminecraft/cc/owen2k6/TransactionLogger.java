package net.oldschoolminecraft.cc.owen2k6;

import net.oldschoolminecraft.cc.data.AccountTransaction;

import java.util.LinkedList;

public class TransactionLogger
{
    private LinkedList<AccountTransaction> dispatchQueue = new LinkedList<>();

    public void enqueueTransaction(AccountTransaction transaction)
    {
        //
    }

    public synchronized boolean hasNext()
    {
        return !dispatchQueue.isEmpty();
    }

    public synchronized AccountTransaction getNextTransaction()
    {
        return dispatchQueue.getFirst();
    }

    public static TransactionLogger getInstance()
    {
        return instance;
    }

    private static TransactionLogger instance = new TransactionLogger();
}
