/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

import java.util.Locale;

@SuppressWarnings("serial")
public class TranslationNotFoundException extends RuntimeException
{

    /**
     * Throw when a particular locator is requested on an element.
     * @param elementName
     *            The WebElement object's name
     * @param language
     *            The Locator object's type
     * @param path
     *            The path of the UIXM file where it should be located
     */
    public TranslationNotFoundException(final String elementName,
        final Locale language, final String path)
    {
        super(String.format(
            "Translation %s not found for element %s in the "
                + "%s TLXML config(s)",
            language.getLanguage() + "_" + language.getCountry(),
            elementName,
            path));
    }

    /**
     * Throw when only an element is requested.
     * @param elementName
     *            The WebElement name.
     * @param path
     *            The path of the UIXM file where it should be located
     */
    public TranslationNotFoundException(final String elementName,
        final String path)
    {
        super(String.format("Translation not found for element '%s' in the "
            + "%s TLXML config(s)", elementName, path));
    }
}
