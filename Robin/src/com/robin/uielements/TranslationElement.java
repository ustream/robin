/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TranslationElement
{
    /**
     * The translations of the Text.
     */
    private Map<String, String> translationMap = Collections
        .synchronizedSortedMap(new TreeMap<String, String>());

    /**
     * The name of the text.
     */
    private String name;

    /**
     * Create a new translation element.
     * @param id The Name of the element
     */
    public TranslationElement(final String id)
    {
        name = id;
    }

    /**
     * Get the name of the element.
     * @return The name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Add a Translation to the element.
     * @param language The Translation to add.
     * @param text The Translation to add.
     */
    public void addTranslation(final Locale language, final String text)
    {
        translationMap.put(language.toString(), text);
    }

    /**
     * Get the Translations of the element.
     * @param language the Translation for language to get
     * @return The Translations
     */
    public String getTranslation(final Locale language)
    {
        if (translationMap.containsKey(language.toString()))
        {
            return translationMap.get(language.toString());
        } else
        {
            throw new TranslationNotFoundException(getName(), language, "");
        }
    }

    /**
     * Get the Translation types defined for the element.
     * @return The languages this text has translations
     */
    public Set<Locale> getTranslationLanguages()
    {
        Set<String> keySet = translationMap.keySet();
        Set<Locale> localeSet = new HashSet<Locale>();
        for (String localeString : keySet)
        {
            localeSet.add(new Locale(localeString));
        }
        return localeSet;
    }
}
