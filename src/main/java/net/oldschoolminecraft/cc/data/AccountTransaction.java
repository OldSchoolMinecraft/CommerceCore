package net.oldschoolminecraft.cc.data;

import java.util.UUID;

public record AccountTransaction(UUID transactionID, UUID lastTransaction, Source source, Type type, double amount, long timestamp)
{
    public enum Type
    {
        ADD, SUBTRACT
    }

    public enum Source
    {
        PLAYER, SHOP, CONTRACT
    }
}
