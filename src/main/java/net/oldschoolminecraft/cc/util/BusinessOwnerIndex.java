package net.oldschoolminecraft.cc.util;

import net.oldschoolminecraft.cc.CommerceCore;
import net.oldschoolminecraft.cc.data.Business;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

public class BusinessOwnerIndex
{
    private final CommerceCore plugin;
    private final HashMap<String, String> nameBusinessCache = new HashMap<>();
    private final File businessDir;

    public BusinessOwnerIndex(CommerceCore plugin)
    {
        this.plugin = plugin;
        this.businessDir = plugin.getBusinessManager().getDataDir();
    }

    public void refresh()
    {
        System.out.println("[CommerceCore] Building business owner->name cache...");
        for (File file : Objects.requireNonNull(businessDir.listFiles()))
        {
            if (file.isDirectory()) continue;
            if (!file.getName().endsWith(".json")) continue;

            Business business = plugin.getBusinessManager().getFromFile(file);
            nameBusinessCache.put(business.owner, business.name);
        }
        System.out.println("[CommerceCore] Cache building complete!");
    }

    public String getByOwner(String ownerName)
    {
        return nameBusinessCache.get(ownerName);
    }
}
