/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

public class StringLocator extends GenericLocator
{
    public StringLocator(final String name, final int value)
    {
        super(name, LocatorTypes.STRING, "" + value);
    }
}
