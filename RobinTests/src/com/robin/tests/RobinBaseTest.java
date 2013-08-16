package com.robin.tests;

import com.robin.config.Config;
import com.robin.testcase.BaseTest;

public class RobinBaseTest extends BaseTest
{
    public RobinBaseTest()
    {
        super();
        Config.init(config());
    }
}
