package net.oldschoolminecraft.cc.commands;

import net.oldschoolminecraft.cc.CommerceCore;
import net.oldschoolminecraft.cc.api.AccountRef;
import net.oldschoolminecraft.cc.api.AccountResolver;
import net.oldschoolminecraft.cc.api.EssentialsAccount;
import net.oldschoolminecraft.cc.data.ExchangeOrder;
import net.oldschoolminecraft.cc.managers.ExchangeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ExchangeCmd implements CommandExecutor
{
    private static final int ORDERS_PER_PAGE = 8;

    private final CommerceCore plugin;
    private final ExchangeManager exchangeManager;
    private final AccountResolver accountResolver;

    public ExchangeCmd(CommerceCore plugin)
    {
        this.plugin = plugin;
        this.exchangeManager = plugin.getExchangeManager();
        this.accountResolver = plugin.getAccountResolver();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!plugin.getConfig().getBoolean("commands.exchange.enabled", true))
        {
            sender.sendMessage(ChatColor.RED + "This command has been disabled by the system administrator!");
            return true;
        }

        if (args.length == 0)
        {
            sendUsage(sender, label);
            return true;
        }

        String subcmd = args[0];

        if (subcmd.equalsIgnoreCase("buy"))
            return handleBuy(sender, label, args);

        if (subcmd.equalsIgnoreCase("sell"))
            return handleSell(sender, label, args);

        if (subcmd.equalsIgnoreCase("orders"))
            return handleOrders(sender, args);

        if (subcmd.equalsIgnoreCase("cancel"))
            return handleCancel(sender, args);

        sendUsage(sender, label);
        return true;
    }

    private boolean handleBuy(CommandSender sender, String label, String[] args)
    {
        Player player = requirePlayer(sender);
        if (player == null)
            return true;

        if (args.length != 4)
        {
            msg(sender, "&c/" + label + " buy <item> <quantity> <max price>");
            return true;
        }

        ItemSpec item = parseItem(sender, args[1]);
        if (item == null)
            return true;

        Integer quantity = parsePositiveInt(sender, args[2], "quantity");
        if (quantity == null)
            return true;

        Double maxPrice = parsePositiveDouble(sender, args[3], "max price");
        if (maxPrice == null)
            return true;

        ExchangeManager.PlaceResult result = exchangeManager.placeBuyOrder(
                player, item.typeId, item.data, quantity, maxPrice);

        switch (result)
        {
            case OK -> msg(sender, "&aBuy order placed: &f" + quantity + "x " + item +
                    " &aat up to &f$" + fmt(maxPrice) + "/ea &a(total escrowed: $" + fmt(quantity * maxPrice) + ")");
            case INSUFFICIENT_FUNDS -> msg(sender, "&cYou don't have enough money for that order (need $" +
                    fmt(quantity * maxPrice) + ").");
            default -> msg(sender, "&cSomething went wrong placing that order. Try again.");
        }

        return true;
    }

    private boolean handleSell(CommandSender sender, String label, String[] args)
    {
        Player player = requirePlayer(sender);
        if (player == null)
            return true;

        if (args.length != 4)
        {
            msg(sender, "&c/" + label + " sell <item> <quantity> <price>");
            return true;
        }

        ItemSpec item = parseItem(sender, args[1]);
        if (item == null)
            return true;

        Integer quantity = parsePositiveInt(sender, args[2], "quantity");
        if (quantity == null)
            return true;

        Double price = parsePositiveDouble(sender, args[3], "price");
        if (price == null)
            return true;

        ExchangeManager.PlaceResult result = exchangeManager.placeSellOrder(
                player, item.typeId, item.data, quantity, price);

        switch (result)
        {
            case OK -> msg(sender, "&aSell order placed: &f" + quantity + "x " + item +
                    " &aat &f$" + fmt(price) + "/ea");
            case INSUFFICIENT_ITEMS -> msg(sender, "&cYou don't have " + quantity + "x " + item + " to sell.");
            default -> msg(sender, "&cSomething went wrong placing that order. Try again.");
        }

        return true;
    }

    private boolean handleOrders(CommandSender sender, String[] args)
    {
        Player player = requirePlayer(sender);
        if (player == null)
            return true;

        int page = 1;
        if (args.length >= 2)
        {
            try
            {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                msg(sender, "&cPage must be a number.");
                return true;
            }
        }

        AccountRef ref = AccountRef.player(player.getName());
        List<ExchangeOrder> orders = exchangeManager.getOrders(ref, page, ORDERS_PER_PAGE);

        if (orders.isEmpty())
        {
            msg(sender, "&7No orders on page " + page + ".");
            return true;
        }

        msg(sender, "&e--- Your Exchange Orders (page " + page + ") ---");
        for (ExchangeOrder order : orders)
        {
            String type = order.getType() == ExchangeOrder.Type.BUY ? "&aBUY " : "&cSELL";
            msg(sender, "&7#" + order.getId() + " " + type + " &f" + order.getRemaining() + "/" + order.getQuantity()
                    + "x " + itemLabel(order.getItemTypeId(), order.getItemData())
                    + " &7@ &f$" + fmt(order.getPrice()) + " &7[" + order.getStatus() + "]");
        }

        return true;
    }

    private boolean handleCancel(CommandSender sender, String[] args)
    {
        Player player = requirePlayer(sender);
        if (player == null)
            return true;

        if (args.length-1 == 0)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /exchange cancel <orderID>");
            return true;
        }

        String orderID = args[1];
        boolean success = exchangeManager.cancelOrder(orderID, AccountRef.of(new EssentialsAccount(player.getName())));
        if (success)
        {
            sender.sendMessage(ChatColor.RED + "Successfully cancelled your order!");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "There was a hiccup while trying to cancel your order!");

        return true;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Player requirePlayer(CommandSender sender)
    {
        if (sender instanceof Player player)
            return player;
        msg(sender, "&cOnly players can use this command.");
        return null;
    }

    private record ItemSpec(int typeId, short data)
    {
        @Override
        public String toString()
        {
            return typeId + (data != 0 ? ":" + data : "");
        }
    }

    private String itemLabel(int typeId, short data)
    {
        return typeId + (data != 0 ? ":" + data : "");
    }

    /**
     * Accepts "<typeId>" or "<typeId>:<data>", e.g. "35" or "35:2".
     */
    private ItemSpec parseItem(CommandSender sender, String raw)
    {
        try
        {
            if (raw.contains(":"))
            {
                String[] parts = raw.split(":", 2);
                return new ItemSpec(Integer.parseInt(parts[0]), Short.parseShort(parts[1]));
            }
            return new ItemSpec(Integer.parseInt(raw), (short) 0);
        } catch (NumberFormatException e) {
            msg(sender, "&cInvalid item, expected a numeric item id (optionally '<id>:<data>').");
            return null;
        }
    }

    private Integer parsePositiveInt(CommandSender sender, String raw, String fieldName)
    {
        try
        {
            int value = Integer.parseInt(raw);
            if (value <= 0)
            {
                msg(sender, "&c" + fieldName + " must be greater than 0.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            msg(sender, "&cInvalid " + fieldName + ", expected a whole number.");
            return null;
        }
    }

    private Double parsePositiveDouble(CommandSender sender, String raw, String fieldName)
    {
        try
        {
            double value = Double.parseDouble(raw);
            if (value <= 0)
            {
                msg(sender, "&c" + fieldName + " must be greater than 0.");
                return null;
            }
            return value;
        }
        catch (NumberFormatException e)
        {
            msg(sender, "&cInvalid " + fieldName + ", expected a number.");
            return null;
        }
    }

    private String fmt(double value)
    {
        return String.format("%.2f", value);
    }

    private void msg(CommandSender sender, String message)
    {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void sendUsage(CommandSender sender, String label)
    {
        String prefix = "&e/" + label;

        msg(sender, prefix + " buy <item> <quantity> <max price>");
        msg(sender, "&f - Place a buy order for an item");

        msg(sender, prefix + " sell <item> <quantity> <price>");
        msg(sender, "&f - Place a sell order for an item");

        msg(sender, prefix + " orders [#]");
        msg(sender, "&f - View your current orders");
    }
}