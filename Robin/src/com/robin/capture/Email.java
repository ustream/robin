/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.capture;

import java.io.File;

import com.robin.reporter.Reporter;
import com.robin.testcase.BaseTest;
import com.robin.utilities.email.EMail;

/**
 * @author Zsolt Takacs <takacs.zsolt@ustream.tv>
 */
public class Email
{
    /**
     * Captures a source of the web page and links it into the report.
     * @param fileName the name of the source page to write into.
     * @param mail the email object to write its content to file and report it
     */
    public static void capture(final String fileName, final EMail mail)
    {
        File output = FileWriter.capture(fileName, mail.getBodyText());
        BaseTest
            .test()
            .setup()
            .getReporter()
            .log(
                "Mail arrived with subject "
                    + Reporter.getDiv(Reporter.VALUE_STYLE, Reporter
                        .getHtmlLink(
                            FileWriter.getRelativePath(output),
                            mail.getSubject())) + ".",
                Reporter.READ_EVENT_STYLE);
    }
}
