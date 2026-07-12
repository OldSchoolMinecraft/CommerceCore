package net.oldschoolminecraft.cc.listeners;

import net.oldschoolminecraft.cc.CommerceCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class ChestShopListener implements Listener
{
    private CommerceCore plugin;

    public ChestShopListener(CommerceCore plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event)
    {
        //
    }
}
