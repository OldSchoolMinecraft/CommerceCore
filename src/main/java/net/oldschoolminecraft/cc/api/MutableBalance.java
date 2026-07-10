package net.oldschoolminecraft.cc.api;

public interface MutableBalance
{
    double balance();
    boolean set(double v);
    boolean add(double v);
    boolean subtract(double v);
    boolean multiply(double v);
    boolean divide(double v);
    boolean hasEnough(double v);
    boolean hasOver(double v);
    boolean hasUnder(double v);
    boolean isNegative();
}
