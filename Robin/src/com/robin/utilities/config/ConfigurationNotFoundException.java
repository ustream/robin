/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.utilities.config;

public class ConfigurationNotFoundException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public ConfigurationNotFoundException(final String key)
    {
        super(String.format("Configuration not found: %s", key));
    }
}
