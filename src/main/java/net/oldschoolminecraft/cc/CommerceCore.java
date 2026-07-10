package net.oldschoolminecraft.cc;

import net.oldschoolminecraft.cc.managers.BankManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class CommerceCore extends JavaPlugin
{
    private BankManager bankManager;

    public void onEnable()
    {
        System.out.println("CommerceCore enabled");
    }

    public void onDisable()
    {
        System.out.println("CommerceCore disabled");
    }
}
