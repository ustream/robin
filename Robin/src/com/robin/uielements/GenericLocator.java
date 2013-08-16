/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

public class GenericLocator implements Locator
{
    private final LocatorTypes locType;

    private final String locVal;

    private String locName;

    public GenericLocator(final String name, final LocatorTypes type,
        final String value)
    {
        locType = type;
        locVal = value;
        locName = name;
    }

    @Override
    public String getValue()
    {
        return locVal;
    }

    @Override
    public LocatorTypes getType()
    {
        return locType;
    }

    @Override
    public String getName()
    {
        return locName;
    }
}
