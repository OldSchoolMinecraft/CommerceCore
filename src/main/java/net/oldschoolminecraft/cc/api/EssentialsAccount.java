package net.oldschoolminecraft.cc.api;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;

public class EssentialsAccount implements NamedMutableBalance
{
    private String playerName;

    public EssentialsAccount(String playerName)
    {
        this.playerName = playerName;
    }

    @Override
    public double balance()
    {
        try
        {
            return Economy.getMoney(playerName);
        } catch (UserDoesNotExistException e) {
            return -1D;
        }
    }

    @Override
    public boolean set(double v)
    {
        try
        {
            Economy.setMoney(playerName, v);
            return true;
        } catch (UserDoesNotExistException | NoLoanPermittedException e) {
            return false;
        }
    }

    @Override
    public boolean add(double v)
    {
        try
        {
            Economy.add(playerName, v);
            return true;
        } catch (UserDoesNotExistException | NoLoanPermittedException e) {
            return false;
        }
    }

    @Override
    public boolean subtract(double v)
    {
        try
        {
            Economy.subtract(playerName, v);
            return true;
        } catch (UserDoesNotExistException | NoLoanPermittedException e) {
            return false;
        }
    }

    @Override
    public boolean multiply(double v)
    {
        try
        {
            Economy.multiply(playerName, v);
            return true;
        } catch (UserDoesNotExistException | NoLoanPermittedException e) {
            return false;
        }
    }

    @Override
    public boolean divide(double v)
    {
        try
        {
            Economy.divide(playerName, v);
            return true;
        } catch (UserDoesNotExistException | NoLoanPermittedException e) {
            return false;
        }
    }

    @Override
    public boolean hasEnough(double v)
    {
        try
        {
            return Economy.hasEnough(playerName, v);
        } catch (UserDoesNotExistException e) {
            return false;
        }
    }

    @Override
    public boolean hasOver(double v)
    {
        try
        {
            return Economy.getMoney(playerName) > v;
        } catch (UserDoesNotExistException e) {
            return false;
        }
    }

    @Override
    public boolean hasUnder(double v)
    {
        try
        {
            return Economy.getMoney(playerName) < v;
        } catch (UserDoesNotExistException e) {
            return false;
        }
    }

    @Override
    public boolean isNegative()
    {
        try
        {
            return Economy.getMoney(playerName) < 0;
        } catch (UserDoesNotExistException e) {
            return false;
        }
    }

    @Override
    public String getAccountName()
    {
        return playerName;
    }
}
