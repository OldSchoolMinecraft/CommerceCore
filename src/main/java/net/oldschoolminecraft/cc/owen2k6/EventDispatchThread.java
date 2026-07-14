package net.oldschoolminecraft.cc.owen2k6;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class EventDispatchThread extends Thread
{
    private TransactionLogger tLogger = TransactionLogger.getInstance();

    public void run()
    {
        while (true)
        {
            if (!tLogger.hasNext()) LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
        }
    }
}
