package net.oldschoolminecraft.cc.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database
{
    private final Connection connection;

    public Database(String file) throws SQLException
    {
        connection = DriverManager.getConnection("jdbc:sqlite:file://" + file);
    }

    public Connection connection()
    {
        return connection;
    }

    public void close() throws SQLException
    {
        connection.close();
    }
}