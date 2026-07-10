package net.oldschoolminecraft.cc.managers;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.google.gson.Gson;
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
    private File dataDir;

    private ArrayList<AbstractContract> contracts = new ArrayList<>();

    public ContractManager(File dataDir)
    {
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
                contract.evaluate();
                if (contract.getStatus() == ContractStatus.COMPLETED)
                    toRemove.add(contract);
                if (contract.getContractType() == ContractType.LOAN && contract.getStatus() == ContractStatus.DEFAULTED)
                {
                    // if borrowers time since last login exceeds 30 days:
                    // deduct the full amount and put the borrowers balance in the negative
                    //TODO

                    try // attempt to deduct any incoming money from borrower balance util debt is repaid
                    {
                        LoanContract loanContract = (LoanContract) contract;
                        double borrowerBalance = Economy.getMoney(loanContract.getBorrower());
                        if (borrowerBalance > 0.0D)
                        {
                            Economy.setMoney(loanContract.getBorrower(), 0);
                            loanContract.repay(borrowerBalance);
                        }
                    } catch (UserDoesNotExistException | NoLoanPermittedException ignored) {}
                }
            }

            for (AbstractContract contract : toRemove)
            {
                removeContract(contract);
            }
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
