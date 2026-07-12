package net.oldschoolminecraft.cc.api;

import java.io.Serializable;

public interface NamedMutableBalance extends MutableBalance, Serializable
{
    String getAccountName();
}
