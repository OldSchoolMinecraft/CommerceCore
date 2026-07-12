package net.oldschoolminecraft.cc.managers;

import com.google.gson.Gson;
import net.oldschoolminecraft.cc.CommerceCore;
import net.oldschoolminecraft.cc.data.Business;
import net.oldschoolminecraft.cc.util.BusinessOwnerIndex;

import java.io.File;
import java.io.FileReader;

public class BusinessManager
{
    private static final Gson gson = new Gson();
    private File dataDir;
    private BusinessOwnerIndex ownerIndex;

    public BusinessManager(CommerceCore plugin, File dataDir)
    {
        this.dataDir = dataDir;
        this.ownerIndex = new BusinessOwnerIndex(plugin);
        this.ownerIndex.refresh();
    }

    public void processEmployeePayment()
    {
        //TODO: loop through all businesses
        // loop through all employees
        // deduct money from business, transfer to employee
        // record transaction
    }

    public Business getByName(String businessName)
    {
        return getFromFile(new File(dataDir, businessName.toLowerCase() + ".json"));
    }

    public Business getByOwner(String ownerName)
    {
        return getByName(ownerIndex.getByOwner(ownerName));
    }

    public Business getFromFile(File file)
    {
        try (FileReader reader = new FileReader(file))
        {
            return gson.fromJson(reader, Business.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    public File getDataDir()
    {
        return dataDir;
    }
}
