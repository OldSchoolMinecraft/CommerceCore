package net.oldschoolminecraft.cc.commands;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import net.oldschoolminecraft.cc.CommerceCore;
import net.oldschoolminecraft.cc.api.EssentialsAccount;
import net.oldschoolminecraft.cc.api.NamedMutableBalance;
import net.oldschoolminecraft.cc.contracts.LoanContract;
import net.oldschoolminecraft.cc.data.BankAccount;
import net.oldschoolminecraft.cc.data.Business;
import net.oldschoolminecraft.cc.util.DurationValue;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.time.Instant;

public class LoansCmd implements CommandExecutor
{
    private CommerceCore plugin;

    public LoansCmd(CommerceCore plugin)
    {
        this.plugin = plugin;
    }

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
            boolean businessLoan = label.equalsIgnoreCase("b-loan");
            Business business = plugin.getBusinessManager().getByOwner(ply.getName());

            if (businessLoan && business == null)
            {
                ply.sendMessage(ChatColor.RED + "You can't issue a business loan because you don't own a business!");
                return true;
            }

            int argumentOffset = 0;

            if (businessLoan)
                argumentOffset++;

            String accountName = args[1];
            BankAccount bankAccount = plugin.getBankManager().getAccount(accountName);

            NamedMutableBalance lender = businessLoan ? bankAccount : new EssentialsAccount(ply.getName());
            NamedMutableBalance borrower = new EssentialsAccount(args[1 + argumentOffset]);
            double amount = Double.parseDouble(args[2 + argumentOffset]);
            String interest = args[3 + argumentOffset];
            String deadline = args[4 + argumentOffset];
            double principal = parsePercentage(interest);
            DurationValue deadlineDuration = DurationValue.parseDuration(deadline);
            LoanContract loanContract = new LoanContract(lender, borrower, principal, amount * (1.0 + principal), Instant.now().plus(deadlineDuration.amount(), deadlineDuration.unit()));

            try
            {
                plugin.getContractManager().addContract(loanContract);
                ply.sendMessage(ChatColor.GREEN + "Your contract has been created!");
            } catch (IOException e) {
                ply.sendMessage(ChatColor.RED + e.getMessage());
            }
            return true;
        }

        // loan repay [amount]
        // loan repay
        // --OR--
        // loan repay 1500
        if (subcmd.equalsIgnoreCase("repay"))
        {
            LoanContract loanContract = plugin.getContractManager().getLoanContractByBorrower(ply.getName());

            if (loanContract == null)
            {
                ply.sendMessage(ChatColor.RED + "You don't have a loan contract!");
                return true;
            }

            double amount = loanContract.getRemainingBalance(); // full repayment amount

            if (args.length-1 == 1) // has amount specified
            {
                try
                {
                    amount = Double.parseDouble(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "That is not a valid double value!");
                }

                if (amount > loanContract.getRemainingBalance())
                {
                    sender.sendMessage(ChatColor.RED + "The number you entered would have exceeded your balance owed!");
                    return true;
                }

                if (amount < 0)
                {
                    sender.sendMessage(ChatColor.RED + "You can't enter negative numbers -_-");
                    return true;
                }
            }

            try
            {
                if (!Economy.hasEnough(ply.getName(), amount))
                {
                    sender.sendMessage(ChatColor.RED + "You don't have enough money to make this repayment!");
                    return true;
                }

                Economy.subtract(ply.getName(), amount);
                loanContract.repay(amount);
                String msgRaw = String.format("&aYou made a repayment of &7$%.2f&a successfully!", amount);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msgRaw));

                // evaluate loan to update its state
                loanContract.evaluate();

                if (loanContract.isRepaidByBorrower())
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYour debt is repaid and the contract is completed!"));
                else sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&eYour balance has &7$%.2f&e remaining!", loanContract.getRemainingBalance())));
            } catch (UserDoesNotExistException | NoLoanPermittedException ex) {
                sender.sendMessage(ChatColor.RED + ex.getMessage());
            }
            return true;
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
