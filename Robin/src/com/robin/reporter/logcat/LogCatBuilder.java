/*
 * Copyright (C) 2013 Ustream Inc.
 * author hrabo <hrabovszki.gyorgy@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.reporter.logcat;

// Builder
public abstract class LogCatBuilder
{
    protected LogCat logcat;

    public LogCat getLogCat()
    {
        return logcat;
    }

    public void createLogCat()
    {
        logcat = new LogCat();
    }

    public abstract void buildOption();

    public abstract void buildFilter();
}
