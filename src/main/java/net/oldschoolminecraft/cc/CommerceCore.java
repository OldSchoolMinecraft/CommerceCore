package net.oldschoolminecraft.cc;

import net.oldschoolminecraft.cc.managers.BankManager;
import net.oldschoolminecraft.cc.managers.BusinessManager;
import net.oldschoolminecraft.cc.managers.ContractManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class CommerceCore extends JavaPlugin
{
    private BusinessManager businessManager;
    private BankManager bankManager;
    private ContractManager contractManager;

    public void onEnable()
    {
        businessManager = new BusinessManager(new File(getDataFolder(), "business-data/"));
        bankManager = new BankManager(new File(getDataFolder(), "bank-data/"));
        contractManager = new ContractManager(new File(getDataFolder(), "contract-data/"));

        contractManager.init();

        System.out.println("CommerceCore enabled");
    }

    public void onDisable()
    {
        System.out.println("CommerceCore disabled");
    }
}
