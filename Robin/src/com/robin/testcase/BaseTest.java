/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.testcase;

import java.io.File;
import java.lang.reflect.Method;

import org.safs.android.auto.lib.DUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.android.ddmlib.IDevice;
import com.jayway.android.robotium.remotecontrol.solo.Solo;

import com.robin.BaseFunctionality;
import com.robin.device.DevicePool;
import com.robin.reporter.Reporter;
import com.robin.reporter.logcat.LogCatHandler;
import com.robin.reporter.logcat.LogCatTimeFormatDebugFilterBuilder;
import com.robin.utilities.Utilities;
import com.robin.utilities.android.AndroidUtilities;
import com.robin.utilities.config.ConfigParams;

/**
 * This classes purpose is to be the Base for all Test classes. Contains setup
 * and teardown phase. Also it opens and reads the local configuration file.
 */
public class BaseTest extends BaseFunctionality
{
    private static final String APK_EXT = ".apk";

    private static Object testRunnerApkLock = new Object();

    private static Object messengerApkLock = new Object();

    private static Object soloInitLock = new Object();

    /**
     * Performing setup for tests.
     * 1. Rebuild / install apks
     * 2. Starting aut
     * The following @Optional parameters can be given (if not specified by the
     * testng.xml, then the Robin.properties are used):
     * @param method the current method that testng is running
     * @param autFilePath path to the application under test .apk file
     * @param deviceSelectorString regular exp. to match device names
     */
    @Parameters({"aut", "device" })
    @BeforeMethod(alwaysRun = true)
    public void setUp(final Method method,
        @Optional("") final String autFilePath,
        @Optional("") final String deviceSelectorString)
    {
        genericSetup(method, autFilePath, deviceSelectorString);
        TestCaseSetup testSetup = test().setup();
        TestExecutionManager.registerMethod(
            method,
            testSetup.getDeviceSelectorRegexp());
        createFilesForTest(testSetup.getAutApk());
        startRobotium(
            testSetup.getAutApk(),
            testSetup.getDeviceSelectorRegexp());
    }

    private void createFilesForTest(final File autFile)
    {
        final boolean isDebug = true;
        ApkResigner.resignAUT(autFile);
        File autAPK = ApkResigner.getReSignedAUT(autFile);

        File messengerApk = getDesiredMessengerApk(isDebug);
        if (!messengerApk.exists())
        {
            synchronized (messengerApkLock)
            {
                Reporter.log(
                    "Building messenger apk: '"
                        + messengerApk.getAbsolutePath() + "'",
                    true);
                final String messengerSourcePath =
                    config().getValue(ConfigParams.MESSENGER_SOURCE);
                boolean success =
                    DUtilities.buildAPK(messengerSourcePath, isDebug);
                Assert.assertTrue(
                    success,
                    "Could not build '" + messengerApk.getAbsolutePath()
                        + "'.");
                Utilities.moveFileAndReplace(
                    new File(messengerSourcePath + File.separator + "bin"
                        + File.separator + messengerApk.getName()),
                    messengerApk);
            }
        }

        // Robotium test runner apk build and install
        File desiredRunnerApk = getDesiredRunnerApkFile(autAPK);
        if (!desiredRunnerApk.exists())
        {
            synchronized (testRunnerApkLock)
            {
                final String runnerSourcePath =
                    config().getValue(ConfigParams.TEST_RUNNER_SOURCE_DIR);
                final String runnerSourceBinPath =
                    runnerSourcePath + File.separator + "bin" + File.separator;
                final String runnerAPKPath =
                    runnerSourceBinPath + "RobotiumTestRunner"
                        + getFilenameDebugPostfix(isDebug) + APK_EXT;
                Reporter.log("Attempt to rebuild testrunner source at '"
                    + runnerSourcePath + "'", true);
                final String runnerApk =
                    DUtilities.rebuildTestRunnerApk(
                        runnerSourcePath,
                        autAPK.getAbsolutePath(),
                        runnerAPKPath,
                        DUtilities.TEST_RUNNER_INSTRUMENT,
                        isDebug);
                Assert.assertNotNull(runnerApk, "Could not build "
                    + runnerAPKPath);
                if (!isFilePresent(runnerApk))
                {
                    Reporter.log(runnerApk
                        + "' runner apk not found -> force rebuilding!'", true);
                    boolean rebildSuccess =
                        DUtilities.buildAPK(runnerSourcePath, isDebug);
                    Assert.assertTrue(
                        rebildSuccess,
                        "Could not rebuild test runner apk.");
                }
                Utilities.moveFileAndReplace(
                    new File(runnerApk),
                    desiredRunnerApk);
            }
        }
    }

    private String getFilenameDebugPostfix(final boolean isDebug)
    {
        return isDebug ? "-debug" : "";
    }

    private File getDesiredMessengerApk(final boolean isDebug)
    {
        String messengerFileName =
            "SAFSTCPMessenger" + getFilenameDebugPostfix(isDebug) + APK_EXT;
        final String messengerAPKPath =
            config().getValue(ConfigParams.RESIGNED_AUT_PATH) + File.separator
                + messengerFileName;
        return new File(messengerAPKPath);
    }

    private void genericSetup(final Method method, final String authFilePath,
        final String deviceSelectorRegexp)
    {
        TestCaseSetup testCaseSetup =
            RobinTestCaseSetup.createFromParametersAndFallbackConfig(
                authFilePath,
                deviceSelectorRegexp,
                config());
        test().addSetup(testCaseSetup);

        testCaseSetup.setTestClassName(getClass().getSimpleName());
        testCaseSetup.setMethodName(method.getName());
        checkAutFile(test().setup().getAutApk().getAbsolutePath());
        checkDeviceAvailable(test().setup().getDeviceSelectorRegexp());
    }

    private void checkDeviceAvailable(final String deviceName)
    {
        Assert.assertTrue(DevicePool.isDeviceExists(deviceName), deviceName
            + " is not in the Device Pool.");

    }

    private void checkAutFile(final String path)
    {
        Assert.assertTrue(new File(path).exists(), path + " is not found!");
    }

    /**
     * Performing a tear down after each test
     * 1. Capturing the last screen for logging
     * 2. Stoping solo
     * @param method the method that was run before this teardown
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(final Method method)
    {
        try
        {
            genericTearDown();
        } finally
        {
            for (int i = 0; i <= test().setup().getLastSoloIndex(); i++)
            {
                DevicePool.unlockDevice(method, test()
                    .setup()
                    .getDevice(i));
            }

            TestExecutionManager.unRegisterMethod(method);
        }
    }

    private void genericTearDown()
    {
        test().setup().getReporter().log(
            "Closing solo(s).",
            Reporter.CONFIG_EVENT_STYLE);
        for (int i = 0; i <= test().lastSoloIndex(); i++)
        {
            stopRobotium(i);
        }
    }

    /**
     * Performing setup and start of a new solo session on the current host
     * and port setup with current device type.
     * @return the index of the solo object stored in the current
     *         TestCaseSetup
     */
    protected int startNewDevice()
    {
        TestCaseSetup testSetup = test().setup();
        startRobotium(
            testSetup.getAutApk(),
            testSetup.getDeviceSelectorRegexp());
        return test().lastSoloIndex();
    }

    private void logTestcaseSetup()
    {
        final String deviceReportString =
            log().valueStyleString(test().setup().getDeviceSelectorRegexp());
        test().setup()
            .getReporter()
            .log(
                String.format(
                    "Testcase setup for device %s.",
                    deviceReportString),
                Reporter.CONFIG_EVENT_STYLE);
    }

    protected void startRobotium(final File autFile,
        final String deviceSelector)
    {
        IDevice device = DevicePool.getDeviceForExecution(deviceSelector);

        //TODO: Investigate how to handle device language change.
        //core().setDeviceLanguage(device, Language.en_US);

        test().setup().addDevice(device);
        int newSoloIndex = test().lastSoloIndex() + 1;
        addLogCat(newSoloIndex);
        logTestcaseSetup();
        final boolean isDebugBuild = true;
        final boolean isRobotiumDebug =
            Boolean.parseBoolean(config()
                .getValue(ConfigParams.ROBOTIUM_LOGGING));

        final String serial = device.getSerialNumber();
        AndroidUtilities androidUtils = new AndroidUtilities();

        File autAPK = ApkResigner.getReSignedAUT(autFile);
        androidUtils.installReplaceApk(autAPK, device);

        File messengerApk = getDesiredMessengerApk(isDebugBuild);
        androidUtils.installReplaceApk(messengerApk, device);

        // Robotium test runner apk build and install
        File desiredRunnerApk = getDesiredRunnerApkFile(autAPK);
        androidUtils.installReplaceApk(desiredRunnerApk, device);

        androidUtils.unlockDeviceScreen(serial);

        androidUtils.launchTestInstrumentation(
            DUtilities.TEST_RUNNER_INSTRUMENT,
            serial);

        test().setup().addSolo(new Solo());
        Solo solo = test().solo(test().lastSoloIndex());
        solo.setPortForwarding(true);
        try
        {
            synchronized (soloInitLock)
            {
                DUtilities.USE_DEVICE_SERIAL = "-s " + serial;
                solo.initialize();
            }
        } catch (Exception e)
        {
            Assert.fail("Solo initialization failed.", e);
        }
        solo.turnProtocolDebug(isRobotiumDebug);
        solo.turnRunnerDebug(isRobotiumDebug);
        try
        {
            solo.startMainLauncher();
        } catch (Exception e)
        {
            Assert.fail("Solo main launcher failed.", e);
        }
        String mainActivityUID = "";
        try
        {
            mainActivityUID = solo.getCurrentActivity();
        } catch (Exception e)
        {
            Assert.fail("Solo read current activity failed.", e);
        }
        test()
            .setup()
            .getReporter()
            .log(
                String.format(
                    "Robin started on '%s' device. MainActivityUID: %s",
                    log().valueStyleString(
                        DevicePool.getDeviceDescriptionString(test()
                            .setup()
                            .getDevice(newSoloIndex))),
                    log().valueStyleString(mainActivityUID)),
                Reporter.CONFIG_EVENT_STYLE);
    }

    private File getDesiredRunnerApkFile(final File autApk)
    {
        return new File(autApk.getPath().replace(APK_EXT, "") + "_"
            + "RobotiumTestRunner.apk");
    }

    private boolean isFilePresent(final String pathToFile)
    {
        return new File(pathToFile).exists();
    }

    protected void stopRobotium(final int... indexOfSolo)
    {
        Assert.assertTrue(
            test().solo(indexOfSolo).shutdownRemote(),
            "Fail to shutdown remote service.");
        test().solo(indexOfSolo).shutdown();
    }

    private void addLogCat(final int... indexOfSolo)
    {
        constructDebugLevelLongFormatLogCat(indexOfSolo);
    }

    private void constructDebugLevelLongFormatLogCat(final int... indexOfSolo)
    {
        LogCatHandler.clearLogCat(indexOfSolo);
        LogCatHandler logCatHandler = new LogCatHandler();
        logCatHandler
            .setLogCatBuilder(new LogCatTimeFormatDebugFilterBuilder());
        logCatHandler.constructLogCat();
        test().setup().addLogCatHandler(logCatHandler);
    }

}
