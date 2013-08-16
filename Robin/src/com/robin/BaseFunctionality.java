/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.testng.Assert;
import org.testng.TestException;

import com.android.ddmlib.IDevice;
import com.jayway.android.robotium.remotecontrol.solo.Solo;

import com.robin.capture.Email;
import com.robin.device.DevicePool;
import com.robin.reporter.Reporter;
import com.robin.testcase.BaseTest;
import com.robin.testcase.TestCaseSetup;
import com.robin.uielements.Locator;
import com.robin.uielements.TranslationFile;
import com.robin.uielements.TranslationMap;
import com.robin.utilities.Utilities;
import com.robin.utilities.android.IntentReceiver;
import com.robin.utilities.config.Configuration;
import com.robin.utilities.config.RobinConfiguration;
import com.robin.utilities.email.EMail;

/**
 * Store common states and behaviours for BaseTest and BasePage.
 * */
public class BaseFunctionality
{
    /**
     * Default device list wait.
     */
    protected static final int DEVICE_LIST_TIMEOUT = 30000;

    /**
     * Default device list polling time.
     */
    protected static final int DEVICE_LIST_POLLING_TIME = 1000;

    private static Configuration config = new RobinConfiguration();

    private static Utilities utils = new Utilities();

    /**
     * Stores thread specific testBase objects.
     */
    private static Map<Long, TestCaseSetup> testSetups = Collections
        .synchronizedMap(new HashMap<Long, TestCaseSetup>());

    /**
     * Basic test case related functionalities.
     */
    private static RobinBaseTest test = new RobinBaseTest();

    /**
     * Basic reporting related functionalities.
     */
    private static RobinBaseReporting log = new RobinBaseReporting();

    // DEFAULT TIMEOUTS

    protected final int defaultActivityTimeOut = 90000;

    protected final int defaultElementTimeOut = 60000;

    protected final int defaultEMailTimeOut = 60000;

    protected final int defaultPollingInterval = 300;

    /**
     * Basic solo related functionalities.
     */
    private RobinBaseFunctions core;

    /**
     * Basic action functionalities.
     */
    private RobinBaseActions actions;

    /**
     * Basic checking functionalities.
     */
    private RobinBaseChecks checks;

    public static Configuration config()
    {
        return config;
    }

    public static Utilities utils()
    {
        return utils;
    }

    public class RobinBaseActions
    {
        public void rotateToLandscape(final int... indexOfSolo)
        {
            rotateOrientation(Solo.LANDSCAPE, indexOfSolo);
        }

        public void rotateToPortrait(final int... indexOfSolo)
        {
            rotateOrientation(Solo.PORTRAIT, indexOfSolo);
        }

        private void rotateOrientation(final int orientation,
            final int... indexOfSolo)
        {
            log().line(
                "Rotating orientation to "
                    + log().valueStyleString(
                        orientation == Solo.LANDSCAPE ? "landscape"
                            : "portrait") + ".",
                            Reporter.FLASH_EVENT_STYLE,
                            indexOfSolo);
            try
            {

                test().solo(indexOfSolo).setActivityOrientation(orientation);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }
    }

    public RobinBaseActions act()
    {
        if (actions == null)
        {
            actions = new RobinBaseActions();
        }
        return actions;
    }

    public class RobinBaseFunctions
    {
        public TranslationFile getTranslationsByName(
            final String elementsFileName)
        {
            return TranslationMap.getTranslationsByName(elementsFileName);
        }

        /**
         * Generate a new email address from a used address.
         * @param emailAddress the used email (e.g.:test@gmail.com)
         * @return newEmail new email address(test+DATE@gmail.com)
         */
        public String newEmailAddress(final String emailAddress)
        {
            final String eventStyle = Reporter.READ_EVENT_STYLE;

            String[] array = emailAddress.split("@");
            String newEmail;
            synchronized (this)
            {
                newEmail =
                    array[0] + "+"
                        + utils().now(Calendar.MILLISECOND).getTime() + "@"
                        + array[1];
            }
            log().lastInLine("Generating new email address... ", eventStyle);
            log().lastInLine(
                "The generated email address is "
                    + log().valueStyleString(newEmail),
                    eventStyle);

            return newEmail;
        }

        public String userInEmailAddress(final String emailAddress)
        {
            return emailAddress.split("\\+")[0];
        }

        public void setDeviceLocale(final IDevice device,
            final Locale locale)
        {
            IntentReceiver receiver = new IntentReceiver();
            String command =
                "am broadcast -a jp.co.c_lis.ccl.morelocale.CHANGE_LOCALE"
                    + " -e LOCALE_LANGUAGE "
                    + locale.getLanguage().toLowerCase()
                    + " -e LOCALE_VARIANT \"\"" + " -e LOCALE_COUNTRY "
                    + locale.getCountry().toUpperCase();
            String result = null;
            try
            {
                System.out.println("Execute intent on device "
                    + device.getSerialNumber() + ": " + command);
                device.executeShellCommand(command, receiver);
                result = receiver.getErrorMessage();
            } catch (Exception e)
            {
                Assert.fail("Failed to change device language.", e);
                e.printStackTrace();
            }
            Assert.assertNull(result, "Error during intent broadcast '"
                + command + "' : " + result);
            Assert.assertTrue(
                locale.equals(core().getDviceLocale(device)),
                "Device " + device.getSerialNumber() + " did not set language "
                    + locale);
        }

        public Locale getDviceLocale(final IDevice device)
        {
            return DevicePool.getDeviceLanguage(device);
        }
    }

    public RobinBaseFunctions core()
    {
        if (core == null)
        {
            core = new RobinBaseFunctions();
        }
        return core;
    }

    public static class RobinBaseReporting
    {
        public void line(final String message, final String style,
            final int... indexOfSolo)
        {
            test()
            .setup()
            .getReporter()
            .log(sessionReportSign(indexOfSolo) + message, style);
        }

        public void firstInLine(final String message, final String style,
            final int... indexOfSolo)
        {
            test()
            .setup()
            .getReporter()
            .logInLine(sessionReportSign(indexOfSolo) + message, style);
        }

        public void inLine(final String message, final String style)
        {
            firstInLine(message, style);
        }

        public void lastInLine(final String message, final String style)
        {
            line(message, style);
        }

        public void tic()
        {
            test().setup().tic();
        }

        private String toc()
        {
            return test().setup().toc();
        }

        public void toc(final String style)
        {
            lastInLine(" " + valueStyleString(toc()) + " ms.", style);
        }

        /**
         * Generates a string to sign solo session related reporter
         * character sequence.
         * @param indexOfSolo index of solo in use
         * @return the string that will appear for the given report line for the
         *         current solo session in use returns empty string if only
         *         one solo session is available.
         */
        private String sessionReportSign(final int... indexOfSolo)
        {
            if (indexOfSolo.length > 0
                && test().lastSoloIndex() > 0)
            {
                String sign = "";
                for (int i = 0; i <= indexOfSolo[0]; i++)
                {
                    sign = sign + ">";
                }
                return sign + " ";
            } else
            {
                return "";
            }
        }

        public String valueStyleString(final String message)
        {
            return Reporter.getDiv(Reporter.VALUE_STYLE, message);
        }

        public String locatorStyleString(final Locator locator)
        {
            return Reporter.getDiv(Reporter.LOCATOR_STYLE, locator.getValue());
        }

        public String elementStyleString(final String message)
        {
            return Reporter.getDiv(Reporter.ELEMENT_NAME_STYLE, message);
        }
    }

    public RobinBaseReporting log()
    {
        if (log == null)
        {
            log = new RobinBaseReporting();
        }
        return log;
    }

    public static class RobinBaseTest
    {
        public String sessionReportSign(final int... indexOfSolo)
        {
            return log.sessionReportSign(indexOfSolo);
        }

        public TestCaseSetup setup()
        {
            synchronized (testSetups)
            {
                return testSetups.get(threadId());
            }
        }

        public void addSetup(final TestCaseSetup testCaseSetup)
        {
            synchronized (testSetups)
            {
                testSetups.put(threadId(), testCaseSetup);
            }
        }

        public String deviceDescriptionString(final int... indexOfSolo)
        {
            return DevicePool.getDeviceDescriptionString(setup().getDevice(
                indexOfSolo));
        }

        public int lastSoloIndex()
        {
            return setup().getLastSoloIndex();
        }

        public String className()
        {
            return setup().getTestClassName();
        }

        public String methodName()
        {
            return setup().getMethodName();
        }

        public String screenShotFileName(final int... indexOfSolo)
        {
            return fileName(indexOfSolo) + ".png";
        }

        public String fileName(final int... indexOfSolo)
        {
            return className() + "_" + methodName() + "_"
                + deviceDescriptionString(indexOfSolo) + "_"
                + utils().getTimeStamp();
        }

        public Solo solo(final int... indexOfSolo)
        {
            return setup().getSolo(indexOfSolo);
        }

        public long threadId()
        {
            return Thread.currentThread().getId();
        }

        public int verbLevel()
        {
            return setup().getReporter().getVerbLevel();
        }

        public void decreaseVerbosity()
        {
            setup().getReporter().decreaseVerbosity();
        }

        public void increaseVerbosity()
        {
            setup().getReporter().increaseVerbosity();
        }

    }


    public static RobinBaseTest test()
    {
        if (test == null)
        {
            test = new RobinBaseTest();
        }
        return test;
    }

    public class RobinBaseChecks
    {
        public void activityLoaded(final String acitivyName,
            final int... indexOfSolo)
        {
            final String eventStyle = Reporter.WAIT_EVENT_STYLE;
            log().firstInLine(
                "Waiting for the activity "
                    + log()
                    .elementStyleString(acitivyName)
                    + " to appear... ",
                    eventStyle);
            log().tic();
            boolean success = false;
            try
            {
                success = test().solo(indexOfSolo).waitForActivity(
                    acitivyName,
                    defaultElementTimeOut);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            String currentActivity = "";
            if (!success)
            {
                try
                {
                    currentActivity =
                        test().solo(indexOfSolo).getCurrentActivity();
                } catch (Exception e)
                {
                    e.printStackTrace();
                } finally
                {
                    Assert.assertTrue(success, "The '" + acitivyName
                        + "' activity did not become active within "
                        + defaultElementTimeOut + "ms. Current activity is '"
                        + currentActivity + "'.");
                }
            }
            log().toc(eventStyle);
        }

        public void activityLoaded(
            @SuppressWarnings("rawtypes") final Class activityClass,
            final int... indexOfSolo)
        {
            final String activityName = activityClass.getSimpleName();
            activityLoaded(activityName, indexOfSolo);
        }

        public EMail newMail(final String username, final String password)
        {
            final String readEventStyle = Reporter.READ_EVENT_STYLE;
            log().inLine(
                "Waiting for new mail to arrive at "
                    + log().valueStyleString(username) + "@gmail.com ...",
                    readEventStyle);
            log().tic();
            EMail mail =
                utils().waitForMailSubjectContains(
                    username,
                    password,
                    "",
                    defaultEMailTimeOut);
            log().toc(readEventStyle);
            final String fileName =
                BaseTest.test().screenShotFileName().replace(".png", ".html");
            Email.capture(fileName, mail);
            return mail;
        }

        public EMail newMailSubjectContains(final String username,
            final String password, final String subject)
        {
            final String eventStyle = Reporter.READ_EVENT_STYLE;
            log().inLine(
                "Waiting for new mail with " + log().valueStyleString(subject)
                + " contained subject to arrrive at "
                + log().valueStyleString(username) + "@gmail.com ...",
                eventStyle);
            log().tic();
            EMail mail =
                utils().waitForMailSubjectContains(
                    username,
                    password,
                    subject,
                    defaultEMailTimeOut);
            log().toc(eventStyle);
            final String fileName =
                BaseTest.test().screenShotFileName().replace(".png", ".html");
            Email.capture(fileName, mail);
            return mail;
        }

        public EMail
        newMailToAndSubjectContains(final String username,
            final String password, final String recepient,
            final String subject)
        {
            final String eventStyle = Reporter.READ_EVENT_STYLE;
            log().inLine(
                "Waiting for new mail with " + log().valueStyleString(subject)
                    + " contained subject addressed for "
                    + log().valueStyleString(recepient) + " to arrive ...",
                eventStyle);
            log().tic();
            EMail mail =
                utils().waitForMailToAndSubjectContains(
                    username,
                    password,
                    recepient,
                    subject,
                    defaultEMailTimeOut);
            log().toc(eventStyle);
            final String fileName =
                BaseTest.test().screenShotFileName().replace(".png", ".html");
            Email.capture(fileName, mail);
            return mail;
        }

        public void newEmailBodyToAndSubjectContains(final String userName,
            final String password, final String recepient,
            final String subject, final String... elements)
        {
            boolean checkContains = true;
            EMail email =
                newMailToAndSubjectContains(
                    userName,
                    password,
                    recepient,
                    subject);
            String bodyText = email.getBodyText();
            for (int i = 0; i < elements.length; i++)
            {
                checkContains = bodyText.contains(elements[i]);
                if (checkContains)
                {
                    log().line(
                        "Checking the email body contains the "
                            + log().valueStyleString(elements[i])
                            + " element.",
                            Reporter.CHECKING_EVENT_STYLE);
                } else
                {
                    Assert.fail("The email body doesn't contain the '"
                        + elements[i] + "' element.");
                }
            }
        }
    }

    public RobinBaseChecks check()
    {
        if (checks == null)
        {
            checks = new RobinBaseChecks();
        }
        return checks;
    }
}
