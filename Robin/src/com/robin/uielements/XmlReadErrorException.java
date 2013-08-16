/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

public class XmlReadErrorException extends RuntimeException
{

    /**
     * Default Serial Version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * UIXml Read Error Exxception.
     * @param s The Error.
     */
    public XmlReadErrorException(final String s)
    {
        super(String.format(
            "The UIXML file could not be parsed. The following"
                + " error occured: \n%s",
            s));
    }

}
