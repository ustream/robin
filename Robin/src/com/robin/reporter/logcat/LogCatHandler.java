/*
 * Copyright (C) 2013 Ustream Inc.
 * author hrabo <hrabovszki.gyorgy@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.reporter.logcat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.safs.android.auto.lib.AndroidTools;

import com.robin.BaseFunctionality;
import com.robin.reporter.Reporter;

// Director
public class LogCatHandler
{

    private static AndroidTools androidTools = AndroidTools.get();
    private File logFile;
    private FileWriter writer;
    private List<String> commandParameters;
    private LogCatBuilder logCatBuilder;

    public void setLogCatBuilder(final LogCatBuilder logCatBuilder)
    {
        this.logCatBuilder = logCatBuilder;
    }

    public void constructLogCat()
    {
        logCatBuilder.createLogCat();
        logCatBuilder.buildOption();
        logCatBuilder.buildFilter();
        setLogCatFile();
        setFileWriter();
        setCommandParameters();
        writeLogcat();
    }

    private void setLogCatFile()
    {
        logFile =
            new File(BaseFunctionality.config().getValue("logcat.dir")
                + File.separator + BaseFunctionality.test().fileName()
                + getLogCat().toString() + ".log");
        Reporter.logConsole("LogCat file will be saved at: "
            + logFile.getPath());
    }

    private void setFileWriter()
    {
        try
        {
            writer = new FileWriter(logFile);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void setCommandParameters()
    {
        String logCatParam = "logcat";
        String deviceName =
            BaseFunctionality.test().setup().getDevice().getSerialNumber();
        commandParameters = new ArrayList<String>();
        commandParameters.add(logCatParam);
        commandParameters.addAll(logCatBuilder.getLogCat().getOptions());
        commandParameters.addAll(logCatBuilder.getLogCat().getFilters());
        if (!deviceName.equals(".*"))
        {
            commandParameters.add(0, deviceName);
            commandParameters.add(0, "-s");
        }
        logParameter();
    }

    private void writeLogcat()
    {
        try
        {
            androidTools.adb(commandParameters).connectStdout(writer);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void logParameter()
    {
        String[] parameters =
            commandParameters.toArray(new String[commandParameters.size()]);
        Reporter.logConsole("ATTEMPTING ADB LogCat command: adb "
            + Arrays.toString(parameters));
    }

    private LogCat getLogCat()
    {
        return logCatBuilder.getLogCat();
    }

    public File getLogCatFile()
    {
        return logFile;
    }

    public void closeLogCatFile()
    {
        try
        {
            writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void clearLogCat(final int... indexOfSolo)
    {
        try
        {
            androidTools.adb(
                "-s",
                BaseFunctionality
                    .test()
                    .setup()
                    .getDevice(indexOfSolo)
                    .getSerialNumber(),
                "logcat",
                "-c");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
