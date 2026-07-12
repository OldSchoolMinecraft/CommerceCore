package net.oldschoolminecraft.cc.util;

import com.oldschoolminecraft.OSMEss.Handlers.PlaytimeHandler;
import com.oldschoolminecraft.OSMEss.compat.OSMPLUserData;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class PTUtil
{
    public static long getLastLogin(String playerName)
    {
        try (FileReader reader = new FileReader(new File(PlaytimeHandler.PLAYER_DATA_DIR, playerName.toLowerCase() + ".json")))
        {
            OSMPLUserData data = OSMPLUserData.gson.fromJson(reader, OSMPLUserData.class);
            return data.lastLogIn;
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            return -1;
        }
    }
}
