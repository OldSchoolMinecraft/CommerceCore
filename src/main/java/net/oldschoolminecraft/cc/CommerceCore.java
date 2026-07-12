package net.oldschoolminecraft.cc;

import com.oldschoolminecraft.OSMEss.OSMEss;
import net.oldschoolminecraft.cc.managers.BankManager;
import net.oldschoolminecraft.cc.managers.BusinessManager;
import net.oldschoolminecraft.cc.managers.ContractManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class CommerceCore extends JavaPlugin
{
    private static File dataFolder;
    private static OSMEss osmEss;

    private BusinessManager businessManager;
    private BankManager bankManager;
    private ContractManager contractManager;

    public void onEnable()
    {
        dataFolder = getDataFolder();

        osmEss = (OSMEss) getServer().getPluginManager().getPlugin("OSM-Ess");

        businessManager = new BusinessManager(this, new File(getDataFolder(), "business-data/"));
        bankManager = new BankManager(new File(getDataFolder(), "bank-data/"));
        contractManager = new ContractManager(this, new File(getDataFolder(), "contract-data/"));

        contractManager.init();

        System.out.println("CommerceCore enabled");
    }

    public void onDisable()
    {
        System.out.println("CommerceCore disabled");
    }

    public BusinessManager getBusinessManager()
    {
        return businessManager;
    }

    public BankManager getBankManager()
    {
        return bankManager;
    }

    public ContractManager getContractManager()
    {
        return contractManager;
    }

    public static OSMEss static_getOSMEss()
    {
        return osmEss;
    }

    public static File static_getDataFolder()
    {
        return dataFolder;
    }
}
