/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin;

import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.robin.capture.Screenshot;
import com.robin.device.DevicePool;
import com.robin.reporter.Reporter;
import com.robin.reporter.logcat.LogCatHandler;
import com.robin.testcase.BaseTest;
import com.robin.testcase.SetupException;
import com.robin.testcase.TestCaseSetup;
import com.robin.uielements.TranslationMap;
import com.robin.utilities.config.ConfigParams;
import com.robin.utilities.config.Configuration;

/**
 * TestNG custom listener to save screenshot and LogCat when a test fails.
 * */
public class ScreenshotListener implements ITestListener,
    IInvokedMethodListener, IExecutionListener
{

    private static int allTestRun = 0;

    private static int allTestSkip = 0;

    private static int allTestFail = 0;

    private static int allConfigFail = 0;

    /**
     * Toggles command line output for test start.
     */
    protected boolean notifyStart = true;

    /**
     * Toggles command line output for Passed tests.
     */
    protected boolean notifyPass = true;

    /**
     * Toggles command line output for Skipped tests.
     */
    protected boolean notifySkip = true;

    /**
     * Toggles command line output for Failed tests.
     */
    protected boolean notifyFail = true;

    /**
     * Toggles screenshot creation for failed tests.
     */
    protected boolean screenshotOnFail = true;


    @Override
    public synchronized void onStart(final ITestContext context)
    {
    }

    @Override
    public void onFinish(final ITestContext arg0)
    {
    }

    @Override
    public void onTestStart(final ITestResult result)
    {
        synchronized (this)
        {
            allTestRun++;
        }
        if (notifyStart)
        {
            Reporter.logConsole(String.format(
                "STARTED %s.",
                getNotifyMessage()));
        }
        TestCaseSetup testSetup = BaseFunctionality.test().setup();
        if (testSetup != null)
        {
            final String deviceName =
                Reporter.getDiv(Reporter.VALUE_STYLE, DevicePool
                    .getDeviceDescriptionString(testSetup.getDevice()));
            Reporter.setCurrentTestResult(result);
            Reporter.log(String.format(Reporter.getDiv(
                Reporter.CONFIG_EVENT_STYLE,
                "Robin session started on %s."), deviceName));
        } else
        {
            Reporter.log(Reporter.getDiv(
                Reporter.CONFIG_EVENT_STYLE,
                "TestCaseSetup instance(s) found for thread "
                    + BaseFunctionality.test().threadId()));
        }
    }

    @Override
    public void onTestSuccess(final ITestResult result)
    {
        if (notifyPass)
        {
            Reporter.logConsole(String
                .format("PASSED %s.", getNotifyMessage()));
        }
        Reporter.setCurrentTestResult(result);
        Reporter.log(Reporter.getDiv(
            Reporter.CONFIG_EVENT_STYLE,
            "Test completed succesfully."));
        try
        {
            deleteLogcat();
        } catch (SetupException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onTestSkipped(final ITestResult result)
    {
        synchronized (this)
        {
            allTestSkip++;
        }
        if (notifySkip)
        {
            Reporter.logConsole(String.format(
                "SKIPPED %s.",
                getNotifyMessage()));
        }
        Reporter.setCurrentTestResult(result);
        Reporter.log(Reporter.getDiv(
            Reporter.CONFIG_EVENT_STYLE,
            "Test is skipped."));
    }

    @Override
    public void onTestFailure(final ITestResult result)
    {
        synchronized (this)
        {
            allTestFail++;
        }
        if (notifyFail)
        {
            Reporter.logConsole(String.format(
                "FAILED %s because of '%s'.",
                getNotifyMessage(),
                result.getThrowable().toString()));
        }
        Reporter.setCurrentTestResult(result);
        createSavedFiles();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(final ITestResult arg0)
    {
    }

    /**
     * Generates a String containing all test specific information: ThreadID,
     * method name, device name.
     * @return the test descriptor string
     */
    protected String getNotifyMessage()
    {
        final long threadID = Thread.currentThread().getId();
        TestCaseSetup testSetup = BaseFunctionality.test().setup();
        if (testSetup != null)
        {
            final String methodName = BaseTest.test().methodName();
            final String deviceName =
                DevicePool.getDeviceDescriptionString(testSetup.getDevice());
            return String.format(
                "(thread %d) '%s' in %s.",
                threadID,
                methodName,
                deviceName);
        } else
        {
            return String.format("(thread %d)", threadID);
        }
    }

    /**
     * Creates Screenshot and LogCat with appropriate filenames.
     */
    private void createSavedFiles()
    {
        try
        {
            for (int i = 0; i <= BaseTest.test().lastSoloIndex(); i++)
            {
                Reporter.log("");
                String lastActivity = "???";
                try
                {
                    lastActivity =
                        BaseTest.test().solo(i).getCurrentActivity();
                } catch (Exception e)
                {
                    Reporter.log("Could not recive last activity due to "
                        + e.getMessage());
                }
                Reporter.log(Reporter.getDiv(
                    Reporter.CONFIG_EVENT_STYLE,
                    "Test failed at "
                        + Reporter.getDiv(Reporter.VALUE_STYLE, lastActivity)
                        + "."));
                final String screenshotName =
                    BaseTest.test().screenShotFileName(i);
                if (screenshotOnFail)
                {
                    Screenshot.capture(screenshotName, i);
                }
                reportLogCat();
            }
        } catch (Exception e)
        {
            Reporter.log(Reporter.getDiv(
                Reporter.CONFIG_EVENT_STYLE,
                "Screenshot listener failure: '"
                    + Reporter.getDiv(Reporter.VALUE_STYLE, e.getMessage())
                    + "'."));
        }
    }

    private void reportLogCat()
    {
        for (LogCatHandler logCatHandler : BaseTest
            .test()
            .setup()
            .getLogCatHandlers())
        {
            logCatHandler.closeLogCatFile();
            Reporter.log(Reporter.getDiv(
                Reporter.CONFIG_EVENT_STYLE,
                "LogCat: "
                    + Reporter.getDiv(Reporter.VALUE_STYLE, Reporter
                        .getFileInsert(logCatHandler
                            .getLogCatFile()
                            .getAbsolutePath(), logCatHandler
                            .getLogCatFile()
                            .getName())) + " saved."));
        }
    }

    private void deleteLogcat() throws SetupException
    {
        for (LogCatHandler logCatHandler : BaseTest
            .test()
            .setup()
            .getLogCatHandlers())
        {
            logCatHandler.closeLogCatFile();
            if (!logCatHandler.getLogCatFile().delete())
            {
                throw new SetupException(new RuntimeException(
                    "Unable to delete logcat file."));
            }
        }
    }

    @Override
    public void afterInvocation(final IInvokedMethod method,
        final ITestResult result)
    {
        if (result.getMethod().isBeforeMethodConfiguration()
            && !isSetupError(result)
            && result.getStatus() == ITestResult.FAILURE)
        {
            synchronized (this)
            {
                allConfigFail++;
            }
            if (notifyFail)
            {
                Reporter.logConsole(String.format(
                    "SETUP FAILED %s because of '%s'.",
                    getNotifyMessage().replaceFirst(
                        BaseTest.test().methodName(),
                        method.getTestMethod().getMethodName() + " ("
                            + BaseTest.test().methodName() + ")"),
                    result.getThrowable().getMessage()));
            }
            Reporter.setCurrentTestResult(result);
            createSavedFiles();
        }
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method,
        final ITestResult result)
    {
    }

    private boolean isSetupError(final ITestResult result)
    {
        return result.getThrowable() != null
            && result.getThrowable() instanceof SetupException;
    }

    @Override
    public void onExecutionStart()
    {
        Configuration config = BaseFunctionality.config();
        Reporter.storeOriginalSysOut();
        System.setOut(Reporter.getLogFileOutputStream());
        TranslationMap.parse(config);
        DevicePool.init(Integer.parseInt(config
            .getValue(ConfigParams.MIN_DEVICE_TO_USE)));
    }

    @Override
    public void onExecutionFinish()
    {
        Reporter.logConsole("Total tests run: " + allTestRun + ", Failures: "
            + allTestFail + ", Skips: " + allTestSkip + ", ConfigFail: "
            + allConfigFail);
    }
}
