package net.oldschoolminecraft.cc.api;

import net.oldschoolminecraft.cc.data.Business;

public interface BusinessAccount extends NamedMutableBalance
{
    String getAccountName();
    Business getHolder();
}
