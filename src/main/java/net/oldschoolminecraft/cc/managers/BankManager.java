package net.oldschoolminecraft.cc.managers;

import com.google.gson.Gson;
import net.oldschoolminecraft.cc.data.BankAccount;
import net.oldschoolminecraft.cc.data.Business;

import java.io.File;
import java.util.ArrayList;

public class BankManager
{
    private static final Gson gson = new Gson();
    private File dataDir;

    public BankManager(File dataDir)
    {
        this.dataDir = dataDir;
    }

    public BankAccount getAccount(String name)
    {
        return null; //TODO
    }

    public ArrayList<BankAccount> getAccounts(Business holder)
    {
        return new ArrayList<>();
    }
}
