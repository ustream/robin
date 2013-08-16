package com.robin.tests.smoke;

import org.testng.annotations.Test;

import com.robin.activities.MainActivity;
import com.robin.tests.RobinBaseTest;

import static com.robin.activities.ActivityHelper.*;

@Test(description = "Simple calculator multiply tests.",
    groups = { "SmokeTests" })
public class MainTest extends RobinBaseTest
{
    @Test(description = "Tests multiply function.",
        groups = { "MultiplySmokeTests" })
    public void testMultiply()
    {
        MainActivity main = getMainActivity();
        main.checkResult("0.00");
        final int repeat = 2;
        for (int i = 0; i < repeat; i++)
        {
            main
                .enterFirstNumber("2")
                .enterSecondNumber("3")
                .pressMultiply()
                .checkResult("6.0")
                .enterFirstNumber("4")
                .pressMultiply()
                .checkResult("12.0")
                .enterSecondNumber("0.25")
                .pressMultiply()
                .checkResult("1.0");
        }
    }

    @Test(description = "Tests multiply function.",
        groups = { "MultiplySmokeTests2" })
    public void testMultiplyLandscape()
    {
        MainActivity main = getMainActivity();
        act().rotateToLandscape();
        main.checkResult("0.00");
        final int repeat = 2;
        for (int i = 0; i < repeat; i++)
        {
            main
                .enterFirstNumber("2")
                .enterSecondNumber("3")
                .pressMultiply()
                .checkResult("6.0")
                .enterFirstNumber("4")
                .pressMultiply()
                .checkResult("12.0")
                .enterSecondNumber("0.25")
                .pressMultiply()
                .checkResult("1.0");
        }
    }
}
