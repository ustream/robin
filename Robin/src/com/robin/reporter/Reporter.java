/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.reporter;

import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringEscapeUtils;
import org.testng.ITestResult;

import com.robin.BaseFunctionality;
import com.robin.utilities.config.ConfigParams;

public final class Reporter
{

    public static final String ELEMENT_NAME_STYLE = "loc_name";

    public static final String LOCATOR_STYLE = "locator";

    public static final String VALUE_STYLE = "value";

    public static final String CLICK_EVENT_STYLE = "click";

    public static final String TYPE_EVENT_STYLE = "type";

    public static final String SELECT_EVENT_STYLE = "select";

    public static final String CHECK_EVENT_STYLE = "check";

    public static final String WAIT_EVENT_STYLE = "wait";

    public static final String CONFIG_EVENT_STYLE = "conf";

    public static final String CHECKING_EVENT_STYLE = "checking";

    public static final String JS_EVENT_STYLE = "js_event";

    public static final String READ_EVENT_STYLE = "reading";

    public static final String FLASH_EVENT_STYLE = "flash";

    public static final String FACEBOOK_EVENT_STYLE = "facebook";

    public static final String SCROLLING_EVENT_STYLE = "scroll";

    public static final String NEWLINE = "<br>";

    private static int textIdCounter = 0;

    /**
     * Main verbose level of logging. If this value is greater or equal then the
     * given input verbosity, the reporter print message into the html
     * report.
     */
    private static final int BASE_VERBOSITY = 1;

    private static PrintStream origSysOut = null;

    private static PrintStream logFileOutput = null;

    private Reporter()
    {
    }

    /**
     * Adds an HTML new line sign to the end of the input string.
     * @param lineText any string that will appear in the HTML report
     * @return lineText appended with a new line sign
     */
    private static String addNewLine(final String lineText)
    {
        return lineText + NEWLINE;
    }

    /**
     * Escapes HTML special characters.
     * @param text any string that will appear in the HTML report
     * @return text where HTML characters are escaped
     */
    public static String escapeHtml(final String text)
    {
        return StringEscapeUtils.escapeHtml(text);
    }

    /**
     * Determines weather the given log level allows to write log text or not.
     * @param level is the integer value of the actual log level
     * @return true if the given log level is grater or equals then the base log
     *         level, false otherwise
     */
    private static boolean isLogLevel(final int level)
    {
        return BASE_VERBOSITY >= level;
    }

    /**
     * Paste a new line into the HTML report as xml source, using original
     * testNG reporter class.
     * @param logLineText xml code of a new line in the report
     */
    public static void log(final String logLineText)
    {
        org.testng.Reporter.log(addNewLine(logLineText));
    }

    public static void logConsole(final String logLineText)
    {
        printToStandardOutput(logLineText);
    }

    /**
     * Paste a new line into the HTML report as xml source, using original
     * testNG reporter class.
     * @param logLineText xml code of a new line in the report
     * @param logToStandardOut Whether to print this string on standard out too
     */
    public static void log(final String logLineText,
        final boolean logToStandardOut)
    {
        if (logToStandardOut)
        {
            printToStandardOutput(logLineText);
        }
        org.testng.Reporter.log(addNewLine(logLineText));
    }

    /**
     * Use OutputStreamWriter to encode UTF-8 and print text to standard output.
     * @param logLineText Text to encode and print.
     */
    private static void printToStandardOutput(final String logLineText)
    {
        getOriginalSysOut().println(logLineText);
    }

    /**
     * Paste a new line into the HTML report as xml source, using original
     * testNG reporter class if the current verbosity is equal or greater than
     * the one passed in parameter.
     * @param logLineText xml code of a new line in the report
     * @param level the verbosity of this message
     */
    public static void log(final String logLineText, final int level)
    {
        if (isLogLevel(level))
        {
            org.testng.Reporter.log(addNewLine(logLineText));
        }
    }

    /**
     * Paste a new line into the HTML report as xml source, using original
     * testNG reporter class if the current verbosity is equal or greater than
     * the one passed in parameter.
     * @param logLineText xml code of a new line in the report
     * @param level the verbosity of this message
     * @param logToStandardOut Whether to print this string on standard out too
     */
    public static void log(final String logLineText, final int level,
        final boolean logToStandardOut)
    {
        if (isLogLevel(level))
        {
            if (logToStandardOut)
            {
                printToStandardOutput(logLineText);
            }
            org.testng.Reporter.log(addNewLine(logLineText));
        }
    }

    /**
     * Paste string into the HTML report as xml source, using original testNG
     * reporter class.
     * @param logLineText xml code of a new line in the report
     */
    public static void logInLine(final String logLineText)
    {
        org.testng.Reporter.log(logLineText);
    }

    /**
     * Paste string into the HTML report as xml source, using original testNG
     * reporter class.
     * @param logLineText xml code of a new line in the report
     * @param logToStandardOut Whether to print this string on standard out too
     */
    public static void logInLine(final String logLineText,
        final boolean logToStandardOut)
    {
        if (logToStandardOut)
        {
            printToStandardOutput(logLineText);
        }
        org.testng.Reporter.log(logLineText);
    }

    /**
     * Paste string into the HTML report as xml source, using original testNG
     * reporter class if the current verbosity is equal or greater than the one
     * passed in parameter.
     * @param logLineText xml code of a new line in the report
     * @param level the verbosity of this message
     */
    public static void logInLine(final String logLineText, final int level)
    {
        if (isLogLevel(level))
        {
            org.testng.Reporter.log(logLineText);
        }
    }

    /**
     * Paste string into the HTML report as xml source, using original testNG
     * reporter class if the current verbosity is equal or greater than the one
     * passed in parameter.
     * @param logLineText xml code of a new line in the report
     * @param level the verbosity of this message
     * @param logToStandardOut Whether to print this string on standard out too
     */
    public static void logInLine(final String logLineText, final int level,
        final boolean logToStandardOut)
    {
        if (isLogLevel(level))
        {
            if (logToStandardOut)
            {
                printToStandardOutput(logLineText);
            }
            org.testng.Reporter.log(logLineText);
        }
    }

    /**
     * Get an HTML link definition from an url string.
     * @param url the link url
     * @return the HTML link
     */
    public static String getHtmlLink(final String url)
    {
        return getHtmlLink(url, url);
    }

    /**
     * Get an HTML link definition from an url string and a displayed text
     * string.
     * @param url the link url
     * @param text the link text field to appear
     * @return the HTML link
     */
    public static String getHtmlLink(final String url, final String text)
    {
        return "<a href=\"" + url + "\" target=\"_blank\">" + text + "</a>";
    }

    /**
     * Get a toggle link to be able to hide long output string in the report.
     * @param title the link title
     * @param text the text field to hide/appear
     * @return the toggle part HTML code
     */
    public static String getToogleText(final String title, final String text)
    {
        final String textClass = "textoutput";
        final String textID = textClass + "-" + textIdCounter;
        synchronized (Reporter.class)
        {
            textIdCounter++;
            return addNewLine(String.format(
                "<a title=\"Click to expand/collapse\""
                    + " href=\"javascript:toggleElement('%s', 'block')\">%s"
                    + "</a>",
                textID,
                title))
                + getDiv(textID, textClass, text);
        }
    }

    /**
     * Get a HTML div code with a class attributes.
     * @param divClass the div class attribute
     * @param divText the div text content
     * @return the toggle part HTML code
     */
    public static String getDiv(final String divClass, final String divText)
    {
        return String.format("<div class=\"%s\">%s</div>", divClass, divText);
    }

    /**
     * Get a HTML div code with id and class attributes.
     * @param divID the div id attribute
     * @param divClass the div class attribute
     * @param divText the div text content
     * @return the toggle part HTML code
     */
    public static String getDiv(final String divID, final String divClass,
        final String divText)
    {
        return String.format(
            "<div id=\"%s\" class=\"%s\">%s</div>",
            divID,
            divClass,
            divText);
    }

    /**
     * Get an image insertion definition from an image path string.
     * @param filePath the relative url of the file to the report html
     * @return the image insert HTML string
     */
    public static String getImageInsert(final String filePath)
    {
        return getImageInsert(filePath, filePath);
    }

    /**
     * Get an image insertion definition from an image path string and a
     * displayed text string.
     * @param filePath the relative url of the file to the report html
     * @param text tooltip fot the image
     * @return the image insert HTML string
     */
    public static String getImageInsert(final String filePath,
        final String text)
    {
        return "<img title = \"" + text + "\" src=\"file:///" + filePath
            + "\" target=\"_blank\">" + text + "</img>";
    }

    public static String
        getFileInsert(final String filePath, final String text)
    {
        return "<a href=\"file:///" + filePath + "\" target=\"_blank\">"
            + text + "</a>";
    }

    public static ITestResult getCurrentTestResult()
    {
        return org.testng.Reporter.getCurrentTestResult();
    }

    public static void setCurrentTestResult(final ITestResult result)
    {

        org.testng.Reporter.setCurrentTestResult(result);
    }

    public static PrintStream getOriginalSysOut()
    {
        if (origSysOut == null)
        {
            storeOriginalSysOut();
        }
        return origSysOut;
    }

    public static void storeOriginalSysOut()
    {
        if (origSysOut == null)
        {
            try
            {
                origSysOut = new PrintStream(System.out, true, "UTF-8");
            } catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
                origSysOut = new PrintStream(System.out, true);
            }
        }
    }

    public static PrintStream getLogFileOutputStream()
    {
        if (logFileOutput == null)
        {
            File logFile =
                new File(BaseFunctionality.config().getValue(
                    ConfigParams.LOGFILE));
            try
            {
                logFileOutput = new PrintStream(logFile, "UTF-8");
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return logFileOutput;
    }
}
