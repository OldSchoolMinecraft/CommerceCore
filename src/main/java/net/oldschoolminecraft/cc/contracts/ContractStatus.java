package net.oldschoolminecraft.cc.contracts;

public enum ContractStatus
{
    /**
     * Created but not yet active (e.g. awaiting funding).
     */
    PENDING,
    /**
     * In force; subclass-specific obligations are outstanding.
     */
    ACTIVE,
    /**
     * Terminal: all obligations were satisfied.
     */
    COMPLETED,
    /**
     * Terminal: an obligation was not satisfied in time.
     */
    DEFAULTED,
    /**
     * Terminal: called off before completion or default.
     */
    CANCELLED
}
