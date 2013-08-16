package com.robin.activities;

public class ActivityHelper
{
    public static MainActivity getMainActivity(final int... indexOfSolo)
    {
        return new MainActivity(indexOfSolo);
    }
}
