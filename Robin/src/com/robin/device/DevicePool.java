/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.device;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.safs.android.auto.lib.DUtilities;
import org.testng.Assert;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.RawImage;

import com.robin.reporter.Reporter;

public class DevicePool
{

    private static final boolean DEVICE_READY = true;
    private static final boolean DEVICE_IN_USE = false;
    private static final IDeviceChangeListener DEVICE_LISTENER =
        new RobinDeviceChangeListener();

    /**
     * Stores device usage flags, true if free, false when used.
     */
    private static final Map<IDevice, Boolean> DEVICES = Collections
        .synchronizedMap(new ConcurrentHashMap<IDevice, Boolean>());
    private static final int WAITDEVICETIMEOUT = 5000;

    public static void init(final int minDeviceNumber)

    {
        DUtilities.getAndroidDebugBridge();
        AndroidDebugBridge.addDeviceChangeListener(DEVICE_LISTENER);

        synchronized (DEVICES)
        {
            waitForConnectedDevices(minDeviceNumber);
        }
    }

    public static void addDeviceToList(final IDevice device)
    {
        synchronized (DEVICES)
        {
            DEVICES.put(device, DEVICE_READY);
            Reporter.log("Device Pool added device: '"
                + getDeviceDescriptionString(device), true);
            DEVICES.notifyAll();
        }
    }

    public static void removeDeviceFromList(final IDevice device)
    {
        synchronized (DEVICES)
        {
            DEVICES.remove(device);
            Reporter.log("Device Pool removed device : '"
                + device.getSerialNumber(), true);
            DEVICES.notifyAll();
        }
    }

    public static IDevice getDeviceForExecution(final String selectorRegexp)
    {
            IDevice deviceToLock;
        synchronized (DEVICES)
        {
            if (isDeviceExists(selectorRegexp))
            {
                while (true)
                {
                    deviceToLock =
                        getFirstUnlockedMatchingDevice(selectorRegexp);
                    if (deviceToLock != null)
                    {
                        DEVICES.put(deviceToLock, DEVICE_IN_USE);
                        Reporter.log("Locked '"
                            + getDeviceDescriptionString(deviceToLock)
                            + "' for execution. (selector: " + selectorRegexp
                            + ")", true);
                        return deviceToLock;
                    } else
                    {
                        try
                        {
                            DEVICES.wait();
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            } else
            {
                Assert.fail("No matching device found for '" + selectorRegexp
                    + "'. Devices list: " + getDevicesListString());
            }
            }
        return null;
    }

    private static String getDevicesListString()
    {
        String listString = "";
        for (IDevice device : DEVICES.keySet())
        {
            listString += "[" + getDeviceDescriptionString(device) + "]";
        }
        return listString;
    }

    private static IDevice getFirstUnlockedMatchingDevice(final String name)
    {
        List<IDevice> matchingDevices = getMatchingDeviceList(name);
        for (IDevice matchingDevice : matchingDevices)
        {
            if (DEVICES.get(matchingDevice))
            {
                return matchingDevice;
            }
        }
        return null;
    }

    public static boolean isDeviceExists(final String name)
    {
        return getMatchingDeviceList(name).size() > 0;
    }

    public static List<IDevice> getMatchingDeviceList(
        final String deviceSelectorRegexp)
    {
        ArrayList<IDevice> matchingDevices = new ArrayList<IDevice>();
        Set<IDevice> devices = DEVICES.keySet();
        for (IDevice device : devices)
        {
            if (getDeviceDescriptionString(device).matches(
                deviceSelectorRegexp))
            {
                matchingDevices.add(device);
            }
        }
        return matchingDevices;
    }

    public static String getDeviceApiLevel(final IDevice device)
    {
        return device.getProperty(IDevice.PROP_BUILD_API_LEVEL);
    }

    public static String getDeviceManufacturer(final IDevice device)
    {
        return device.getProperty("ro.product.manufacturer");
    }

    public static Locale getDeviceLanguage(final IDevice device)
    {
        final String language = device.getProperty("persist.sys.language");
        final String country = device.getProperty("persist.sys.country");
        return new Locale(language, country);
    }

    public static String getDeviceModel(final IDevice device)
    {
        return device.getProperty("ro.product.model");
    }

    public static String getDeviceDisplayResolution(final IDevice device)
    {
        RawImage tmpImage;
        try
        {
            tmpImage = device.getScreenshot();
        } catch (Exception e)
        {
            return "?x?" + e.getMessage();
        }
        return "" + tmpImage.width + "x" + tmpImage.height;
    }

    public static String getDeviceDescriptionString(final IDevice device)
    {
        if (device != null)
        {
            final String sep = "_";
            String name =
                getDeviceManufacturer(device) + sep + getDeviceModel(device)
                    + sep + getDeviceApiLevel(device) + sep
                    + getDeviceDisplayResolution(device) + sep
                    + device.getSerialNumber() + sep
                    + getDeviceLanguage(device);
            return name.replace(" ", "_");
        }
        return "???";
    }

    public static void unlockDevice(final Method method, final IDevice device)
    {
        synchronized (DEVICES)
        {
            final String deviceDescriptionString =
                getDeviceDescriptionString(device);
            if (DEVICES.containsKey(device))
            {
                if (!DEVICES.get(device))
                {
                    DEVICES.put(device, DEVICE_READY);
                    Reporter.log("Unlocked '" + deviceDescriptionString
                        + "', now waits for execution.", true);
                    DEVICES.notifyAll();
                } else
                {
                    DEVICES.notifyAll();
                    Assert.fail("The '" + deviceDescriptionString
                        + "' device is unlocked already.");
                }
            } else
            {
                DEVICES.notifyAll();
                Assert.fail("No matching ('" + deviceDescriptionString
                    + "') device found for method "
                    + method.getDeclaringClass().getName() + "."
                    + method.getName() + ".");
            }
        }
    }

    public static void waitForConnectedDevices(final int numOfDevices)
    {
        synchronized (DEVICES)
        {
            boolean success = true;
            while (DEVICES.size() < numOfDevices && success)
            {
                success = waitForDevices("waiting for " + numOfDevices
                    + " device(s) to connect.");
            }
        }
    }

    private static boolean waitForDevices(final String timeoutMessage)
    {
        try
        {
            long tBefore = System.currentTimeMillis();
            DEVICES.wait(WAITDEVICETIMEOUT);
            if (System.currentTimeMillis() - tBefore > WAITDEVICETIMEOUT)
            {
                return false;
            }
        } catch (InterruptedException e)
        {
            return false;
        }
        return true;
    }
}
