/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.robin.utilities.config.ConfigParams;
import com.robin.utilities.config.Configuration;

public class TranslationMap
{
    /**
     * To store all translation elements defined in translation xmls.
     */
    private static final Map<String, TranslationFile> ELEMENTMAP = Collections
        .synchronizedMap(new HashMap<String, TranslationFile>());

    /**
     * Parse all translation xmls and store their data in memory.
     * @param config Configuration.
     */
    public static void parse(final Configuration config)
    {
        synchronized (ELEMENTMAP)
        {
            if (ELEMENTMAP.isEmpty())
            {
                File baseFolder =
                    new File(config.getValue(ConfigParams.TLXMLSDIR));
                @SuppressWarnings("unchecked")
                Collection<File> fileList =
                    FileUtils.listFiles(
                        baseFolder,
                        new String[] { "xml" },
                        true);
                for (File file : fileList)
                {
                    ELEMENTMAP.put(
                        baseFolder.toURI().relativize(file.toURI()).getPath(),
                        getTranslationsFromFile(baseFolder, file));
                }
            }
        }
    }

    /**
     * Returns the TranslationFile object stored in memory by its file relative
     * filename to the TLXML base dir.
     * @param filePath the name and relative path of the file parsed
     * @return the file representation  with content from the xml
     */
    public static TranslationFile getTranslationsByName(final String filePath)
    {
        TranslationFile page = ELEMENTMAP.get(filePath);
        if (page == null)
        {
            throw new NullPointerException("Could not find '"
                + filePath + "' UIPage.");
        }
        return page;
    }

    private static TranslationFile getTranslationsFromFile(
        final File baseFolder, final File locatorFile)
    {
        return new TranslationSerializer()
            .deserialize(baseFolder, locatorFile);
    }
}
