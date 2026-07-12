package net.oldschoolminecraft.cc.managers;

import com.google.gson.Gson;
import net.oldschoolminecraft.cc.data.BankAccount;
import net.oldschoolminecraft.cc.data.Business;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Objects;

public class BankManager
{
    private static final Gson gson = new Gson();
    private File dataDir;

    public BankManager(File dataDir)
    {
        this.dataDir = dataDir;
    }

    public BankAccount getAccount(String businessName, String name)
    {
        File businessDir = new File(dataDir, businessName + "/");
        return getBankFromFile(new File(businessDir, name.toLowerCase() + ".json"));
    }

    public ArrayList<BankAccount> getAccounts(Business holder)
    {
        File businessDir = new File(dataDir, holder.name + "/");
        return getAccountsFromBusinessDir(businessDir);
    }

    private ArrayList<BankAccount> getAccountsFromBusinessDir(File dir)
    {
        ArrayList<BankAccount> accounts = new ArrayList<>();

        for (File file : Objects.requireNonNull(dir.listFiles()))
        {
            if (file.isDirectory()) continue;
            if (!file.getName().endsWith(".json")) continue;

            BankAccount account = getBankFromFile(file);
            if (account == null) continue; // something failed with the file
            accounts.add(account);
        }

        return accounts;
    }

    private BankAccount getBankFromFile(File file)
    {
        try (FileReader reader = new FileReader(file))
        {
            return gson.fromJson(reader, BankAccount.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
