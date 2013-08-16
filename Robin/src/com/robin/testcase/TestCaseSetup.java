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

import com.android.ddmlib.IDevice;
import com.jayway.android.robotium.remotecontrol.solo.Solo;

import com.robin.reporter.TestCaseReporter;
import com.robin.reporter.logcat.LogCatHandler;
import com.robin.utilities.user.User;

/**
 * @author ChaotX
 *         This interface defines what information a single test is depend
 *         upon and what data might be collect during the test run.
 */
public interface TestCaseSetup
{

    // ----- TestNG setup required data related functions ---------

    String getDeviceSelectorRegexp();

    void setDeviceSelectorRegexp(final String deviceRegexp);

    File getAutApk();

    void setAutApk(final File deviceRegexp);

    // - After setup and during test run required data related functions -

    IDevice getDevice(final int... indexOfSolo);

    void addDevice(final IDevice device);

    String getTestClassName();

    void setTestClassName(final String className);

    String getMethodName();

    void setMethodName(final String methodNameParam);

    void addSolo(final Solo solo);

    Solo getSolo(final int... indexOfSolo);

    int getLastSoloIndex();

    TestCaseReporter getReporter();

    void addUser(User user);

    ArrayList<User> getUsers();

    void generateTimeStamp();

    String getTimeStamp(final Long threadId);

    void tic();

    String toc();

    void addLogCatHandler(final LogCatHandler logCatHandler);

    ArrayList<LogCatHandler> getLogCatHandlers();
}
