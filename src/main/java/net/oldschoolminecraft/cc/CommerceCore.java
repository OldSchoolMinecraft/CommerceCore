package net.oldschoolminecraft.cc;

import net.oldschoolminecraft.cc.api.AccountResolver;
import net.oldschoolminecraft.cc.api.DefaultAccountResolver;
import net.oldschoolminecraft.cc.commands.BusinessCmd;
import net.oldschoolminecraft.cc.commands.ExchangeCmd;
import net.oldschoolminecraft.cc.commands.LoansCmd;
import net.oldschoolminecraft.cc.commands.TrusteesCmd;
import net.oldschoolminecraft.cc.managers.BankManager;
import net.oldschoolminecraft.cc.managers.BusinessManager;
import net.oldschoolminecraft.cc.managers.ContractManager;
import net.oldschoolminecraft.cc.managers.ExchangeManager;
import net.oldschoolminecraft.cc.util.CoreConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Logger;

public class CommerceCore extends JavaPlugin
{
    private static final Logger logger = Logger.getLogger("CommerceCore");

    private AccountResolver accountResolver;
    private CoreConfig config;

    private BusinessManager businessManager;
    private BankManager bankManager;
    private ContractManager contractManager;
    private ExchangeManager exchangeManager;

    public void onEnable()
    {
        getDataFolder().mkdirs();

        accountResolver = new DefaultAccountResolver(this);
        config = new CoreConfig(new File(getDataFolder(), "config.yml"));

        businessManager = new BusinessManager(this, new File(getDataFolder(), "business-data/"));
        bankManager = new BankManager(new File(getDataFolder(), "bank-data/"));
        contractManager = new ContractManager(this, new File(getDataFolder(), "contract-data/"));

        try
        {
            exchangeManager = new ExchangeManager(this);
        } catch (SQLException ex) {
            ex.printStackTrace(System.err);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("business").setExecutor(new BusinessCmd(this));
        getCommand("loans").setExecutor(new LoansCmd(this));
        getCommand("trustees").setExecutor(new TrusteesCmd(this));
        getCommand("exchange").setExecutor(new ExchangeCmd(this));

        contractManager.init();
        exchangeManager.init();

        System.out.println("CommerceCore enabled");
    }

    public void onDisable()
    {
        businessManager = null;
        bankManager = null;

        contractManager.shutdown();
        contractManager = null;

        exchangeManager = null;

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

    public ExchangeManager getExchangeManager()
    {
        return exchangeManager;
    }

    public AccountResolver getAccountResolver()
    {
        return accountResolver;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public CoreConfig getConfig()
    {
        return config;
    }
}
