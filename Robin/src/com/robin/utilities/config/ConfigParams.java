/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.utilities.config;

public final class ConfigParams
{

    public static final String CONFIGDIR = "config.dir";

    /**
     * Translation xml folder.
     */
    public static final String TLXMLSDIR = "tlxmls.dir";

    public static final String MIN_DEVICE_TO_USE = "mindevice";

    public static final String DEVICE_SELECTOR = "robin.device";

    /**
     * Log file for default Sys.out.
     */
    public static final String LOGFILE = "robin.logfile";

    /**
     * Debug parameter for robotium protocol debug console output.
     */
    public static final String ROBOTIUM_LOGGING = "robin.debug";

    /**
     * Application under test file location.
     */
    public static final String AUT_FILE = "robin.autFile";

    public static final String TEST_RUNNER_SOURCE_DIR =
        "robin.testRunnerSource";

    public static final String MESSENGER_SOURCE = "robin.messengerSource";

    public static final String RESIGNED_AUT_PATH = "robin.resignedAutPath";

    public static final String SCREENSHOT_PATH = "robin.screenshotPath";

    private ConfigParams()
    {

    }
}
