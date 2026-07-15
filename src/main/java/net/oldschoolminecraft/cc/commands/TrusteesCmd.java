package net.oldschoolminecraft.cc.commands;

import net.oldschoolminecraft.cc.CommerceCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TrusteesCmd implements CommandExecutor
{
    private CommerceCore plugin;

    public TrusteesCmd(CommerceCore plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!plugin.getConfig().getBoolean("commands.trustees.enabled", true))
        {
            sender.sendMessage(ChatColor.RED + "This command has been disabled by the system administrator!");
            return true;
        }

        if (args.length == 0)
        {
            //TODO: send help
            return true;
        }

        String subcmd = args[0];

        if (subcmd.equalsIgnoreCase("add"))
        {
            //TODO: /trustees add <username> <account>
        }

        if (subcmd.equalsIgnoreCase("remove"))
        {
            //TODO: /trustees remove <username> <account>
        }

        if (subcmd.equalsIgnoreCase("grant"))
        {
            //TODO: /trustees grant <username> <account> <permission>
        }

        if (subcmd.equalsIgnoreCase("revoke"))
        {
            //TODO: /trustees revoke <username> <account> <permission>
        }

        if (subcmd.equalsIgnoreCase("permlist"))
        {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7-- &8Permission List &7--"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eVIEW_BALANCE, DEPOSIT, WITHDRAW"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eMANAGE_TRUSTEES, CHANGE_LIMIT, CLOSE_ACCOUNT"));
        }

        return false;
    }
}
