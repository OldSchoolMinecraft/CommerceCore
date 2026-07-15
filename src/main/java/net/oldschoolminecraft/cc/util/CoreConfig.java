package net.oldschoolminecraft.cc.util;

import org.bukkit.util.config.Configuration;

import java.io.File;

public class CoreConfig extends Configuration
{
    public CoreConfig(File file)
    {
        super(file);
        reload();
    }

    public void reload()
    {
        load();
        write();
        save();
    }

    private void write()
    {
        generateConfigOption("commands.business.enabled", false);
        generateConfigOption("commands.loans.enabled", false);
        generateConfigOption("commands.trustees.enabled", false);
        generateConfigOption("commands.exchange.enabled", true);
    }

    private void generateConfigOption(String key, Object defaultValue)
    {
        if (this.getProperty(key) == null) this.setProperty(key, defaultValue);
        final Object value = this.getProperty(key);
        this.removeProperty(key);
        this.setProperty(key, value);
    }

    public Object getConfigOption(String key)
    {
        return this.getProperty(key);
    }

    public Object getConfigOption(String key, Object defaultValue)
    {
        Object value = getConfigOption(key);
        if (value == null) value = defaultValue;
        return value;
    }
}
