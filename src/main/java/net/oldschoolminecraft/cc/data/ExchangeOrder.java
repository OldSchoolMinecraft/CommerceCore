package net.oldschoolminecraft.cc.data;

import net.oldschoolminecraft.cc.api.AccountRef;

import java.io.Serializable;
import java.util.UUID;

/**
 * A single buy or sell order sitting in the exchange.
 *
 * <p>For a BUY order, {@code price} is the maximum per-unit price the buyer
 * is willing to pay; the buyer's balance is debited {@code quantity * price}
 * up front when the order is placed, so funds are guaranteed to exist at
 * fulfillment time. Any difference between the max price and the actual
 * execution price is refunded once the order (partially or fully) fills.
 *
 * <p>For a SELL order, {@code price} is the fixed per-unit asking price; the
 * items themselves are removed from the seller's inventory and held by the
 * exchange the moment the order is placed, so a sell order is always
 * immediately fulfillable from the exchange's point of view.
 */
public final class ExchangeOrder implements Serializable
{
    public enum Type { BUY, SELL }
    public enum Status { OPEN, FULFILLED, CANCELLED }

    private final String id;
    private final Type type;
    private final AccountRef owner;
    private final int itemTypeId;
    private final short itemData;
    private final int quantity;
    private int remaining;
    private final double price;
    private Status status;
    private final long createdAt;

    public ExchangeOrder(String id, Type type, AccountRef owner, int itemTypeId, short itemData,
                         int quantity, int remaining, double price, Status status, long createdAt)
    {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.itemTypeId = itemTypeId;
        this.itemData = itemData;
        this.quantity = quantity;
        this.remaining = remaining;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId()
    {
        return id;
    }

    public Type getType()
    {
        return type;
    }

    public AccountRef getOwner()
    {
        return owner;
    }

    public int getItemTypeId()
    {
        return itemTypeId;
    }

    public short getItemData()
    {
        return itemData;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public int getRemaining()
    {
        return remaining;
    }

    public void setRemaining(int remaining)
    {
        this.remaining = remaining;
    }

    public double getPrice()
    {
        return price;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    public boolean isOpen()
    {
        return status == Status.OPEN && remaining > 0;
    }

    public boolean sameItem(ExchangeOrder other)
    {
        return itemTypeId == other.itemTypeId && itemData == other.itemData;
    }

    @Override
    public String toString()
    {
        return "ExchangeOrder{#" + id + " " + type + " " + owner + " item=" + itemTypeId + ":" + itemData
                + " qty=" + remaining + "/" + quantity + " price=" + price + " status=" + status + "}";
    }
}