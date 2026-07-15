package net.oldschoolminecraft.cc.managers;

import net.oldschoolminecraft.cc.CommerceCore;
import net.oldschoolminecraft.cc.api.AccountRef;
import net.oldschoolminecraft.cc.api.AccountResolver;
import net.oldschoolminecraft.cc.api.EssentialsAccount;
import net.oldschoolminecraft.cc.api.NamedMutableBalance;
import net.oldschoolminecraft.cc.data.ExchangeOrder;
import net.oldschoolminecraft.cc.util.Database;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Owns all exchange state: order persistence, escrow of buyer funds and
 * seller items at order-placement time, and the matching loop that pairs
 * buy orders against sell orders every tick.
 *
 * <p>Escrow model: a BUY order debits {@code quantity * maxPrice} from the
 * buyer immediately, refunding the unused portion on each fill. A SELL
 * order removes {@code quantity} of the item from the seller's inventory
 * immediately, so a sell order only ever exists in the DB once the items
 * are actually in the exchange's custody — matching {@code run()} can
 * assume every open sell order is fulfillable.
 *
 * <p>Item delivery to the buyer happens immediately if they're online;
 * otherwise the delivery is queued in {@code exchange_pending_items} and
 * should be handed out next login (see {@link #collectPendingItems(AccountRef)}).
 */
public class ExchangeManager extends Thread
{
    private final CommerceCore plugin;
    private final Database dbHandle;
    private final AccountResolver accountResolver;

    private volatile boolean running = false;
    private final Object lock = new Object();

    public ExchangeManager(CommerceCore plugin) throws SQLException
    {
        this.plugin = plugin;
        this.accountResolver = plugin.getAccountResolver();
        this.dbHandle = new Database(new File(plugin.getDataFolder(), "exchange.db").getAbsolutePath());
        setName("ExchangeManager");
        setDaemon(true);
        initSchema();
    }

    private void initSchema() throws SQLException
    {
        try (Statement st = dbHandle.connection().createStatement())
        {
            st.execute("""
                CREATE TABLE IF NOT EXISTS exchange_orders (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    owner_kind TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    item_type_id INTEGER NOT NULL,
                    item_data INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    remaining INTEGER NOT NULL,
                    price REAL NOT NULL,
                    status TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_exchange_matching " +
                    "ON exchange_orders (status, type, item_type_id, item_data, price)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_exchange_owner " +
                    "ON exchange_orders (owner_kind, owner_name, status)");

            st.execute("""
                CREATE TABLE IF NOT EXISTS exchange_pending_items (
                    id TEXT PRIMARY KEY,
                    owner_kind TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    item_type_id INTEGER NOT NULL,
                    item_data INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS exchange_order_history (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    owner_kind TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    item_type_id INTEGER NOT NULL,
                    item_data INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    remaining INTEGER NOT NULL,
                    price REAL NOT NULL,
                    status TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);
        }
    }

    public void init()
    {
        running = true;
        start();
    }

    public void shutdown()
    {
        running = false;
        interrupt();
    }

    // ------------------------------------------------------------------
    // Order placement
    // ------------------------------------------------------------------

    public enum PlaceResult { OK, INSUFFICIENT_FUNDS, INSUFFICIENT_ITEMS, ERROR }

    /**
     * Places a buy order for {@code player}, debiting {@code quantity * maxPrice}
     * from their balance immediately as escrow.
     */
    public PlaceResult placeBuyOrder(Player player, int itemTypeId, short itemData, int quantity, double maxPrice)
    {
        AccountRef buyer = AccountRef.player(player.getName());
        NamedMutableBalance balance = accountResolver.resolve(buyer);

        double total = quantity * maxPrice;
        synchronized (lock)
        {
            if (!balance.hasEnough(total))
                return PlaceResult.INSUFFICIENT_FUNDS;

            if (!balance.subtract(total))
                return PlaceResult.ERROR;

            try
            {
                insertOrder(ExchangeOrder.Type.BUY, buyer, itemTypeId, itemData, quantity, maxPrice);
                return PlaceResult.OK;
            } catch (SQLException e) {
                balance.add(total); // roll back escrow
                plugin.getLogger().warning("Failed to place buy order: " + e.getMessage());
                return PlaceResult.ERROR;
            }
        }
    }

    /**
     * Places a sell order for {@code player}, removing {@code quantity} of the
     * item from their inventory immediately and holding it in escrow.
     */
    public PlaceResult placeSellOrder(Player player, int itemTypeId, short itemData, int quantity, double price)
    {
        AccountRef seller = AccountRef.player(player.getName());

        synchronized (lock)
        {
            PlayerInventory inv = player.getInventory();
            if (countMatching(inv, itemTypeId, itemData) < quantity)
                return PlaceResult.INSUFFICIENT_ITEMS;

            removeMatching(inv, itemTypeId, itemData, quantity);

            try
            {
                insertOrder(ExchangeOrder.Type.SELL, seller, itemTypeId, itemData, quantity, price);
                return PlaceResult.OK;
            } catch (SQLException e) {
                giveOrQueue(seller, player, itemTypeId, itemData, quantity); // roll back escrow
                plugin.getLogger().warning("Failed to place sell order: " + e.getMessage());
                return PlaceResult.ERROR;
            }
        }
    }

    private void insertOrder(ExchangeOrder.Type type, AccountRef owner, int itemTypeId, short itemData,
                             int quantity, double price) throws SQLException
    {
        insertOrder(type, owner, itemTypeId, itemData, quantity, price, false);
    }

    private void insertOrder(ExchangeOrder.Type type, AccountRef owner, int itemTypeId, short itemData,
                             int quantity, double price, boolean archive) throws SQLException
    {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("INSERT INTO").append(" ");
        queryBuilder.append(archive ? "exchange_order_history" : "exchange_orders").append(" ");
        queryBuilder.append("(id, type, owner_kind, owner_name, item_type_id, item_data, quantity, remaining, price, status, created_at)").append(" ");
        queryBuilder.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        String sql = queryBuilder.toString();

        try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
        {
            ps.setString(1, getRandomTruncatedUUID());
            ps.setString(2, type.name());
            ps.setString(3, owner.getKind().name());
            ps.setString(4, owner.getName());
            ps.setInt(5, itemTypeId);
            ps.setInt(6, itemData);
            ps.setInt(7, quantity);
            ps.setInt(8, quantity);
            ps.setDouble(9, price);
            ps.setString(10, archive ? ExchangeOrder.Status.FULFILLED.name() : ExchangeOrder.Status.OPEN.name());
            ps.setLong(11, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Cancellation
    // ------------------------------------------------------------------

    public boolean cancelOrder(String orderId, AccountRef requester)
    {
        synchronized (lock)
        {
            ExchangeOrder order = getOrder(orderId);
            if (order == null || order.getStatus() != ExchangeOrder.Status.OPEN)
                return false;
            if (!order.getOwner().equals(requester))
                return false;

            try
            {
                if (order.getType() == ExchangeOrder.Type.BUY)
                {
                    NamedMutableBalance balance = accountResolver.resolve(order.getOwner());
                    balance.add(order.getRemaining() * order.getPrice());
                } else {
                    Player p = Bukkit.getPlayerExact(order.getOwner().getName());
                    giveOrQueue(order.getOwner(), p, order.getItemTypeId(), order.getItemData(), order.getRemaining());
                }

                updateOrderStatus(orderId, ExchangeOrder.Status.CANCELLED, 0);
                return true;
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to cancel order #" + orderId + ": " + e.getMessage());
                return false;
            }
        }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public ExchangeOrder getOrder(String orderId)
    {
        String sql = "SELECT * FROM exchange_orders WHERE id = ?";
        try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
        {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                    return readOrder(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load order #" + orderId + ": " + e.getMessage());
        }
        return null;
    }

    public List<ExchangeOrder> getOrders(AccountRef owner, int page, int pageSize)
    {
        List<ExchangeOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM exchange_orders WHERE owner_kind = ? AND owner_name = ? " +
                "ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
        {
            ps.setString(1, owner.getKind().name());
            ps.setString(2, owner.getName());
            ps.setInt(3, pageSize);
            ps.setInt(4, Math.max(0, page - 1) * pageSize);

            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                    orders.add(readOrder(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load orders for " + owner + ": " + e.getMessage());
        }
        return orders;
    }

    private List<ExchangeOrder> getOpenOrders(ExchangeOrder.Type type, String priceOrder) throws SQLException
    {
        List<ExchangeOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM exchange_orders WHERE status = ? AND type = ? " +
                "ORDER BY price " + priceOrder + ", created_at ASC";

        try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
        {
            ps.setString(1, ExchangeOrder.Status.OPEN.name());
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                    orders.add(readOrder(rs));
            }
        }
        return orders;
    }

    private void archiveFulfilledOrders() throws SQLException
    {
        List<ExchangeOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM exchange_orders WHERE status = ?";

        try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
        {
            ps.setString(1, ExchangeOrder.Status.FULFILLED.name());
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                    orders.add(readOrder(rs));
            }
        }

        for (ExchangeOrder order : orders)
        {
            insertOrder(order.getType(), order.getOwner(), order.getItemTypeId(), order.getItemData(), order.getQuantity(), order.getPrice(), true);
        }

        sql = "DELETE FROM exchange_orders WHERE status = ?";
        try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
        {
            ps.setString(1, ExchangeOrder.Status.FULFILLED.name());
            ps.execute();
        }
    }

    private ExchangeOrder readOrder(ResultSet rs) throws SQLException
    {
        AccountRef owner = "PLAYER".equals(rs.getString("owner_kind"))
                ? AccountRef.player(rs.getString("owner_name"))
                : AccountRef.business(rs.getString("owner_name"));

        return new ExchangeOrder(
                rs.getString("id"),
                ExchangeOrder.Type.valueOf(rs.getString("type")),
                owner,
                rs.getInt("item_type_id"),
                (short) rs.getInt("item_data"),
                rs.getInt("quantity"),
                rs.getInt("remaining"),
                rs.getDouble("price"),
                ExchangeOrder.Status.valueOf(rs.getString("status")),
                rs.getLong("created_at")
        );
    }

    private void updateOrderStatus(String orderId, ExchangeOrder.Status status, int remaining) throws SQLException
    {
        String sql = "UPDATE exchange_orders SET status = ?, remaining = ? WHERE id = ?";
        try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
        {
            ps.setString(1, status.name());
            ps.setInt(2, remaining);
            ps.setString(3, orderId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Matching loop
    // ------------------------------------------------------------------

    @Override
    public void run()
    {
        while (running)
        {
            try
            {
                synchronized (lock)
                {
                    matchOrders();
                    cleanupOrders();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Exchange matching tick failed: " + e.getMessage());
            }

            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(60)); //TODO: make this interval configurable
        }
    }

    private void matchOrders() throws SQLException
    {
        List<ExchangeOrder> buyOrders = getOpenOrders(ExchangeOrder.Type.BUY, "DESC");
        if (buyOrders.isEmpty())
            return;

        List<ExchangeOrder> sellOrders = getOpenOrders(ExchangeOrder.Type.SELL, "ASC");
        if (sellOrders.isEmpty())
            return;

        for (ExchangeOrder buy : buyOrders)
        {
            if (buy.getRemaining() <= 0)
                continue;

            for (ExchangeOrder sell : sellOrders)
            {
                if (sell.getRemaining() <= 0)
                    continue;
                if (!buy.sameItem(sell))
                    continue;
                if (sell.getPrice() > buy.getPrice())
                    continue; // sell orders are sorted ascending, but keep the guard explicit
                if (buy.getOwner().equals(sell.getOwner()))
                    continue; // don't let a player trade with themselves

                int tradeQty = Math.min(buy.getRemaining(), sell.getRemaining());
                if (tradeQty <= 0)
                    continue;

                executeTrade(buy, sell, tradeQty);

                if (buy.getRemaining() <= 0)
                    break;
            }
        }
    }

    private void cleanupOrders()
    {
        // hand out queued items that couldn't be collected for whatever reason
        for (Player player : Bukkit.getOnlinePlayers())
        {
            AccountRef ref = AccountRef.of(new EssentialsAccount(player.getName()));
            collectPendingItems(ref);
        }

        try
        {
            // delete cancelled orders
            String sql = "DELETE FROM exchange_orders WHERE status = ?";
            try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
            {
                ps.setString(1, ExchangeOrder.Status.CANCELLED.name());
                ps.execute();
            }

            // archive fulfilled orders into separate table
            archiveFulfilledOrders();
        } catch (SQLException ex) {
            ex.printStackTrace(System.err);
        }
    }

    private void executeTrade(ExchangeOrder buy, ExchangeOrder sell, int quantity)
    {
        double executionPrice = sell.getPrice(); // fill at the seller's asking price
        double sellerProceeds = quantity * executionPrice;
        double buyerRefund = quantity * (buy.getPrice() - executionPrice);

        try
        {
            // 1. Pay the seller.
            NamedMutableBalance sellerBalance = accountResolver.resolve(sell.getOwner());
            sellerBalance.add(sellerProceeds);

            Player buyerPlayer = Bukkit.getPlayerExact(buy.getOwner().getName());
            Player sellerPlayer = Bukkit.getPlayer(sell.getOwner().getName());

            if (sellerPlayer != null && sellerPlayer.isOnline())
            {
                sellerPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        String.format("&aYou received &7$%.2f&a from your sell order: &7#%s",
                        sellerProceeds,
                        sell.getId())
                ));
            }

            // 2. Refund the buyer the unused portion of their max-price escrow.
            if (buyerRefund > 0)
            {
                NamedMutableBalance buyerBalance = accountResolver.resolve(buy.getOwner());
                buyerBalance.add(buyerRefund);

                if (buyerPlayer != null && buyerPlayer.isOnline())
                {
                    buyerPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            String.format("&aYou were refunded &e$%.2f&a from your buy order: &7#%s",
                            buyerRefund,
                            buy.getId())
                    ));
                }
            }

            // 3. Deliver the items to the buyer.
            giveOrQueue(buy.getOwner(), buyerPlayer, buy.getItemTypeId(), buy.getItemData(), quantity);

            if (buyerPlayer != null && buyerPlayer.isOnline())
            {
                String receivedItemsStr = plugin.getItemDb().getName(buy.getItemTypeId(), buy.getItemData()) + " x" + quantity;
                buyerPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        String.format("&aYou received &7%s&a from your buy order: &7#%s",
                        receivedItemsStr,
                        buy.getId())
                ));
            }

            // 4. Persist the new remaining/status for both orders.
            buy.setRemaining(buy.getRemaining() - quantity);
            sell.setRemaining(sell.getRemaining() - quantity);

            updateOrderStatus(buy.getId(),
                    buy.getRemaining() <= 0 ? ExchangeOrder.Status.FULFILLED : ExchangeOrder.Status.OPEN,
                    buy.getRemaining());
            updateOrderStatus(sell.getId(),
                    sell.getRemaining() <= 0 ? ExchangeOrder.Status.FULFILLED : ExchangeOrder.Status.OPEN,
                    sell.getRemaining());
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to persist trade between #" + buy.getId() +
                    " and #" + sell.getId() + ": " + e.getMessage());
            // Note: money/items already moved at this point. This is logged for manual
            // reconciliation rather than attempted auto-rollback, since partial rollback
            // after a partially-applied DB failure risks double-crediting the accounts.
        }
    }

    // ------------------------------------------------------------------
    // Item escrow helpers
    // ------------------------------------------------------------------

    private int countMatching(PlayerInventory inv, int itemTypeId, short itemData)
    {
        int count = 0;
        for (ItemStack stack : inv.getContents())
        {
            if (stack != null && stack.getTypeId() == itemTypeId && stack.getDurability() == itemData)
                count += stack.getAmount();
        }
        return count;
    }

    private void removeMatching(PlayerInventory inv, int itemTypeId, short itemData, int quantity)
    {
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length && quantity > 0; i++)
        {
            ItemStack stack = contents[i];
            if (stack == null || stack.getTypeId() != itemTypeId || stack.getDurability() != itemData)
                continue;

            int take = Math.min(quantity, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            quantity -= take;

            if (stack.getAmount() <= 0)
                contents[i] = null;
        }
        inv.setContents(contents);
    }

    /**
     * Gives items directly to {@code player} if they're online and have room;
     * otherwise queues them in {@code exchange_pending_items} for later pickup.
     * A follow-up {@code /exchange claim} command (not yet implemented) should
     * call {@link #collectPendingItems(AccountRef)} to hand these out.
     */
    private void giveOrQueue(AccountRef owner, Player onlinePlayer, int itemTypeId, short itemData, int quantity)
    {
        if (quantity <= 0)
            return;

        if (onlinePlayer != null && onlinePlayer.isOnline())
        {
            ItemStack stack = new ItemStack(itemTypeId, quantity, itemData);
            var leftover = onlinePlayer.getInventory().addItem(stack);
            int notDelivered = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            if (notDelivered > 0)
                queuePendingItems(owner, itemTypeId, itemData, notDelivered);
            return;
        }

        queuePendingItems(owner, itemTypeId, itemData, quantity);
    }

    private void queuePendingItems(AccountRef owner, int itemTypeId, short itemData, int quantity)
    {
        String sql = "INSERT INTO exchange_pending_items " +
                "(owner_kind, owner_name, item_type_id, item_data, quantity, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = dbHandle.connection().prepareStatement(sql))
        {
            ps.setString(1, owner.getKind().name());
            ps.setString(2, owner.getName());
            ps.setInt(3, itemTypeId);
            ps.setInt(4, itemData);
            ps.setInt(5, quantity);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to queue pending items for " + owner + ": " + e.getMessage());
        }
    }

    /**
     * Hands out any queued items for {@code owner} — call this on player join.
     * Returns the list of [itemTypeId, itemData, quantity] rows that couldn't
     * fit and remain queued.
     */
    public void collectPendingItems(AccountRef owner)
    {
        Player player = Bukkit.getPlayerExact(owner.getName());
        if (player == null || !player.isOnline())
            return;

        String selectSql = "SELECT * FROM exchange_pending_items WHERE owner_kind = ? AND owner_name = ?";
        String deleteSql = "DELETE FROM exchange_pending_items WHERE id = ?";

        synchronized (lock)
        {
            try (PreparedStatement ps = dbHandle.connection().prepareStatement(selectSql))
            {
                ps.setString(1, owner.getKind().name());
                ps.setString(2, owner.getName());

                try (ResultSet rs = ps.executeQuery())
                {
                    while (rs.next())
                    {
                        String rowId = rs.getString("id");
                        int itemTypeId = rs.getInt("item_type_id");
                        short itemData = (short) rs.getInt("item_data");
                        int quantity = rs.getInt("quantity");

                        ItemStack stack = new ItemStack(itemTypeId, quantity, itemData);
                        var leftover = player.getInventory().addItem(stack);
                        int delivered = quantity - leftover.values().stream().mapToInt(ItemStack::getAmount).sum();

                        if (delivered > 0)
                        {
                            try (PreparedStatement del = dbHandle.connection().prepareStatement(deleteSql))
                            {
                                del.setString(1, rowId);
                                del.executeUpdate();
                            }
                            if (delivered < quantity)
                                queuePendingItems(owner, itemTypeId, itemData, quantity - delivered);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to deliver pending items to " + owner + ": " + e.getMessage());
            }
        }
    }

    private static String getRandomTruncatedUUID()
    {
        String start = UUID.randomUUID().toString();
        String part1 = start.substring(0, 3);
        String part2 = start.substring(start.length() - 3);
        return part1 + part2;
    }
}