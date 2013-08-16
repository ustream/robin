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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.imageio.ImageIO;

import com.robin.BaseFunctionality;
import com.robin.utilities.config.Configuration;

/**
 * @author Zsolt Takacs <takacs.zsolt@ustream.tv>
 */
public class FileWriter
{
    /**
     * Captures a source of the web page and links it into the report.
     * @param fileName the name of the source page to write into.
     * @param content the String representation of the content that will be
     *            written into the file
     * @return the File object representing the created file
     */
    public static File capture(final String fileName,
        final String content)
    {
        File outputFile = createNewUniqueFilename(fileName);
        try
        {
            FileOutputStream fos = new FileOutputStream(outputFile);
            Writer writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(content);
            writer.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return outputFile;
    }

    public static String getRelativePath(final File output)
    {
        return output
            .getPath()
            .replace(BaseFunctionality.config().getValue("report.dir"), ".")
            .replace(File.separator, "/");
    }

    /**
     * Captures a source of the web page and links it into the report.
     * @param fileName the name of the source page to write into.
     * @param image the raw image representation of the image that will be
     *            written into the file
     * @return the File object representing the created file
     */
    public static File capture(final String fileName, final BufferedImage image)
    {
        File outputFile = createNewUniqueFilename(fileName);
        try
        {
            ImageIO.write(image, "png", outputFile);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return outputFile;
    }

    /**
     * Captures a source of the web page and links it into the report.
     * @param fileName the name of the source page to write into.
     * @param content the byte[] representation of the content that will be
     *            written into the file
     * @return the File object representing the created file
     */
    public static File capture(final String fileName,
        final byte[] content)
    {
        File outputFile = createNewUniqueFilename(fileName);
        try
        {
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(content);
            fos.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return outputFile;
    }

    private static File createNewUniqueFilename(final String fileName)
    {
        final String baseFileName =
            fileName.substring(0, fileName.lastIndexOf('.'));
        final String fileExtension =
            fileName.substring(fileName.lastIndexOf('.'), fileName.length());
        Configuration config = BaseFunctionality.config();
        final String imagePath = config.getValue("screenshot.dir");
        String finalFileName = fileName;
        try
        {
            final int maxTryNum = 100;
            int tryNum = 1;
            File imageDir = new File(imagePath);
            if (!imageDir.exists())
            {
                if (!imageDir.mkdirs())
                {
                    throw new IOException("Could not create '" + imagePath
                        + "' directory.");
                }
            }
            while (new File(imagePath + File.separator + finalFileName)
                .exists() && maxTryNum > tryNum)
            {
                finalFileName =
                    baseFileName + "_" + String.format("%02d", tryNum)
                        + fileExtension;
                tryNum++;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return new File(imagePath + File.separator + finalFileName);
    }


}
