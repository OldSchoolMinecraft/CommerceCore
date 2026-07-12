package net.oldschoolminecraft.cc.managers;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.google.gson.Gson;
import net.oldschoolminecraft.cc.CommerceCore;
import net.oldschoolminecraft.cc.contracts.AbstractContract;
import net.oldschoolminecraft.cc.contracts.ContractStatus;
import net.oldschoolminecraft.cc.contracts.ContractType;
import net.oldschoolminecraft.cc.contracts.LoanContract;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ContractManager
{
    private static final Gson gson = new Gson();

    private CommerceCore plugin;
    private File dataDir;

    private ArrayList<AbstractContract> contracts = new ArrayList<>();

    public ContractManager(CommerceCore plugin, File dataDir)
    {
        this.plugin = plugin;
        this.dataDir = dataDir;
    }

    public void init()
    {
        loadAllContracts();

        new Thread(() ->
        {
            ArrayList<AbstractContract> toRemove = new ArrayList<>();

            for (AbstractContract contract : contracts)
            {
                // evaluate the contract to update its state
                contract.evaluate();

                // if the contract is marked as completed, we can delete the file & release it from memory
                if (contract.getStatus() == ContractStatus.COMPLETED)
                    toRemove.add(contract);
            }

            for (AbstractContract contract : toRemove)
            {
                removeContract(contract);
            }

            // wait 30 seconds until our next sweep
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(30));
        }).start();
    }

    public void addContract(AbstractContract contract) throws IOException
    {
        saveContract(contract);
        contracts.add(contract);
    }

    public void removeContract(AbstractContract contract)
    {
        deleteContract(new File(dataDir, contract.getContractId() + ".json"));
        contracts.remove(contract);
    }

    public LoanContract getLoanContractByBorrower(String name)
    {
        for (AbstractContract contract : contracts)
        {
            if (contract.getContractType() == ContractType.LOAN && ((LoanContract)contract).getBorrower().equalsIgnoreCase(name))
                return (LoanContract) contract;
        }
        return null;
    }

    private void loadAllContracts()
    {
        for (File file : Objects.requireNonNull(dataDir.listFiles()))
        {
            if (file.isDirectory()) continue;
            if (!file.getName().endsWith(".json")) continue;
            AbstractContract contract = loadContract(file);
            if (contract != null)
                contracts.add(contract);
        }
    }

    private AbstractContract loadContract(File file)
    {
        try (FileReader reader = new FileReader(file))
        {
            return gson.fromJson(reader, AbstractContract.class);
        } catch (IOException e) {
            return null;
        }
    }

    private void saveContract(AbstractContract contract) throws IOException
    {
        try (FileWriter writer = new FileWriter(new File(dataDir, contract.getContractId() + ".json")))
        {
            gson.toJson(contract, AbstractContract.class, writer);
        }
    }

    private void deleteContract(File file)
    {
        if (!file.delete()) file.deleteOnExit();
    }
}
