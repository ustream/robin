/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.client;

import com.jayway.android.robotium.solo.RCSolo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Robotium requires the use of an ActivityInstrumentationTestCase2 and not the 
 * InstrumentationTestRunner of the test package.
 * <p>
 * In a general-purpose framework like SAFS this may be all that this class ever does--
 * instantiate a working version of Robotium Solo.
 * @author Carl Nagle, SAS Institute, Inc.
 * @since JAN 28, 2012
 * <br>May 21, 2013		(SBJLWA)	Use RCSolo instead of Solo.
 */
public class RobotiumTestCase extends ActivityInstrumentationTestCase2{

	RCSolo solo = null;
	Activity activity = null;
	Intent intent = null;

	/**
	 * Constructor for the class merely calling the superclass constructor.
	 * Prepares the instance with the targetPackage and launch Activity Class.
	 * These items are deduced elsewhere thru the Android PackageManager.
	 * @param String targetPackage
	 * @param Class targetClass
	 * @see ActivityInstrumentationTestCase2#ActivityInstrumentationTestCase2(String, Class)
	 */
	public RobotiumTestCase(String targetPackage, Class targetClass){
		super(targetPackage, targetClass);
	}

	/**
	 * Wrapper to preferred output/debug logging.
	 * @param text
	 */
	void setStatus(String text){
		Log.d(AbstractTestRunner.TAG, text);
	}
	
	/**
	 *  Acquires the launch Activity with a call to getActivity() and then creates the 
	 *  Robotium Solo instance from that Activity.
	 *  @see #getActivity()
	 *  @see com.jayway.android.robotium.solo.RCSolo#RCSolo(Instrumentation, Activity)
	 */
	public void setUp(){
		if(activity == null){
			activity = getActivity();
			intent = new Intent(activity, activity.getClass());
		}else{
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
			activity.startActivity(intent);
		}
		solo = new RCSolo(getInstrumentation(), activity);
	}

	/**
	 * Retrieve the Solo instance after it has been created.
	 * @return Solo instance
	 */
	public RCSolo getRobotium() { return solo; }
	
	/**
	 * Currently, simply calls solo.finishOpenActivities()
	 * @see RCSolo#finishOpenedActivities()
	 */
	public void tearDown(){
		solo.finishOpenedActivities();
	}
}
