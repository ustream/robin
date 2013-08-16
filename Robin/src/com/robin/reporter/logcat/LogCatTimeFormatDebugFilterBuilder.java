/*
 * Copyright (C) 2013 Ustream Inc.
 * author hrabo <hrabovszki.gyorgy@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.reporter.logcat;

// Concrete Builder
public class LogCatTimeFormatDebugFilterBuilder extends LogCatBuilder
{
    @Override
    public void buildOption()
    {
        logcat.setOption("-v", "threadtime");
    }

    @Override
    public void buildFilter()
    {
        logcat.setFilter("*:D");
    }
}
