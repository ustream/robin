package com.robin.config;

import com.robin.utilities.config.Configuration;

public final class Config
{
    /**
     * Local build properties.
     */
    public static final String BUILD_PROPERTIES = "#basedir#/build.properties";

    /**
     * Robin config file.
     */
    private static final String ROBIN_PROPERTIES =
        "#basedir#/Robin.properties";

    /**
     * Initialize configuration.
     * @param config Configuration
     */
    public static void init(final Configuration config)
    {
        config.addConfigFile(ROBIN_PROPERTIES);
        config.addConfigFileIfExists(BUILD_PROPERTIES);
    }
}
