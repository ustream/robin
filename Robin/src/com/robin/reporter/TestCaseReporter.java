/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.reporter;

public class TestCaseReporter
{
    /**
     * Default verbosity level parameter.
     */
    private int verbLevel = 1;

    public int getVerbLevel()
    {
        return verbLevel;
    }

    /**
     * Decreases log level verbosity. Call this if further log outputs are less
     * important. Use increaseVerbosity before return of the current function
     * occurs.
     */
    public void decreaseVerbosity()
    {
        verbLevel++;
    }

    /**
     * Increases log level verbosity. Call this if further log outputs are
     * important. Use only if a decreaseVerbosity preceded this call.
     */
    public void increaseVerbosity()
    {
        verbLevel--;
    }

    /**
     * Writes report (with solo session stamp) message inside a div with
     * style valued class attribute.
     * @param message the text message to report
     * @param style the style definition to use for the text div
     */
    public void log(final String message, final String style)
    {
        Reporter.log(Reporter.getDiv(style, message), getVerbLevel());
    }

    /**
     * Writes report (with solo session stamp) message without line break inside
     * a div with style valued class attribute.
     * @param message the text message to report
     * @param style the style definition to use for the text div
     */
    public void logInLine(final String message, final String style)
    {
        Reporter.logInLine(Reporter.getDiv(style, message), getVerbLevel());
    }

}
