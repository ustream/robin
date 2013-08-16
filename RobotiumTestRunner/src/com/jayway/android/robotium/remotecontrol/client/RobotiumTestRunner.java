/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.client;

import org.safs.android.messenger.client.MessageResult;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.jayway.android.robotium.remotecontrol.client.processor.SoloProcessor;
import com.jayway.android.robotium.remotecontrol.solo.Message;
import com.jayway.android.robotium.solo.RCSolo;

/**
 * Primary InstrumentationTestRunner used for remote controlled Android Automation in associated 
 * with a TCP Messenger Service.
 * <p>
 * This is the InstrumentationTestRunner that is considered the test package usually associated with 
 * a very specific target Package to be tested.  However, in the case of SAFS we want to make a completely 
 * reusable test package that is NOT built with a specific target Package association.  We want this 
 * general-purpose test framework to be usable to test all Android Applications via the data-driven 
 * <a href="http://safsdev.sourceforge.net" target="_blank">SAFS framework</a>.
 * <p>
 * The initial implementation, however, is not necessarily SAFS-specific.
 * <p>
 * How the remote control mechanism works:<br>
 * The test package AdroidManifest.xml does need to have the Instrumentation tags set for the target 
 * Application to be tested.  There is no getting around this:
 * <p><pre>
 * &lt;instrumentation android:name="com.jayway.android.robotium.remotecontrol.client.RobotiumTestRunner"
 *                  android:targetPackage="com.android.example.spinner"
 *                  android:label="General-Purpose SAFS Droid Automation Framework"/>
 * </pre>
 * The AndroidManifest.xml then simply needs to be repackaged into a working test APK in order to test 
 * the target application.
 * <p>
 * The test consists of the following on-device assets:<br>
 * 1. TCP Messenger Service,<br>
 * 2. Robotium Remote Solo APK,<br>
 * 3. Target Application APK.<br>
 * <p>
 * The remote control assets are simply:<br>
 * 1. Remote Controller implementing a TCP SocketServerListener,<br>
 * 2. TCP SocketServer binding to the on-device TCP Messenger Service for two-way communication.<br>
 * <p>
 * There is a predefined TCP Protocol the remote TCP SocketServer and the on-device TCP Messenger Service 
 * must adhere to for proper signalling and synchronization. 
 * <p>
 * When using the Droid Emulator, the remote controller must ensure the proper emulator port forwarding is 
 * set in order for the TCP Messenger Service to be able to communicate with the outside world.
 * 
 * @see org.safs.android.messenger.MessengerService
 * @see com.jayway.android.robotium.remotecontrol.client.AbstractTestRunner
 * @author Carl Nagle, SAS Institute, Inc.
 * <br>May 21, 2013		(SBJLWA)	Use RCSolo instead of Solo.
 */
public class RobotiumTestRunner extends AbstractTestRunner {

	public static String TAG = "RobotiumTestRunner";
	
	PackageManager myPackageManager = null;
	
	/** 
	 * Flags OR'd together to retrieve all available information on an installed Package.
	 */
	int ALL_PACKAGE_INFO = PackageManager.GET_ACTIVITIES  | PackageManager.GET_CONFIGURATIONS  | 
	                       PackageManager.GET_GIDS        | PackageManager.GET_INSTRUMENTATION | 
	                       PackageManager.GET_PERMISSIONS | PackageManager.GET_PROVIDERS       |
	                       PackageManager.GET_RECEIVERS   | PackageManager.GET_SERVICES        |
	                       PackageManager.GET_SIGNATURES ;

	/** 
	 * Flags OR'd together to retrieve all available information on an Application.
	 */
	int ALL_APPLICATION_INFO = PackageManager.GET_META_DATA | PackageManager.GET_SHARED_LIBRARY_FILES |
							   PackageManager.GET_UNINSTALLED_PACKAGES;
	
	PackageInfo myPackageInfo = null;
	InstrumentationInfo myInstrumentInfo = null;
	String targetPackageString = null;
	Intent targetLaunchIntent = null;
	String targetApplicationClassName = null;
	PackageInfo targetPackageInfo = null;
	ActivityInfo[] targetActivityInfo = null;
	
	RobotiumTestCase activityrunner = null;
	Activity mainApp = null;
	RCSolo solo = null;
	
	public RobotiumTestRunner(){
		super();
		addProcessor(SoloMessage.target_solo, initializeSoloProcessor());
	}
	
	/**
	 * Subclasses will want to override to instantiate their own SoloProcessor subclass.<br>
	 * This Processor will be add to processors-cache with key {@link SoloMessage#target_solo}
	 * 
	 * @see 	SoloMessage#target_solo
	 * @param 	runner
	 * @return	
	 */
	public SoloProcessor initializeSoloProcessor(){
		return new SoloProcessor(this);
	}
	
    public RobotiumTestCase getActivityrunner() {
		return activityrunner;
	}

    /**
     * The returned {@link RCSolo} is probably a null.<br>
     * The {@link RCSolo} object will be initialized after calling {@link #launchApplication()}<br>
     * <p>
     * We CANNOT automatically call launchApplication as part of this method because there 
     * are cases--especially during initialization--when the call to launchApplication will 
     * most definitely fail.
     * 
     * @return {@link com.jayway.android.robotium.solo.RCSolo} or null if the application 
     * has not yet been launched by the test/user at an appropriate time.
     */
	public RCSolo getSolo() {
		return solo;
	}
	
	@Override
	public String getListenerName() {
		return TAG;
	}
	
    /**
     * Instantiate the RobotiumTestCase.  Subclasses will likely override.
     * Called as part of the beforeStart initialization AFTER getTargetPackageInfo has initialized 
     * some critical fields.
     * 
	 * @see RobotiumTestCase
	 * @see #beforeStart()
	 * @see #getTargetPackageInfo()
     */
	boolean createRobotiumTestCase(){
		debug("createTestCase commencing...");

		try {
			//Using the extracted activity class name targetApplicationClassName to instantiate a TestCase
			activityrunner = new RobotiumTestCase(targetPackageString, Class.forName(targetApplicationClassName));
		} catch (ClassNotFoundException e) {
			debug("ClassNotFoundException: "+targetApplicationClassName+" can't be found.");
			return false;
		}

		debug("createTestCase COMPLETE.");
		return true;	
	}

    /**
     * Attempt to initialize the RobotiumTestCase with Intent and Instrumentation.
     * Normally, subclasses would not need to override this method.
     * 
	 * @see RobotiumTestCase#setActivityIntent(Intent)
	 * @see RobotiumTestCase#injectInstrumentation(android.app.Instrumentation)
     */
	boolean initializeInstrumentation(){
		debug("initializeInstrumentation commencing...");

		activityrunner.setActivityIntent(targetLaunchIntent);
		//Inject the Instrumentation to TestCase
		activityrunner.injectInstrumentation(this);
		
		debug("initializeInstrumentation COMPLETE.");
		return true;	
	}
	
    /**
     * Attempt to extract all pertinent information for launching/driving the package we are testing.
     * 
     * @see PackageManager#getPackageInfo(String, int)
     * @see PackageInfo#instrumentation
     * @see InstrumentationInfo#targetPackage
     * @see PackageManager#getLaunchIntentForPackage(String)
     */
    boolean getTargetPackageInfo(){
    	myPackageManager = getContext().getPackageManager();
    	try{
    		myPackageInfo = myPackageManager.getPackageInfo(getContext().getPackageName(), 
    													    PackageManager.GET_INSTRUMENTATION );
    		myInstrumentInfo = myPackageInfo.instrumentation[0];
    		targetPackageString = myInstrumentInfo.targetPackage;    		
    		debug("Found target Package: "+ targetPackageString);
    		    		
    		targetPackageInfo = myPackageManager.getPackageInfo(targetPackageString, ALL_PACKAGE_INFO);
    		
    		targetLaunchIntent = myPackageManager.getLaunchIntentForPackage(targetPackageString);
    		targetLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		
    		try{ 
    			debug("Found Launch Intent Action: "+ targetLaunchIntent.getAction() );
    			debug("Is ACTION_MAIN  : "+ targetLaunchIntent.getAction().equals(Intent.ACTION_MAIN));
        		targetApplicationClassName = targetLaunchIntent.getComponent().getClassName();    		
        		debug("Found Launch Component: "+ targetApplicationClassName);
    		}catch(NullPointerException np){
    			debug("Target Launch Intent is NULL!");
    		}
    		targetActivityInfo = targetPackageInfo.activities;
    		try{
    			debug("Found "+ targetActivityInfo.length +" target Activities.");
    			for(int i=0;i<targetActivityInfo.length;i++){
    				debug("   Activity Name: "+ targetActivityInfo[i].name);
    			}
    		}catch(NullPointerException np){
    			debug("Found 0 target Activities.");
    		}
    		return true;
    	}catch(NameNotFoundException nf){
    		debug("getTargetPackage "+nf.getClass().getSimpleName()+", "+ nf.getMessage());
    		return false;
    	}    	
    }
    
    @Override
    public boolean beforeStart(){
    	boolean processok = true;
    	
    	processok &= getTargetPackageInfo();
    	processok &= createRobotiumTestCase();
    	processok &= initializeInstrumentation();
    	
    	return processok;
    }

	@Override
	public void afterStart() {

	}
	
	/**
	 * Launches the main Application Activity and initializes the Robotium Solo object.
	 * This method MUST be invoked firstly before we can use Robotium Solo object to do further work.
	 * 
	 * @see RobotiumTestCase#setUp()
	 * @see RobotiumTestCase#getRobotium()
	 */
	public void launchApplication(){
		debug("launchApplication commencing...");

		activityrunner.setUp();
		solo = activityrunner.getRobotium();
		
		debug("launchApplication COMPLETE.");
	}
	
	/**
	 * Force a shutdown of ANY running test Activities.
	 * This is done via our Robotium Solo instance.
	 */
	public void closeApplication(){
		if(solo==null){
			debug("Error: The solo has not been initialized!");
			return;
		}
		solo.finishOpenedActivities();
	}

	/**
	 * ===========================================================================================
	 * Following are the call-back methods inheritated from CommandListener
	 * ===========================================================================================
	 */
	public MessageResult handleDispatchFile(String filename) {
		// TODO:
		return null;
	}

	public MessageResult handleMessage(String message) {
		// DEBUG Proof-of-concept:
		// this will not normally have a command implementation here
		// "launch" the launcher activity
		if(message.equalsIgnoreCase("launch")){ 
			debug("Handler processing LaunchApplication of "+ targetApplicationClassName);
			try{
				launchApplication();
				debug("LaunchApplication of "+ targetApplicationClassName +" sending Results.");
				messageRunner.sendServiceResult(Message.STATUS_REMOTERESULT_OK, "We should have launched "+targetApplicationClassName);						
			}catch(Throwable x){
				debug("LaunchApplication "+ x.getClass().getSimpleName()+" "+x.getMessage());
				x.printStackTrace();
				messageRunner.sendServiceResult(Message.STATUS_REMOTERESULT_UNKNOWN, "We failed to LAUNCH "+targetApplicationClassName);						
			}
		}
		// DEBUG Proof-of-concept:
		// this will not normally have a command implementation here
		// "close" the application
		else if(message.equalsIgnoreCase("close")){
			debug("Handler processing CloseApplication of "+ targetApplicationClassName);
			try{
				closeApplication();
				debug("CloseApplication of "+ targetApplicationClassName +" sending Results.");
				messageRunner.sendServiceResult(Message.STATUS_REMOTERESULT_OK, "We should have closed "+targetApplicationClassName);						
			}catch(Throwable x){
				debug("CloseApplication "+ x.getClass().getSimpleName()+" "+x.getMessage());
				x.printStackTrace();
				messageRunner.sendServiceResult(Message.STATUS_REMOTERESULT_UNKNOWN, "We failed to CLOSE "+targetApplicationClassName);						
			}
		}
		// any other unrecognized message is passed thru
		else{
			debug("Handler Received Custom message: "+ message);
			// TODO: where to?
			messageRunner.sendServiceResult(Message.STATUS_REMOTERESULT_OK, "Received custom message "+message);						
		}
		return null;
	}
    
	public MessageResult handleServerConnected() {
		// TODO Auto-generated method stub
		return null;
	}

	public MessageResult handleServerDisconnected() {
		// TODO
		return null;
	}

	public MessageResult handleServerShutdown() {
		// TODO Auto-generated method stub
		return null;
	}

	public MessageResult handleRemoteShutdown() {
		// TODO Auto-generated method stub
		return null;
	}

}