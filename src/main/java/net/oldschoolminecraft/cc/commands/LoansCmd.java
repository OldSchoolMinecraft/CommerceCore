package net.oldschoolminecraft.cc.commands;

import net.oldschoolminecraft.cc.contracts.LoanContract;
import net.oldschoolminecraft.cc.util.DurationValue;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;

public class LoansCmd implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "You must be a player!");
            return true;
        }

        Player ply = (Player) sender;

        if (args.length < 1)
        {
            // send help

            sender.sendMessage(ChatColor.YELLOW + "/loan contract <username> <amount> <% interest> <deadline>");
            sender.sendMessage(ChatColor.GRAY + " - Issue a new loan contract");

            sender.sendMessage(ChatColor.YELLOW + "/loan repay [amount]");
            sender.sendMessage(ChatColor.GRAY + " - Repay your active loan (max owed amount by default)");

            return true;
        }

        String subcmd = args[0];

        // loan contract <username> <amount> <% interest> <deadline>
        // loan contract Steve 2500 2% 14d
        if (subcmd.equalsIgnoreCase("contract"))
        {
            String lender = null; //TODO: get business registered by sender
            String borrower = args[1];
            double amount = Double.parseDouble(args[2]);
            String interest = args[3];
            String deadline = args[4];
            double principal = parsePercentage(interest);
            DurationValue deadlineDuration = DurationValue.parseDuration(deadline);
            LoanContract loanContract = new LoanContract(lender, borrower, principal, amount * (1.0 + principal), Instant.now().plus(deadlineDuration.amount(), deadlineDuration.unit()));
            return true;
        }

        // loan repay [amount]
        // loan repay
        // --OR--
        // loan repay 1500
        if (subcmd.equalsIgnoreCase("repay"))
        {
            //TODO
        }

        return false;
    }

    public static double parsePercentage(String text)
    {
        if (text == null)
            throw new IllegalArgumentException("Percentage cannot be null.");

        text = text.trim();

        if (!text.endsWith("%"))
            throw new IllegalArgumentException("Percentage must end with '%'.");

        double value = Double.parseDouble(text.substring(0, text.length() - 1).trim());
        return value / 100.0;
    }
}
