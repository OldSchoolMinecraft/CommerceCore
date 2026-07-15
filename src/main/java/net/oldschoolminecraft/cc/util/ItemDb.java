package net.oldschoolminecraft.cc.util;

import com.earth2me.essentials.IConf;
import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemDb implements IConf
{
    private final transient JavaPlugin plugin;
    private static final Logger LOGGER;
    private final transient Map<String, Integer> items;
    private final transient Map<String, Short> durabilities;

    public ItemDb(final JavaPlugin plugin) {
        this.items = new HashMap<String, Integer>();
        this.durabilities = new HashMap<String, Short>();
        this.plugin = plugin;
    }

    @Override
    public void reloadConfig() {
        final File file = new File(this.plugin.getDataFolder(), "items.csv");
        if (!file.exists()) {
            final InputStream res = ItemDb.class.getResourceAsStream("/items.csv");
            FileWriter tx = null;
            try {
                tx = new FileWriter(file);
                int i = 0;
                while ((i = res.read()) > 0) {
                    tx.write(i);
                }
                tx.flush();
            }
            catch (IOException ex) {
                ItemDb.LOGGER.log(Level.SEVERE, Util.i18n("itemsCsvNotLoaded"), ex);
                return;
            }
            finally {
                try {
                    res.close();
                }
                catch (Exception ex5) {}
                try {
                    if (tx != null) {
                        tx.close();
                    }
                }
                catch (Exception ex6) {}
            }
        }
        BufferedReader rx = null;
        try {
            rx = new BufferedReader(new FileReader(file));
            this.durabilities.clear();
            this.items.clear();
            int j = 0;
            while (rx.ready()) {
                try {
                    final String line = rx.readLine().trim().toLowerCase();
                    if (!line.startsWith("#")) {
                        final String[] parts = line.split("[^a-z0-9]");
                        if (parts.length >= 2) {
                            final int numeric = Integer.parseInt(parts[1]);
                            this.durabilities.put(parts[0].toLowerCase(), (short)((parts.length > 2 && !parts[2].equals("0")) ? Short.parseShort(parts[2]) : 0));
                            this.items.put(parts[0].toLowerCase(), numeric);
                        }
                    }
                }
                catch (Exception ex4) {
                    ItemDb.LOGGER.warning(Util.format("parseError", "items.csv", j));
                }
                ++j;
            }
        }
        catch (IOException ex2) {
            ItemDb.LOGGER.log(Level.SEVERE, Util.i18n("itemsCsvNotLoaded"), ex2);
            if (rx != null) {
                try {
                    rx.close();
                }
                catch (IOException ex3) {
                    ItemDb.LOGGER.log(Level.SEVERE, ex2.getMessage(), ex3);
                }
            }
        }
        finally {
            if (rx != null) {
                try {
                    rx.close();
                }
                catch (IOException ex3) {
                    ItemDb.LOGGER.log(Level.SEVERE, ex3.getMessage(), ex3);
                }
            }
        }
    }

    public String getName(final int id) {
        return this.getName(id, (short)0);
    }

    public String getName(final int id, final short data) {
        for (final Map.Entry<String, Integer> entry : this.items.entrySet()) {
            if (entry.getValue() == id) {
                final String key = entry.getKey();
                final short dur = this.durabilities.containsKey(key) ? this.durabilities.get(key) : 0;
                if (dur == data) {
                    return key;
                }
            }
        }
        final Material mat = Material.getMaterial(id);
        if (mat == null) {
            return String.valueOf(id);
        }
        return (data != 0) ? (mat.name().toLowerCase() + ":" + data) : mat.name().toLowerCase();
    }

    public ItemStack get(final String id, final int quantity) throws Exception {
        final ItemStack retval = this.get(id.toLowerCase());
        retval.setAmount(quantity);
        return retval;
    }

    public ItemStack get(final String id) throws Exception {
        int itemid = 0;
        String itemname = null;
        short metaData = 0;
        if (id.matches("^\\d+[:+',;.]\\d+$")) {
            itemid = Integer.parseInt(id.split("[:+',;.]")[0]);
            metaData = Short.parseShort(id.split("[:+',;.]")[1]);
        }
        else if (id.matches("^\\d+$")) {
            itemid = Integer.parseInt(id);
        }
        else if (id.matches("^[^:+',;.]+[:+',;.]\\d+$")) {
            itemname = id.split("[:+',;.]")[0].toLowerCase();
            metaData = Short.parseShort(id.split("[:+',;.]")[1]);
        }
        else {
            itemname = id.toLowerCase();
        }
        if (itemname != null) {
            if (this.items.containsKey(itemname)) {
                itemid = this.items.get(itemname);
                if (this.durabilities.containsKey(itemname) && metaData == 0) {
                    metaData = this.durabilities.get(itemname);
                }
            }
            else {
                if (Material.getMaterial(itemname) == null) {
                    throw new Exception(Util.format("unknownItemName", id));
                }
                itemid = Material.getMaterial(itemname).getId();
                metaData = 0;
            }
        }
        final Material mat = Material.getMaterial(itemid);
        if (mat == null) {
            throw new Exception(Util.format("unknownItemId", itemid));
        }
        final ItemStack retval = new ItemStack(mat);
        retval.setAmount(64);
        retval.setDurability(metaData);
        return retval;
    }

    static {
        LOGGER = Logger.getLogger("Minecraft");
    }
}