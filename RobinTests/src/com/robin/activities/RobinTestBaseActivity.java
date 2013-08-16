package com.robin.activities;

import com.robin.activity.BaseActivity;

/**
 * Base Page for all pages.
 * @author Ferenc Lombai
 */
public class RobinTestBaseActivity extends BaseActivity
{
    /**
     * Basic project specific selenium related functionalities for pages.
     */
    private TestBaseActivityFunctions core;

    /**
     * Constructor for this page.
     * @param indexOfSolo optional parameter, the index of the solo
     *            object to get from TestCaseSetup
     */
    public RobinTestBaseActivity(final int... indexOfSolo)
    {
        super("CommonElements.xml", indexOfSolo);
    }

    /**
     * @see RobinBaseActivityFunctions
     */
    public class TestBaseActivityFunctions extends RobinBaseActivityFunctions
    {

    }

    @Override
    public TestBaseActivityFunctions core()
    {
        if (core == null)
        {
            core = new TestBaseActivityFunctions();
        }
        return core;
    }

}
