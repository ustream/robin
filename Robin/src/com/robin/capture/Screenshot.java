/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.capture;

import java.awt.image.BufferedImage;
import java.io.File;

import org.safs.android.auto.lib.ImageUtils;

import com.android.ddmlib.RawImage;

import com.robin.BaseFunctionality;
import com.robin.reporter.Reporter;
import com.robin.testcase.BaseTest;

public class Screenshot
{
    public static void capture(final String fileName, final int... indexOfSolo)
    {
        RawImage img = null;
        try
        {
            img =
                BaseTest.test().setup().getDevice(indexOfSolo).getScreenshot();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        //TODO: Rotation does not work on all activity.
        // decide if we need to rotate:
        // RawImage rimg = img.getRotated();
        BufferedImage bimg = ImageUtils.convertImage(img);
        File output = FileWriter.capture(fileName, bimg);
        final String relativeFilePath = FileWriter.getRelativePath(output);
        Reporter.log(Reporter.getDiv(
            Reporter.CONFIG_EVENT_STYLE,
            BaseFunctionality.test().sessionReportSign(indexOfSolo)
                + "Screenshot: "
                + Reporter.getDiv(
                    Reporter.VALUE_STYLE,
                    Reporter.getHtmlLink(relativeFilePath, output.getName()))
                + " captured."));
    }

}
