package net.oldschoolminecraft.cc.managers;

import com.google.gson.*;
import net.oldschoolminecraft.cc.CommerceCore;
import net.oldschoolminecraft.cc.api.AccountResolver;
import net.oldschoolminecraft.cc.contracts.AbstractContract;
import net.oldschoolminecraft.cc.contracts.ContractStatus;
import net.oldschoolminecraft.cc.contracts.ContractType;
import net.oldschoolminecraft.cc.contracts.LoanContract;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ContractManager extends Thread
{
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(AbstractContract.class, new ContractTypeAdapter())
            .create();

    private CommerceCore plugin;
    private File dataDir;
    private AccountResolver accountResolver;

    private ArrayList<AbstractContract> contracts = new ArrayList<>();
    private volatile boolean running = true;

    public ContractManager(CommerceCore plugin, File dataDir)
    {
        this.plugin = plugin;
        this.dataDir = dataDir;
        this.accountResolver = plugin.getAccountResolver();
    }

    public void init()
    {
        dataDir.mkdirs();
        loadAllContracts();
        start();
    }

    public void run()
    {
        while (running)
        {
            ArrayList<AbstractContract> toRemove = new ArrayList<>();

            for (AbstractContract contract : contracts)
            {
                // evaluate the contract to update its state
                contract.evaluate();

                // persist in case evaluate() changed state (e.g. ACTIVE -> DEFAULTED)
                // so a mid-sweep server restart doesn't lose that transition
                // or, worse, re-apply an already-recorded forced repayment.
                try
                {
                    saveContract(contract);
                } catch (IOException e) {
                    System.err.println("[CommerceCore] Failed to persist contract " + contract.getContractId() + " during sweep: " + e.getMessage());
                }

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
        }
    }

    public void shutdown()
    {
        running = false;
        interrupt();
    }

    public void addContract(AbstractContract contract) throws IOException
    {
        contract.attachAccountResolver(accountResolver);
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
            {
                contract.attachAccountResolver(accountResolver);
                contracts.add(contract);
            }
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
            // Serialize through the declared AbstractContract.class type token so
            // our custom adapter below fires; the adapter itself re-dispatches to
            // the contract's concrete runtime type, which is what actually gets
            // the subclass fields (lender, borrower, principal...) written out.
            gson.toJson(contract, AbstractContract.class, writer);
        }
    }

    private void deleteContract(File file)
    {
        if (!file.delete()) file.deleteOnExit();
    }

    /**
     * Dispatches AbstractContract (de)serialization to the correct concrete
     * subclass based on the "contractType" field, and always serializes
     * using the object's runtime type so subclass-only fields aren't dropped.
     */
    private static final class ContractTypeAdapter implements JsonSerializer<AbstractContract>, JsonDeserializer<AbstractContract>
    {
        @Override
        public JsonElement serialize(AbstractContract src, Type typeOfSrc, JsonSerializationContext context)
        {
            return context.serialize(src, src.getClass());
        }

        @Override
        public AbstractContract deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject obj = json.getAsJsonObject();
            ContractType type = context.deserialize(obj.get("contractType"), ContractType.class);

            Class<? extends AbstractContract> targetClass = switch (type)
            {
                case LOAN -> LoanContract.class;
                case CUSTOM_CONTRACT -> throw new JsonParseException(
                        "No concrete class registered for ContractType.CUSTOM_CONTRACT yet");
            };

            return context.deserialize(json, targetClass);
        }
    }
}