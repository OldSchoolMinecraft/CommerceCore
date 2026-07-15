package net.oldschoolminecraft.cc.commands;

import net.oldschoolminecraft.cc.CommerceCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCmd implements CommandExecutor
{
    private CommerceCore plugin;

    public AdminCmd(CommerceCore plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender.hasPermission("commercecore.admin") || sender.isOp()))
        {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0)
        {
            //TODO: help
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            plugin.getConfig().reload();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
            return true;
        }

        return false;
    }
}
