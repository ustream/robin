/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.utilities.android;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.ddmlib.MultiLineReceiver;

import com.robin.reporter.Reporter;

public class IntentReceiver extends MultiLineReceiver{
    private final String successOutput =
        "Broadcast completed: result=0"; //$NON-NLS-1$
    private final Pattern failurePattern = Pattern
        .compile("Error:\\s+\\[(.*)\\]"); //$NON-NLS-1$
    private String mErrorMessage = null;
    @Override
    public boolean isCancelled()
    {
        return false;
    }

    @Override
    public void processNewLines(final String[] lines)
    {
        for (int i = 0; i < lines.length; i++)
        {
            System.out.println(lines[i]);
            Reporter.logConsole(lines[i]);
        }
        for (String line : lines) {
            if (line.length() > 0) {
                if (line.startsWith(successOutput)) {
                    mErrorMessage = null;
                } else {
                    Matcher m = failurePattern.matcher(line);
                    if (m.matches()) {
                        mErrorMessage = m.group(1);
                    }
                }
            }
        }
    }
    public String getErrorMessage() {
        return mErrorMessage;
    }
}
