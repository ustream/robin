/*
 * Copyright (C) 2013 Ustream Inc.
 * author hrabo <hrabovszki.gyorgy@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.reporter.logcat;

import java.util.ArrayList;
import java.util.List;

// Product
public class LogCat
{
    private List<String> options = new ArrayList<String>();
    private List<String> filters = new ArrayList<String>();

    public void setOption(final String... options)
    {
        for (String str : options)
        {
            this.options.add(str);
        }
    }

    public void setFilter(final String... filter)
    {
        for (String str : filter)
        {
            this.filters.add(str);
        }
    }

    public List<String> getOptions()
    {
        return options;
    }

    public List<String> getFilters()
    {
        return filters;
    }

    public String toString()
    {
        String optionsString = "";
        String filter = "";
        for (String str : options)
        {
            optionsString += "_" + str;
        }
        for (String str : filters)
        {
            filter += "_" + str.replace(":", "-").replace("*", "");
        }
        return optionsString + filter;
    }
}
