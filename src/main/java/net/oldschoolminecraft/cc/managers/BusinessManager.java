package net.oldschoolminecraft.cc.managers;

import com.google.gson.Gson;

import java.io.File;

public class BusinessManager
{
    private static final Gson gson = new Gson();
    private File dataDir;

    public BusinessManager(File dataDir)
    {
        this.dataDir = dataDir;
    }
}
