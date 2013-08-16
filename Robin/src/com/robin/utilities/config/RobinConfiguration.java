/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.utilities.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.SystemUtils;

public final class RobinConfiguration implements Configuration
{

    private static final String BUILD_PROPERTIES = "#basedir#/build.properties";

    private static final String ROBIN_PROPERTIES = "#basedir#/Robin.properties";

    private Properties properties;

    private File baseDir;

    public RobinConfiguration()
    {
        properties = System.getProperties();

        baseDir = new File(properties.getProperty("basedir"));
        if (baseDir == null)
        {
            baseDir = new File(".");
        }

        addConfigFile(ROBIN_PROPERTIES);
        addConfigFileIfExists(BUILD_PROPERTIES);

        if (properties.getProperty(ConfigParams.MIN_DEVICE_TO_USE) == null)
        {
            properties.setProperty(ConfigParams.MIN_DEVICE_TO_USE, "1");
        }
    }

    @Override
    public String getValue(final String key)
    {
        String result = properties.getProperty(key);

        if (null == result)
        {
            throw new ConfigurationNotFoundException(key);
        }

        return result;
    }

    @Override
    public void addConfigFile(final String filename)
    {
        addConfigFile(new File(replaceBaseDirInString(filename)));
    }

    /**
     * Add Config File.
     * @param filename Name of the Configuration file
     */
    public void addConfigFile(final File filename)
    {
        try
        {
            loadPropertiesFromPath(filename);
        } catch (IOException e)
        {
            throw new ConfigurationCantBeLoadedException(filename.getPath());
        }
    }

    private void loadPropertiesFromPath(final File file)
        throws IOException
    {
        Properties propertiesFromFile = new Properties();

        propertiesFromFile.load(getStream(file));

        for (String key : propertiesFromFile.stringPropertyNames())
        {
            String value =
                replaceBaseDirInString(propertiesFromFile.getProperty(key));
            properties.setProperty(key, value);
        }
    }

    @Override
    public void addConfigFileIfExists(final String filename)
    {
        addConfigFileIfExists(new File(replaceBaseDirInString(filename)));
    }


    public void addConfigFileIfExists(final File filename)
    {
        if (filename.exists())
        {
            addConfigFile(filename);
        }
    }

    private FileInputStream getStream(final File file)
    {
        try
        {
            FileInputStream is = new FileInputStream(file);
            return is;
        } catch (FileNotFoundException e)
        {
            throw new ConfigurationFileNotFoundException(file.getPath());
        }
    }

    private String replaceBaseDirInString(final String path)
    {
        String out = path.replace("#basedir#", baseDir.getPath());
        if (SystemUtils.IS_OS_WINDOWS)
        {
            out = separatorsToWindows(out);
        }
        return out;
    }

    private String separatorsToWindows(final String input)
    {
        String out = input.replaceAll("[/]", "\\\\");
        return out;
    }

}
