package net.oldschoolminecraft.cc.api;

import net.oldschoolminecraft.cc.CommerceCore;

/**
 * Wire this up wherever your business accounts actually live — replace the
 * TODO with a real lookup (e.g. plugin.getBusinessManager().getAccount(name)).
 */
public class DefaultAccountResolver implements AccountResolver
{
    private final CommerceCore plugin;

    public DefaultAccountResolver(CommerceCore plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public NamedMutableBalance resolve(AccountRef ref)
    {
        String[] parts = ref.getName().contains(":") ? ref.getName().split(":") : new String[]{};
        return switch (ref.getKind())
        {
            case PLAYER -> new EssentialsAccount(ref.getName());
            case BUSINESS -> resolveBusinessAccount(parts[0], parts[1]);
        };
    }

    private NamedMutableBalance resolveBusinessAccount(String businessName, String accountName)
    {
        return plugin.getBankManager().getAccount(businessName, accountName);
    }
}