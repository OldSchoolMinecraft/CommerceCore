package net.oldschoolminecraft.cc.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BusinessCmd implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (args.length == 0)
        {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/business create <name>"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f - Establish your business"));

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/business dissolve"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f - Dissolve/delete your business"));

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/business info"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f - Display a basic overview of your business"));

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/buisiness payroll-account <account>"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f - Set the account used to pay employees from"));

            return true;
        }

        String subcmd = args[0];

        if (subcmd.equalsIgnoreCase("create"))
        {
            //TODO: create <name>
        }

        if (subcmd.equalsIgnoreCase("dissolve"))
        {
            //TODO: dissolve
        }

        if (subcmd.equalsIgnoreCase("info"))
        {
            //TODO: info
        }

        if (subcmd.equalsIgnoreCase("payroll-account"))
        {
            //TODO: payroll-account <account>
        }

        return false;
    }
}
