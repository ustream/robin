/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.utilities.config;

public interface Configuration
{
    String getValue(final String key);

    void addConfigFile(final String filename);

    void addConfigFileIfExists(final String filename);
}
