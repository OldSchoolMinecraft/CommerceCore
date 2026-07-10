package net.oldschoolminecraft.cc.api;

import net.oldschoolminecraft.cc.data.Business;

public interface BusinessAccount extends MutableBalance
{
    String getAccountName();
    Business getHolder();
    boolean delete();
}
