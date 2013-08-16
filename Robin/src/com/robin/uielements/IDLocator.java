/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

public class IDLocator extends GenericLocator
{
    public IDLocator(final String name, final int id)
    {
        super(name, LocatorTypes.ID, "" + id);
    }
}
