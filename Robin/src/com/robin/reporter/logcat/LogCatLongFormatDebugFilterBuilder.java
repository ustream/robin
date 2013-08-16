/*
 * Copyright (C) 2013 Ustream Inc.
 * author hrabo <hrabovszki.gyorgy@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.reporter.logcat;

// Concrete Builder
public class LogCatLongFormatDebugFilterBuilder extends LogCatBuilder
{
    public void buildOption()
    {
        logcat.setOption("-v", "long");
    }

    public void buildFilter()
    {
        logcat.setFilter("*:D");
    }
}
