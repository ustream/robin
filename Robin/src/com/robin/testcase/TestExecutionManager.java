/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.testcase;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.testng.Assert;

import com.robin.device.DevicePool;
import com.robin.testcase.annotations.MultiDevice;
import com.robin.testcase.annotations.Sequential;

/**
 * Manages method starts when multi-device and sequential conditions must be
 * met.
 *
 * @author ChaotX
 */
public class TestExecutionManager
{
    /**
     * Stores list of methods currently running.
     */
    private static ArrayList<Method> runningMethods = new ArrayList<Method>();

    /**
     * Suspend the actual thread that is going to run the next test method till
     * the conditions defined by runtime annotations \@MultiDevice and
     * \@Sequential are met for the method to start.
     * @param method the Method that is going to be run
     * @param deviceSelector device selector regexp for the method
     */
    public static void
        registerMethod(final Method method, final String deviceSelector)
    {
        final int methodMaxDevice = getMethodMaxDeviceUsage(method);
        final boolean sequentialMethod = isMethodSequential(method);
        final boolean globallySequentialMethod =
            isMethodGloballySequential(method);
        synchronized (runningMethods)
        {
            while (true)
            {
                int matchingDeviceNum =
                    DevicePool.getMatchingDeviceList(deviceSelector).size();
                if (matchingDeviceNum >= methodMaxDevice
                    && !isOtherMultiDeviceRunning()
                    && (!sequentialMethod || !globallySequentialMethod
                        && !isMethodAlreadyRunning(method)
                        && !isOtherSequentialClassGroupRunning(method)
                        || !isOtherSequentialMethodRunning()))
                {
                    runningMethods.add(method);
                    return;
                } else
                {
                    Assert.assertTrue(
                        methodMaxDevice <= matchingDeviceNum,
                        "Method " + method.getDeclaringClass().getName() + "."
                            + method.getName() + " need more device ("
                            + methodMaxDevice + ") than available ("
                            + matchingDeviceNum + ") for " + deviceSelector
                            + " device selector expression!");
                    try
                    {
                        runningMethods.wait();
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static boolean isOtherMultiDeviceRunning()
    {
        synchronized (runningMethods)
        {
            for (Method runningMethod : runningMethods)
            {
                if (getMethodMaxDeviceUsage(runningMethod) > 1)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the finisher method running from the runningMethods list and
     * notifies other threads to try re-register their methods.
     * @param method the Method object that is going to be removed from
     *            currently running method list
     */
    public static void unRegisterMethod(final Method method)
    {
        synchronized (runningMethods)
        {
            for (int i = 0; i < runningMethods.size(); i++)
            {
                if (runningMethods.get(i).equals(method))
                {
                    runningMethods.remove(i);
                    runningMethods.notifyAll();
                    return;
                }
            }
            runningMethods.notifyAll();
        }
    }

    private static boolean isMethodAlreadyRunning(final Method method)
    {
        synchronized (runningMethods)
        {
            for (Method runningMethod : runningMethods)
            {
                if (runningMethod.equals(method))
                {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean isOtherSequentialMethodRunning()
    {
        synchronized (runningMethods)
        {
            for (Method runningMethod : runningMethods)
            {
                if (isMethodSequential(runningMethod))
                {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean isOtherSequentialClassGroupRunning(
        final Method method)
    {
        synchronized (runningMethods)
        {
            if (isMethodSequential(method))
            {
                Class<?>[] classGroups =
                    method.getAnnotation(Sequential.class).groupClasses();
                for (int i = 0; i < classGroups.length; i++)
                {
                    for (Method runningMethod : runningMethods)
                    {
                        if (isMethodSequential(runningMethod))
                        {
                            Class<?>[] runningGroups =
                                method
                                    .getAnnotation(Sequential.class)
                                    .groupClasses();
                            for (int j = 0; j < runningGroups.length; j++)
                            {
                                if (runningGroups[j].equals(classGroups[i]))
                                {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
    }

    private static int getMethodMaxDeviceUsage(final Method method)
    {
        MultiDevice annotation = method.getAnnotation(MultiDevice.class);
        if (annotation != null)
        {
            return annotation.maxDeviceUsed();
        }
        return 1;
    }

    private static boolean isMethodSequential(final Method method)
    {
        return method.getAnnotation(Sequential.class) != null;
    }

    private static boolean isMethodGloballySequential(final Method method)
    {
        Sequential annotation = method.getAnnotation(Sequential.class);
        if (annotation != null)
        {
            return annotation.globally();
        }
        return false;
    }
}
