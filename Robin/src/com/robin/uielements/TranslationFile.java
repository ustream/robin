/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TranslationFile
{
    private String path;

    private String fileName;

    /**
     * The translations of the activity.
     */
    private Map<String, TranslationElement> elementMap = Collections
        .synchronizedSortedMap(new TreeMap<String, TranslationElement>());

    public String getPath()
    {
        return path;
    }

    public void setPath(final String path)
    {
        this.path = path;
    }

    @Override
    public TranslationFile clone()
    {
        TranslationFile newUI = new TranslationFile();
        newUI.setPath(getPath());
        newUI.setFileName(fileName);
        newUI.elementMap.putAll(elementMap);
        return newUI;
    }

    public void addElement(final TranslationElement elem)
    {
        elementMap.put(elem.getName(), elem);
    }

    public TranslationElement getTranslationElement(final String elementName)
    {
        if (elementMap.containsKey(elementName))
        {
            return elementMap.get(elementName);
        } else
        {
            throw new TranslationNotFoundException(elementName, path);
        }
    }

    public String getTranslationText(final Locale language,
        final String elementName)
    {
        return getTranslation(language, elementName);
    }

    public String
        getTranslation(final Locale language, final String elementName)
    {
        return getTranslationElement(elementName).getTranslation(language);
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(final String fileName)
    {
        this.fileName = fileName;
    }

    public TranslationFile mergeUIPages(final TranslationFile generalUI)

    {
        TranslationFile mergedUI = clone();
        Set<String> generalKeys = generalUI.elementMap.keySet();
        for (String key : generalKeys)
        {
            mergedUI.addElement(generalUI.getTranslationElement(key));
        }
        mergedUI.setPath(generalUI.getPath() + " || " + getPath());
        return mergedUI;
    }
}
