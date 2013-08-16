/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.utilities.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.safs.android.auto.lib.AndroidTools;
import org.safs.android.auto.lib.AntTool;
import org.safs.android.auto.lib.DUtilities;
import org.safs.android.auto.lib.Process2;
import org.testng.Assert;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;

import com.robin.reporter.Reporter;

public class AndroidUtilities
{
    private static final short PROCESS_TIMEOUT_SECOND = 120;

    private final AndroidTools androidTools = DUtilities.getAndroidTools(System
        .getProperty(AndroidTools.ANDROID_HOME_ENV_VAR));

    {
        DUtilities.ROOT_ANT_SDK_DIR = System.getProperty(AntTool.ANT_HOME_ENV);
    }

    public void installReplaceAPK(final File apk, final String serial)
    {
        String targetPackage =
            DUtilities.getTargetPackageValue(apk.getAbsolutePath());
        uninstallAPKPackage(targetPackage, serial, false);
        installAPK(apk, serial);
    }

    public void installReplaceApk(final File apk, final IDevice device)
    {
        Reporter.log("Install&Replace " + apk.getAbsolutePath() + " on "
            + device.getSerialNumber(), true);

        String installResult = "";
        try
        {
            String targetPackageValue = DUtilities.getTargetPackageValue(apk
                .getAbsolutePath());
            String uninstResult = device.uninstallPackage(targetPackageValue);
            if (uninstResult != null)
            {
                Reporter.log("Could not uninstall: " + targetPackageValue
                    + " because " + uninstResult);
            }
            installResult = device.installPackage(apk.getAbsolutePath(), false);
        } catch (InstallException e)
        {
            Assert.fail("Failed to install " + apk.getAbsolutePath() + " to "
                + device.getSerialNumber(), e);
            e.printStackTrace();
        }
        if (installResult != null)
        {
            Reporter.log("Could not install: " + apk.getAbsolutePath()
                + " because " + installResult);
        }
    }

    public void uninstallAPKPackage(final String targetPackage,
        final String serial, final boolean keepData)
    {
        Reporter.log(
            "Uninstall " + targetPackage + " KEEPDATA="
                + String.valueOf(keepData),
            true);

        checkDevice(targetPackage);
        String[] uninstallParamsKeepData =
            { "uninstall", "-k", "application.package" };
        String[] uninstallParams = { "uninstall", "application.package" };
        String[] params =
            keepData ? uninstallParamsKeepData.clone() : uninstallParams
                .clone();
        int field = params.length - 1;
        params[field] = targetPackage;
        params = addDeviceSerialParam(params, serial);
        Process2 process = null;
        Reporter.log(
            "ATTEMPTING ADB uninstall command: adb " + Arrays.toString(params),
            true);
        try
        {
            process = androidTools.adb(params);
            process.forwardOutput().waitForSuccess().destroy();
            process = null;
        } catch (IOException e)
        {
            Assert.fail("Failed to uninstall " + targetPackage + " due to "
                + " IOException, " + e.getMessage());
        } catch (InterruptedException e)
        {
            Assert.fail("Failed to uninstall " + targetPackage + " due to "
                + " InterruptedException, " + e.getMessage());
        } catch (RuntimeException e)
        {
            Assert.fail("Failed to uninstall " + targetPackage + " due to "
                + " RuntimeException, " + e.getMessage());
        } finally
        {
            if (process != null)
            {
                process.destroy();
                process = null;
            }
        }
    }

    private void checkDevice(final String targetPackage)
    {
        Assert.assertTrue(
            DUtilities.waitDevice(),
            "OFFLINE device failure... during installing '"
                + targetPackage + "'");
    }

    private void installAPK(final File apk, final String serial)
    {
        String[] installParams = {"install", "-r", "application.apk"};
        checkDevice(apk.getAbsolutePath());
        String[] params = installParams.clone();
        params[2] = apk.getAbsolutePath();
        params = addDeviceSerialParam(params, serial);
        Process2 process = null;

        Reporter.log(
            "ATTEMPTING ADB install command: adb " + Arrays.toString(params),
            true);
        try
        {
            process = androidTools.adb(params);
            process
                .forwardOutput()
                .waitForSuccess(PROCESS_TIMEOUT_SECOND)
                .destroy();
            Reporter.log("ADB install command successful.", true);
        } catch (IOException e)
        {
            Assert.fail("Failed to run command: adb " + Arrays.toString(params)
                + " IOException: " + e.getMessage());

        } catch (InterruptedException e)
        {
            Assert.fail("Failed to finish command run: adb "
                + Arrays.toString(params) + " InterruptionException: "
                + e.getMessage());
        } catch (RuntimeException e)
        {
            Assert.fail("Failed to finish command run: adb "
                + Arrays.toString(params) + " RuntimeException: "
                + e.getMessage());
        } finally
        {
            if (process != null)
            {
                process.destroy();
                process = null;
            }
        }
    }

    private static String[] addDeviceSerialParam(final String[] params,
        final String serial)
    {
        final String serialParam = "-s " + serial;
        String[] serialParams = serialParam.trim().split(" ");
        String[] newParams = new String[params.length + serialParams.length];
        for (int i = 0; i < serialParams.length; i++)
        {
            newParams[i] = serialParams[i];
        }

        for (int i = 0; i < params.length; i++)
        {
            newParams[i + serialParams.length] = params[i];
        }
        return newParams;
    }

    public void unlockDeviceScreen(final String serial)
    {
        String[] params = new String[] { "shell", "input", "keyevent", "82" };
        Process2 process = null;
        try
        {
            process = androidTools.adb(addDeviceSerialParam(params, serial));
            process
                .forwardOutput()
                .waitForSuccess(PROCESS_TIMEOUT_SECOND)
                .destroy();
        } catch (IOException e)
        {
            Assert.fail("Failed to unlock " + serial + " device screen. "
                + e.getMessage());
        } catch (InterruptedException e)
        {
            Assert.fail("Failed to finish screen lock process on " + serial
                + " device. " + e.getMessage());
        } finally
        {
            if (process != null)
            {
                process.destroy();
                process = null;
            }
        }
    }

    private boolean isTestInsturmentationLaunced(final String instumentation,
        final String serial)
    {
        String[] params = { "shell", "am", "instrument", instumentation };
        final Pattern androidException = Pattern.compile(".*Exception:.*");
        final Pattern instrumentationFailed =
            Pattern.compile(".*INSTRUMENTATION_FAILED.*");
        checkDevice(instumentation);
        boolean instrumentLaunched = true;
        Process2 process = null;
        BufferedReader stdout = null;
        BufferedReader stderr = null;
        params = addDeviceSerialParam(params, serial);
        try
        {
            Reporter.log("INSTRUMENTATION command: adb "
                + Arrays.toString(params));
            process = androidTools.adb(params);
            String tempstr = null;

            stdout = process.getStdoutReader();
            while ((tempstr = stdout.readLine()) != null)
            {
                Reporter.log(tempstr, true);
                if (!instrumentLaunched)
                {
                    continue;
                }
                if (androidException.matcher(tempstr).matches()
                    || instrumentationFailed.matcher(tempstr).matches())
                {
                    instrumentLaunched = false;
                }
            }

            stderr = process.getStderrReader();
            while ((tempstr = stderr.readLine()) != null)
            {
                if (instrumentLaunched)
                {
                    instrumentLaunched = false;
                }
            }

            process.waitForSuccess();

        } catch (IOException e)
        {
            Assert.fail("Failed to launch " + instumentation + " instrument. "
                + e.getMessage());
        } catch (InterruptedException e)
        {
            Assert.fail("Failed to launch " + instumentation + " instrument. "
                + e.getMessage());
        } finally
        {
            if (process != null)
            {
                process.destroy();
            }
            try
            {
                stdout.close();
                stderr.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return instrumentLaunched;
    }

    public void launchTestInstrumentation(final String testinstrumentation,
        final String serial)
    {
        Assert.assertTrue(
            isTestInsturmentationLaunced(testinstrumentation, serial),
            "Failed to launch instrument '" + testinstrumentation + "'");
    }
}
