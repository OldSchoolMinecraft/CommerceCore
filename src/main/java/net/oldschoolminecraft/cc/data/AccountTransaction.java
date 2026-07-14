package net.oldschoolminecraft.cc.data;

import java.util.UUID;

public record AccountTransaction(UUID transactionID, UUID lastTransaction, Type type, double amount, long timestamp)
{
    public enum Type
    {
        ADD, SUBTRACT
    }
}
