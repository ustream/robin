/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.testcase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.android.ddmlib.IDevice;
import com.jayway.android.robotium.remotecontrol.solo.Solo;

import com.robin.BaseFunctionality;
import com.robin.reporter.TestCaseReporter;
import com.robin.reporter.logcat.LogCatHandler;
import com.robin.utilities.config.ConfigParams;
import com.robin.utilities.config.Configuration;
import com.robin.utilities.user.User;

/**
 * @author ChaotX
 */
public class RobinTestCaseSetup implements TestCaseSetup
{
    private TestCaseReporter reporter = new TestCaseReporter();

    private long actTime = 0;

    private ArrayList<Solo> soloList = new ArrayList<Solo>();

    private ArrayList<User> userList = new ArrayList<User>();

    private Map<Long, String> timeStempMap = Collections
        .synchronizedMap(new HashMap<Long, String>());

    private String className;

    private String methodName;

    private String deviceSelectorRegexp;

    private ArrayList<IDevice> devices = new ArrayList<IDevice>();

    private ArrayList<LogCatHandler> logCatHandlers =
        new ArrayList<LogCatHandler>();

    private File aut;

    static TestCaseSetup createFromParametersAndFallbackConfig(
        final String autFilePath, final String deviceSelectorRegexp,
        final Configuration config)
    {
        TestCaseSetup testCaseSetup = new RobinTestCaseSetup();


        testCaseSetup.setAutApk(getAutFileByTestngParameter(
            autFilePath,
            config));

        if (deviceSelectorRegexp.equals(""))
        {
            testCaseSetup.setDeviceSelectorRegexp(config
                .getValue(ConfigParams.DEVICE_SELECTOR));
        } else
        {
            testCaseSetup.setDeviceSelectorRegexp(deviceSelectorRegexp);
        }

        return testCaseSetup;
    }

    static File getAutFileByTestngParameter(final String autFilePath,
        final Configuration config)
    {
        if (autFilePath.equals(""))
        {
            return new File(config.getValue(ConfigParams.AUT_FILE));
        } else
        {
            return new File(autFilePath);
        }
    }

    @Override
    public String getDeviceSelectorRegexp()
    {
        return deviceSelectorRegexp;
    }

    @Override
    public void setDeviceSelectorRegexp(final String deviceRegexp)
    {
        deviceSelectorRegexp = deviceRegexp;
    }

    @Override
    public File getAutApk()
    {
        return aut;
    }

    @Override
    public void setAutApk(final File autFile)
    {
        aut = autFile;
    }

    // - After setup and during test run required data related functions -

	@Override
	public IDevice getDevice(final int... indexOfSolo) {
		int index = indexOfSolo.length > 0 ? indexOfSolo[0] : 0;
		if (devices.size() > 0) {
			return devices.get(index);
		}
		return null;
	}

    @Override
    public void addDevice(final IDevice device)
    {
        devices.add(device);
    }

    /**
     * Getter for className.
     * @return the className
     */
    @Override
    public String getTestClassName()
    {
        return className;
    }

    /**
     * Setter for className.
     * @param classNameParam the className to set
     */
    @Override
    public void setTestClassName(final String classNameParam)
    {
        className = classNameParam;
    }

    /**
     * Getter for methodName.
     * @return the methodName
     */
    @Override
    public String getMethodName()
    {
        return methodName;
    }

    /**
     * Setter for methodName.
     * @param methodNameParam the methodName to set
     */
    @Override
    public void setMethodName(final String methodNameParam)
    {
        methodName = methodNameParam;
    }

    /**
     * Setter for webDriver.
     * @param soloParam the webDriver to set
     */
    @Override
    public void addSolo(final Solo soloParam)
    {
        soloList.add(soloParam);
    }

    /**
     * Getter for webDriver.
     * @param indexOfSolo the index of the solo object to get
     * @return the solo
     */
    @Override
    public Solo getSolo(final int... indexOfSolo)
    {
        if (indexOfSolo.length < 1)
        {
            return soloList.get(0);
        } else
        {
            return soloList.get(indexOfSolo[0]);
        }
    }

    /**
     * Gets the size of the solo list.
     * @return integer
     */
    @Override
    public int getLastSoloIndex()
    {
        return soloList.size() - 1;
    }

    @Override
    public TestCaseReporter getReporter()
    {
        return reporter;
    }

    @Override
    public void addUser(final User user)
    {
        userList.add(user);
    }

    @Override
    public ArrayList<User> getUsers()
    {
        return userList;
    }

    @Override
    public void generateTimeStamp()
    {
        String timeStamp =
            BaseFunctionality.utils().getTimeStamp()
            + BaseFunctionality.utils().generateRandomString(2);
        timeStempMap.put(BaseFunctionality.test().threadId(), timeStamp);
    }

    @Override
    public String getTimeStamp(final Long threadId)
    {
        return timeStempMap.get(threadId);
    }

    /**
     * Stores the current System milliseconds.
     */
    @Override
    public void tic()
    {
        actTime = System.currentTimeMillis();
    }

    /**
     * Computes the milliseconds elapsed since the last tic() call.
     * @return the milliseconds elapsed since last tic() call.
     */
    @Override
    public String toc()
    {
        return Long.toString(System.currentTimeMillis() - actTime);
    }

    @Override
    public void addLogCatHandler(final LogCatHandler logCatHandler)
    {
        logCatHandlers.add(logCatHandler);
    }

    @Override
    public ArrayList<LogCatHandler> getLogCatHandlers()
    {
        return logCatHandlers;
    }
}
