/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.solo;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * This is used to test Remote Solo Implementation.<br>
 * You see link <a href="http://safsdev.sourceforge.net/doc/com/jayway/android/robotium/remotecontrol/solo/Solo.html">Remote Solo</a> for methods to test.<br>
 * We use the sample application ApiDemos provided by Android<br>
 * <br>
 * Due to the resolution problem, to make the test run smoothly on different<br>
 * device, you need to modify the AndroidManifest.xml of ApiDemos<br>
 * application by adding the following line.<br>
 * <supports-screens android:largeScreens="true" android:normalScreens="true" android:smallScreens="true" android:anyDensity="true"/><br>
 * After the modification, you need to rebuild it.<br>
 * <br>
 * This test requires you set you device's "language setting" to "English"<br>
 *    
 * @author Lei Wang, SAS Institute, Inc.
 * <br>May 17, 2013		(SBJLWA)	Move some codes to SoloTest
 */
public class SoloTestAndroidApi extends SoloTest {
	
	public static final String DEFAULT_AUT_APK = "D:\\Eclipse\\workspace\\ApiDemos\\bin\\ApiDemos-debug.apk";
	
	public SoloTestAndroidApi() {
		super();
	}

	public SoloTestAndroidApi(String messengerApk, String testRunnerApk, String instrumentArg) {
		super(messengerApk, testRunnerApk, instrumentArg);
	}

	/**
	 * @param args	Array of String: {"messenger=XXX", "runner=XXX", "instrument=XXX"}
	 */
	public SoloTestAndroidApi(String[] args) {
		super(args);
	}

	/**
	 * <pre>
	 * Use solo to test Android ApiDemos
	 * 
	 * <instrumentation android:name="com.jayway.android.robotium.remotecontrol.client.RobotiumTestRunner"
	 *                  android:targetPackage="com.example.android.apis"
	 *                  android:label="General-Purpose Robotium Test Runner"/>
	 * 
	 * Test methods as:
	 * {@link Solo#finishOpenedActivities()}
	 * {@link Solo#getCurrentActivity()}
	 * {@link Solo#waitForActivity(String)}
	 * {@link Solo#assertMemoryNotLow()}
	 * {@link Solo#getAllOpenActivities()}
	 * {@link Solo#setActivityOrientation(int)}
	 * {@link Solo#getActivityMonitor()}
	 * 
	 * The other Robotium-RC methods will be tested in the following methods:
	 * They are independent from one to other, so you can comment any them during the regression test.
	 * </pre>                 
	 *                  
	 * @see #verifyAssertActivityMethods(String, String)
	 * @see #verifyWaitActivityMethods(String, boolean)
	 * @see #testCustomTitleActivity()
	 * @see #testDefaultThemeActivity()
	 * @see #testDialogActivity()
	 * @see #testDatePicker()
	 * @see #gotoViewsList()
	 * @see #gotoInflateMenu()
	 * @see #gotoPhotoGallery()
	 * @see #gotoIconGrid()
	 */
	protected void test(){
		//Begin the testing
		//Don't output the error messages from the remote side.
		solo.doProcessFailure = false;
		solo.doProcessSuccess = false;
		
		try {
			if(solo.assertMemoryNotLow()){
				debug("assertMemoryNotLow() Success: We have enough memory.");
			}else{
				debug("assertMemoryNotLow() Success: We don't have enough memory.");
				return;
			}
			
			//Set orientation to LANDSCAPE
			if(solo.setActivityOrientation(Solo.LANDSCAPE)){
				pass("setActivityOrientation(int): set orientation to LANDSCAPE");
			}else{
				pass("setActivityOrientation(int): fail to set orientation to LANDSCAPE");
			}
			
			//List all opened activities
			info("getAllOpenActivities(): Before testing, got All Opened Activities:");
			List<String> openedActivites = solo.getAllOpenActivities();
			for(String activity: openedActivites){
				info(activity);
			}
			
			//Get the current activity
			String activityID = solo.getCurrentActivity();
			Properties props = solo._last_remote_result;
			String activityName = props.getProperty(Message.PARAM_NAME);
			String activityClass = props.getProperty(Message.PARAM_CLASS);

			info("CurrentActivity   UID: "+ activityID);
			info("CurrentActivity Class: "+ activityClass);				
			info("CurrentActivity  Name: "+ activityName);
			
			verifyAssertActivityMethods(activityClass, activityName);
			
			verifyWaitActivityMethods(activityName,true);
			verifyWaitActivityMethods("BoguActivity",false);
			
			//Set orientation to PORTRAIT
			if(solo.setActivityOrientation(Solo.PORTRAIT)){
				pass("setActivityOrientation(int): set orientation to PORTRAIT");
			}else{
				pass("setActivityOrientation(int): fail to set orientation to PORTRAIT");
			}
			
			if(solo.waitForActivity(activityName)){
				pass("waitForActivity(String) Correct: '"+activityName+"' was found.");
				printCurrentViews(TYPE_LISTVIEW);
				gotoCustomTitleActivity();	
				gotoDefaultThemeActivity();
				gotoDialogActivity();
				gotoDateTimePicker();
				gotoViewsList();
				gotoInflateMenu();
				gotoPhotoGallery();
				gotoIconGrid();
			}else{
				String name = solo._last_remote_result.getProperty(Message.PARAM_NAME);
				fail("***waitForActivity(String) Error: '"+activityName+"' was NOT found. The current activity is '"+name+"'***");
			}
			
			//List all opened activities
			info("getAllOpenActivities(): After testing, got All Opened Activities:");
			openedActivites = solo.getAllOpenActivities();
			for(String activity: openedActivites){
				info(activity);
			}
			
			String activityMonitorID = solo.getActivityMonitor();
			if(activityMonitorID!=null){
				pass("getActivityMonitor(): got the activity monitor's ID is '"+activityMonitorID+"'");
			}else{
				pass("getActivityMonitor(): fail to get the activity monitor");
			}
			
			// SHUTDOWN all Activities.  Done Testing.
			if(solo.finishOpenedActivities()){
				info("Application finished/shutdown without error.");				
			}else{
				warn("Application finished/shutdown with error.");
			}

		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * <pre>
	 * {@link Solo#getString(String)}
	 * {@link Solo#getView(int)}
	 * 
	 * The test depends on the application itself.
	 * The parameter is got from the ApiDemos android project
	 * </pre>
	 * 
	 * @see #testEditBoxInCustomActivity()
	 */
	protected void testRelateToApplication(String editTextIdToVerify){
		try {
			//for different language setting, the string is got from different xml file, for example:
			//String is got from ApiDemosProject/res/values/strings.xml when "language setting" is "English"
			//String is got from ApiDemosProject/res/values-zh/strings.xml when "language setting" is "Chinese"
			//<string name="activity_animation">App/Activity/Animation</string>
			String activity_animation_value = "App/Activity/Animation";
		
			//This is the generated id after the compilation of the ApiDeoms project,
			//So it is often changed, you need to verify it before test.
			//you can find it at ApiDemosProject/gen/the.package.of.component/R.string.stringId
			final int activity_animation_id = 0x7f0a0015;
			String value = solo.getString(String.valueOf(activity_animation_id));

			if(value!=null){
				pass("getString(String): get value '"+value+"' for native string id '"+Integer.toHexString(activity_animation_id)+"'");
				if(value.equals(activity_animation_value)){
					debug("getString(String): get the expected value");
				}else{
					debug("getString(String): fail to get the expected value, the expected value is '+activity_animation_value+'");
				}
			}else{
				fail("getString(String): fail to get value for native string id '"+Integer.toHexString(activity_animation_id)+"'");
			}
			
			//This is the generated id after the compilation of the ApiDeoms project,
			//So it is often changed, you need to verify it before test.
			//you can find it at ApiDemosProject/gen/the.package.of.component/R.id.comId
			//com.example.android.apis.app.CustomTitle
			final int leftTextEdit_id=0x7f090036;//This is the first editText's id in CustomTitle activity
			String editCompId = solo.getView(leftTextEdit_id);
			if(editCompId!=null){
				pass("getView(int): get the view's id as '"+editCompId+"' for native component id '"+Integer.toHexString(leftTextEdit_id)+"'");
				if(editCompId.equals(editTextIdToVerify)){
					pass("Verification pass: get the same eidtText id");
				}else{
					fail("Verification fail: get the same eidtText id");
				}
			}else{
				fail("getView(int): fail to get the view's id for native component id '"+Integer.toHexString(leftTextEdit_id)+"'");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
	}
	
	private static final String TYPE_CURRENBUTTON	= "CB";
	private static final String TYPE_CURRENTVIEW	= "CV";
	private static final String TYPE_CHECKBOX		= "CHKB";
	private static final String TYPE_DATEPICKER 	= "DP";
	private static final String TYPE_EDITTEXT	 	= "ET";
	private static final String TYPE_GRIDVIEW 		= "GV";
	private static final String TYPE_IMAGEBUTTON 	= "IB";
	private static final String TYPE_IMAGEVIEW 		= "IV";
	private static final String TYPE_LISTVIEW 		= "LV";
	private static final String TYPE_PROGRESSBAR	= "PB";
	private static final String TYPE_RADIOBUTTON	= "RB";
	private static final String TYPE_SLIDERDRAWER 	= "SD";
	private static final String TYPE_SPINNER	 	= "SP";
	private static final String TYPE_SCROLLVIEW 	= "SV";
	private static final String TYPE_TOGGLEBUTTON	= "TB";
	private static final String TYPE_TIMEPICKER		= "TP";
	private static final String TYPE_TEXTVIEW 		= "TV";
	
	@SuppressWarnings({ "unused", "unchecked" })
	private void printCurrentViews(String viewType) throws Exception{
		List<String> views = null;
		String methodString = null;
		
		if(TYPE_CURRENBUTTON.equals(viewType)){
			views = solo.getCurrentButtons();
			methodString = "getCurrentButtons";
		}else if(TYPE_CURRENTVIEW.equals(viewType)){
			views = solo.getCurrentViews();
			methodString = "getCurrentViews";
		}else if(TYPE_CHECKBOX.equals(viewType)){
			views = solo.getCurrentCheckBoxes();
			methodString = "getCurrentCheckBoxes";
		}else if(TYPE_DATEPICKER.equals(viewType)){
			views = solo.getCurrentDatePickers();
			methodString = "getCurrentDatePickers";
		}else if(TYPE_EDITTEXT.equals(viewType)){
			views = solo.getCurrentEditTexts();
			methodString = "getCurrentEditTexts";
		}else if(TYPE_GRIDVIEW.equals(viewType)){
			views = solo.getCurrentGridViews();
			methodString = "getCurrentGridViews";
		}else if(TYPE_IMAGEBUTTON.equals(viewType)){
			views = solo.getCurrentImageButtons();
			methodString = "getCurrentImageButtons";
		}else if(TYPE_IMAGEVIEW.equals(viewType)){
			views = solo.getCurrentImageViews();
			methodString = "getCurrentImageViews";
		}else if(TYPE_LISTVIEW.equals(viewType)){
			views = solo.getCurrentListViews();
			methodString = "getCurrentListViews";
		}else if(TYPE_PROGRESSBAR.equals(viewType)){
			views = solo.getCurrentProgressBars();
			methodString = "getCurrentProgressBars";
		}else if(TYPE_RADIOBUTTON.equals(viewType)){
			views = solo.getCurrentRadioButtons();
			methodString = "getCurrentRadioButtons";
		}else if(TYPE_SCROLLVIEW.equals(viewType)){
			views = solo.getCurrentScrollViews();
			methodString = "getCurrentScrollViews";
		}else if(TYPE_SLIDERDRAWER.equals(viewType)){
			views = solo.getCurrentSlidingDrawers();
			methodString = "getCurrentSlidingDrawers";
		}else if(TYPE_SPINNER.equals(viewType)){
			views = solo.getCurrentSpinners();
			methodString = "getCurrentSpinners";
		}else if(TYPE_TOGGLEBUTTON.equals(viewType)){
			views = solo.getCurrentToggleButtons();
			methodString = "getCurrentToggleButtons";
		}else if(TYPE_TIMEPICKER.equals(viewType)){
			views = solo.getCurrentTimePickers();
			methodString = "getCurrentTimePickers";
		}else if(TYPE_TEXTVIEW.equals(viewType)){
			views = solo.getCurrentTextViews();
			methodString = "getCurrentTextViews";
		}
		
		if(views!=null && views.size()>0){
			pass(methodString+"() Correct: get current view as:");
			for(String id: views){
				debug("id="+id);
			}
		}else{
			fail(methodString+"() Error: fail to get current view.");
		}
	}
	
	/**
	 * <pre>
	 * Verify following methods:
	 * {@link Solo#waitForActivity(String)}
	 * {@link Solo#waitForActivity(String, int)}
	 * </pre>
	 * 
	 * @param activityName	String,  The current activity's name
	 * @param positiveTest	boolean, true for positive test, the activityName should be valid.
	 *                               false for negative test, the activityName should NOT be valid.
	 * @throws Exception
	 * 
	 * @return true, if the expected activity exists
	 */
	void verifyWaitActivityMethods(String activityName, boolean positiveTest) throws Exception{
		
		if(positiveTest){
			debug("Positive Test waitForActivity(): ");

			if(solo.waitForActivity(activityName)){
				pass("waitForActivity(String) Correct: '"+activityName+"' was found.");
			}else{
				String name = solo._last_remote_result.getProperty(Message.PARAM_NAME);
				fail("***waitForActivity(String) Error: '"+activityName+"' was NOT found. The current activity is '"+name+"'***");
			}
			
			if(solo.waitForActivity(activityName, 1000)){
				pass("waitForActivity(String, int) Correct: '"+activityName+"' was found.");
			}else{
				String name = solo._last_remote_result.getProperty(Message.PARAM_NAME);
				fail("***waitForActivity(String, int) Error: '"+activityName+"' was NOT found. The current activity is '"+name+"'***");
			}
		}else{
			debug("Negative Test waitForActivity(): ");
			
			if(solo.waitForActivity(activityName)){
				fail("waitForActivity(String) Error: '"+activityName+"' was found, but it should NOT.");
			}else{
				pass("waitForActivity(String) Correct: '"+activityName+"' was not found as expected. ***");
			}
			
			if(solo.waitForActivity(activityName,1000)){
				fail("waitForActivity(String, int) Error: '"+activityName+"' was found, but it should NOT.");
			}else{
				pass("waitForActivity(String, int) Correct: '"+activityName+"' was not found as expected. ***");
			}
		}
		
	}
	
	/**
	 * <pre>
	 * Verify following methods:
	 * {@link Solo#assertCurrentActivityClass(String, String)}
	 * {@link Solo#assertCurrentActivityClass(String, String, boolean)}
	 * {@link Solo#assertCurrentActivityName(String, String)}
	 * {@link Solo#assertCurrentActivityName(String, String, boolean)}
	 * </pre>
	 * 
	 * @param activityClass		The current activity's class name
	 * @param activityName		The current activity's name
	 * @throws Exception
	 */
	void verifyAssertActivityMethods(String activityClass, String activityName) throws Exception{

		
		//Positive Verification of assertCurrentActivityClass(String, String)
		if(solo.assertCurrentActivityClass("assertCurrentActivityClass Error", activityClass)){
			pass("assertCurrentActivityClass(String, String) Correct: activity's class is '"+activityClass+"'");
			
			//Positive Verification of assertCurrentActivityClass(String, String, boolean)
			if(solo.assertCurrentActivityClass("assertCurrentActivityClass Error", activityClass, true)){
				pass("assertCurrentActivityClass(String, String, boolean) Correct: activity's class is new instance of '"+activityClass+"'");				
			}else{
				fail("assertCurrentActivityClass(String, String, boolean) Error: activity's class should be new instance of '"+activityClass+"'");
			}
			//Negative verification  of assertCurrentActivityClass(String, String, boolean)
			if(solo.assertCurrentActivityClass("assertCurrentActivityClass Correct", activityClass, false)){
				fail("assertCurrentActivityClass(String, String, boolean) Error: activity's class should NOT be old instance of '"+activityClass+"'");				
			}else{
				pass("assertCurrentActivityClass(String, String, boolean) Correct: activity's class is not old instance of '"+activityClass+"'");				
			}
			
		}else{
			String clazz = solo._last_remote_result.getProperty(Message.PARAM_CLASS);
			fail("assertCurrentActivityClass(String, String) Error: activity's class should be '"+activityClass+"'. But the real class is '"+clazz+"'");
		}
		//Negative verification  of assertCurrentActivityClass(String, String)
		if(solo.assertCurrentActivityClass("assertCurrentActivityClass Correct", "some.bogu.class")){
			fail("assertCurrentActivityClass(String, String) Error: activity's class should NOT be 'some.bogu.class'");				
		}else{
			pass("assertCurrentActivityClass(String, String) Correct: activity's class is not 'some.bogu.class'");				
		}
		
		//Positive Verification of assertCurrentActivityName(String, String)
		if(solo.assertCurrentActivityName("assertCurrentActivityName Error", activityName)){
			pass("assertCurrentActivityName(String, String) Correct: activity's name is '"+activityName+"'");	
			
			//Positive Verification of assertCurrentActivityName(String, String, boolean)
			if(solo.assertCurrentActivityName("assertCurrentActivityName Error", activityName, true)){
				pass("assertCurrentActivityName(String, String, boolean) Correct: activity's name is new instance of '"+activityName+"'");				
			}else{
				fail("assertCurrentActivityName(String, String, boolean) Error: activity's name should be new instance of '"+activityName+"'");
			}
			//Negative verification  of assertCurrentActivityName(String, String, boolean)
			if(solo.assertCurrentActivityName("assertCurrentActivityName Correct", activityName, false)){
				fail("assertCurrentActivityName(String, String, boolean) Error: activity's name should NOT be old instance of '"+activityName+"'");				
			}else{
				pass("assertCurrentActivityName(String, String, boolean) Correct: activity's name is not old instance of '"+activityName+"'");				
			}
			
		}else{
			String name = solo._last_remote_result.getProperty(Message.PARAM_NAME);
			fail("assertCurrentActivityName(String, String) Error: activity's name should be '"+activityName+"'. But the real name is '"+name+"'");
		}
		//Negative verification  of assertCurrentActivityName(String, String)
		if(solo.assertCurrentActivityName("assertCurrentActivityName Correct", "BoguActivityName")){
			fail("assertCurrentActivityName(String, String) Error: activity's name should NOT be 'BoguActivityName'");				
		}else{
			pass("assertCurrentActivityName(String, String) Correct: activity's name is not 'BoguActivityName'");				
		}

	}
	
	/**
	 * <pre>
	 * "App -> Activity -> Custom Title" is the path to 'Custom Title Activity'.
	 * This method will open the 'Custom Title Activity', then it will call {@link #testEditBox()} to
	 * test some methods related to EditBox, finally it will go back to the first page of ApiDemos.
	 * During this process, it will test the following methods
	 * 
	 * Tested methods:
	 * {@link Solo#waitForView(String)}
	 * {@link Solo#waitForView(String, int, long)}
	 * 
	 * {@link Solo#getView(String, int)}
	 * 
	 * {@link Solo#clickOnText(String)}
	 * {@link Solo#clickOnText(String, int)}
	 * {@link Solo#clickOnText(String, int, boolean)}
	 * 
	 * {@link Solo#clickInList(int)}
	 * 
	 * {@link Solo#goBackToActivity(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	void gotoCustomTitleActivity() throws Exception{
		String firstLevelListUID = null;
		String secondLevelListUID = null;
		String thirdLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				String text = "App";
				//review the source code of Robotium: clickOnText() will only scroll down to search text, never up
				//If the text is above, the method clickOnText will certainly fail.
				if(solo.clickOnText(wrapRegex(text), 1, true)){
					pass("clickOnText(String, int, boolean) Correct: Text '"+text+"' was clicked.");
					
					if(solo.waitForView("android.widget.ListView", 1, 1000)){
						pass("waitForView(String, int, long) Correct: 'ListView' appears within timeout");
						
						secondLevelListUID = solo.getView("android.widget.ListView", 0);
						debug("Second level ListView UID= "+secondLevelListUID);
						if(secondLevelListUID!=null){
							pass("getView(String, int) Correct: 'ListView' was got.");
							
							text = "Activity";
							if(solo.clickOnText(wrapRegex(text), 1)){
								pass("clickOnText(String, int) Correct: Text '"+text+"' was clicked.");
								
								if(solo.waitForView("android.widget.ListView")){
									pass("waitForView(String) Correct: 'ListView' appears within timeout");
									
									thirdLevelListUID = solo.getView("android.widget.ListView", 0);
									debug("Third level ListView UID= "+thirdLevelListUID);
									if(thirdLevelListUID!=null){
										pass("getView(String, int) Correct: 'ListView' was got.");
										
										text = "Custom Title";
//										if(solo.clickOnText(wrapRegex(text))){
//											pass("clickOnText(String, int) Correct: Click on '"+text+"' successfully.");
//											testEditBoxInCustomActivity();
//											
//										}else{
//											fail("clickOnText(String, int) Error: There is no Text '"+text+"'!");
//										}
										
										//Use clickInList(int), the third line should be 'Custom Title'
										int customTitleLine = 3;
										//textViews contains the UID of the TextViews in the line of the List
										List textViews = solo.clickInList(customTitleLine);
										if(textViews!=null){
											pass("clickInList(int) Correct: Click on line='"+customTitleLine+"' successfully.");
											
											//We will get the 'out parameter' Message.PARAM_TEXT, where the text we clicked should be stored
											String clickedTexts = solo._last_remote_result.getProperty(Message.PARAM_TEXT);
											if(clickedTexts!=null && clickedTexts.indexOf(text)>-1){
												pass("Click on '"+text+"' of ListView successfully.");
												testEditBoxInCustomActivity();
											}else{
												super.fail("Fail to Click on '"+text+"' of ListView. But we clicked '"+clickedTexts+"' in ListView.");
											}
											
											debug("Clicked the text '"+clickedTexts+"'");
											
										}else{
											fail("clickInList(int) Error: Fail to Click on line='"+customTitleLine+"'!");
										}
										
										goBackToViewUID(thirdLevelListUID);
										
									}else{
										fail("getView(String, int) Error: 'ListView' was NOT got.");
									}
									
								}else{
									fail("waitForView(String) Error: ListView NOT appear!");
								}
								
							}else{
								fail("clickOnText(String, int) Error: Fail to click on Text '"+text+"'!");
							}
							
							goBackToViewUID(secondLevelListUID);
						}else{
							fail("getView(String, int) Error: 'ListView' was NOT got.");
						}
						
					}else{
						fail("waitForView(String, int, long) Error: 'ListView' NOT appear within timeout.");
					}
					
				}else{
					fail("clickOnText(String, int, boolean) Error: Fail to click on Text '"+text+"'!");	
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
		
	}
	
	/**
	 * <pre>
	 * "App -> Dialog " is the path to 'Dialog Activity'.
	 * This method will open the 'Dialog Activity' and make some test
	 * finally it will go back to the first page of ApiDemos.
	 * During this process, it will test the following methods
	 * 
	 * Tested methods:
	 * {@link Solo#clickInList(int)}
	 * {@link Solo#clickInList(int, int)}
	 * {@link Solo#clickLongInList(int)}
	 * {@link Solo#clickLongInList(int, int)}
	 * {@link Solo#clickLongInList(int, int, int)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 * @see #testProgressBar()
	 * @see #testButtons()
	 */
	void gotoDialogActivity()throws Exception{
		String firstLevelListUID = null;
		String secondLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				int line = 1;//The line in list to click, start from 1
				int index = 0;//The list to click if there are multiple lists, start from 0
				
				//TODO scroll up to the top of the list
				
				List clickedItems = solo.clickInList(line);
				String clickedItemText = solo._last_remote_result.getProperty(Message.PARAM_TEXT);
				debug("Item '"+clickedItemText+"' was clicked in the List");
				
				if(clickedItems!=null && clickedItems.size()>0){
					
					pass("clickInList(int) Correct: The "+line+"th item was clicked.");
					
					if(solo.waitForView("android.widget.ListView", 1, 1000)){
						pass("waitForView(String, int, long) Correct: 'ListView' appears within timeout");
						
						secondLevelListUID = solo.getView("android.widget.ListView", 0);
						debug("Second level ListView UID= "+secondLevelListUID);
						if(secondLevelListUID!=null){
							pass("getView(String, int) Correct: 'ListView' was got.");
							
							line = 4;
							clickedItems = solo.clickInList(line, index);
							clickedItemText = solo._last_remote_result.getProperty(Message.PARAM_TEXT);
							debug("Item '"+clickedItemText+"' was clicked in the List");
							
							if(clickedItems!=null && clickedItems.size()>0){
								pass("clickInList(int, int) Correct: The "+line+"th item was clicked.");
								
								testButtons();

								testProgressBar();
								
							}else{
								fail("clickInList(int, int) Error: Fail to click the "+line+"th item.");
							}
							
							//GO BACK and Try the clickLongInList(int)
							goBackToViewUID(secondLevelListUID);
							
							clickedItems = solo.clickLongInList(line);
							String theSameDialogText = solo._last_remote_result.getProperty(Message.PARAM_TEXT);
							debug("Item '"+theSameDialogText+"' was clicked in the List");
							
							if(clickedItems!=null && clickedItems.size()>0){
								pass("clickLongInList(int) Correct: The "+line+"th item was clicked longtime.");
								if(theSameDialogText!=null && theSameDialogText.equals(clickedItemText)){
									pass("clickInList("+line+") and clickLongInList("+line+"): click on same text '"+theSameDialogText+"'" );
								}else{
									fail("clickInList("+line+") click on text '"+clickedItemText+"'" +"; While clickLongInList("+line+") click on text '"+theSameDialogText+"'" );
								}
							}else{
								fail("clickLongInList(int) Error: Fail to click the "+line+"th item longtime.");
							}
							
							//GO BACK and Try the clickLongInList(int, int)
							goBackToViewUID(secondLevelListUID);
							
							clickedItems = solo.clickLongInList(line, index);
							theSameDialogText = solo._last_remote_result.getProperty(Message.PARAM_TEXT);
							debug("Item '"+theSameDialogText+"' was clicked in the List");
							
							if(clickedItems!=null && clickedItems.size()>0){
								pass("clickLongInList(int, int) Correct: The "+line+"th item was clicked longtime on the "+index+"th list.");
								if(theSameDialogText!=null && theSameDialogText.equals(clickedItemText)){
									pass("clickInList("+line+") and clickLongInList("+line+","+index+"): click on same text '"+theSameDialogText+"'" );
								}else{
									fail("clickInList("+line+") click on text '"+clickedItemText+"'" +"; While clickLongInList("+line+","+index+") click on text '"+theSameDialogText+"'" );
								}
							}else{
								fail("clickLongInList(int, int) Error: Fail to click the "+line+"th item longtime on the "+index+"th list.");
							}
							
							//GO BACK and Try the clickLongInList(int, int, int)
							goBackToViewUID(secondLevelListUID);
							int time = 500;
							clickedItems = solo.clickLongInList(line, index, time);
							theSameDialogText = solo._last_remote_result.getProperty(Message.PARAM_TEXT);
							debug("Item '"+theSameDialogText+"' was clicked in the List");
							
							if(clickedItems!=null && clickedItems.size()>0){
								pass("clickLongInList(int, int, int) Correct: The "+line+"th item was clicked longtime ("+time+" millis) on the "+index+"th list.");
								if(theSameDialogText!=null && theSameDialogText.equals(clickedItemText)){
									pass("clickInList("+line+") and clickLongInList("+line+","+index+","+time+"): click on same text '"+theSameDialogText+"'" );
								}else{
									fail("clickInList("+line+") click on text '"+clickedItemText+"'" +"; While clickLongInList("+line+","+index+","+time+") click on text '"+theSameDialogText+"'" );
								}
							}else{
								fail("clickLongInList(int, int) Error: Fail to click the "+line+"th item longtime ("+time+" millis) on the "+index+"th list.");
							}
							
							goBackToViewUID(secondLevelListUID);
							
						}else{
							fail("getView(String, int) Error: 'ListView' was NOT got.");
						}
						
					}else{
						fail("waitForView(String, int, long) Correct: 'ListView' NOT appear within timeout.");
					}
					
				}else{
					fail("clickInList(int) Error: Fail to click the "+line+"th item.");
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
		
	}
	
	/**
	 * <pre>
	 * Test some methods as:
	 * {@link Solo#searchButton(String)}
	 * 
	 * {@link Solo#clickOnButton(String)}
	 * 
	 * Test some method of ProgressBar
	 * {@link Solo#getCurrentProgressBars()}
	 * {@link Solo#setProgressBar(int, int)}
	 * {@link Solo#setProgressBar(String, int)}
	 * 
	 * {@link Solo#waitForDialogToClose(int)}
	 * 
	 * </pre>
	 * 
	 * @see #testDialogActivity()
	 */
	private void testProgressBar() throws Exception{

		String text = "Progress dialog";
		
		try {
			if(solo.searchButton(text)){
				pass("searchButton(String) Correct: search button with text '"+text+"'");
				
				//Click button to open dialog
				if(solo.clickOnButton(text)){
					pass("searchButton(String) Correct: click button with text '"+text+"'");
					
					List progressBarList = solo.getCurrentProgressBars();
					
					if(progressBarList!=null && progressBarList.size()>0){
						pass("getCurrentProgressBars() Correct: got progress bar as: ");
						for(int i=0;i<progressBarList.size();i++){
							debug("Progress Bar ID="+progressBarList.get(i));
						}
						
						//Set progress bar to 60%
						int progress = 60;//should not bigger than ProgressBar.getMax()!!!
						String progressBarID = (String) progressBarList.get(0);
						if(solo.setProgressBar(progressBarID, progress)){
							pass("setProgressBar(String, int) Correct: set progress to "+progress+"% for progress bar '"+progressBarID+"'");
						}else{
							fail("setProgressBar(String, int) Error: fail to set progress to "+progress+"% for progress bar '"+progressBarID+"'");
						}
						
						//Set progress bar to 0%
						progress = 0;
						int index = 0;
						if(solo.setProgressBar(index, progress)){
							pass("setProgressBar(int, int) Correct: set progress to "+progress+"% for '"+index+"'th progress bar.");
						}else{
							fail("setProgressBar(int, int) Error: fail to set progress to "+progress+"% for '"+index+"'th progress bar.");
						}
						
						//Wait for progress bar dialog to close
						int timeout = 3000;//time to wait for dialog to close, milliseconds
						//we know 3 seconds is not enough for progress dialog to close.
						if(solo.waitForDialogToClose(timeout)){
							info("waitForDialogToClose(int) : dialog has been closed in '"+timeout+"' milliseconds.");
						}else{
							info("waitForDialogToClose(int) : dialog was not closed in '"+timeout+"' milliseconds.");
							timeout = 30000;//wait for 30 seconds, this is enough for dialog ot close
							if(solo.waitForDialogToClose(timeout)){
								pass("waitForDialogToClose(int) Correct: dialog has been closed in '"+timeout+"' milliseconds.");
							}else{
								fail("waitForDialogToClose(int) Error: dialog was not closed in '"+timeout+"' milliseconds.");
							}
						}
						
					}else{
						pass("getCurrentProgressBars() Error: fail to get progress bar.");
					}
				}else{
					fail("searchButton(String) Error: fail to click button with text '"+text+"'");
					
				}
				
			}else{
				fail("searchButton(String) Error: fail to search button with text '"+text+"'");
			}

		} catch (RemoteSoloException e) {
			fail("searchButton(String) or waitForDialogToClose(int) Error!");
			debug("Met RemoteSoloException="+e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * <pre>
	 * Test some methods as:
	 * {@link Solo#getCurrentButtons()}
	 * 
	 * {@link Solo#getButton(int)}
	 * {@link Solo#getButton(String)}
	 * {@link Solo#getButton(String, boolean)}
	 * 
	 * {@link Solo#searchButton(String)}
	 * {@link Solo#searchButton(String, boolean)}
	 * {@link Solo#searchButton(String, int)}
	 * {@link Solo#searchButton(String, int, boolean)}
	 * 
	 * {@link Solo#clickOnButton(int)}
	 * {@link Solo#clickOnButton(String)}
	 * 
	 * {@link Solo#searchText(String)}
	 * 
	 * </pre>
	 * 
	 * @see #testDialogActivity()
	 */
	private void testButtons() throws Exception{

		String text = "OK Cancel dialog with a message";
		String textok = "OK";
		String textcancel = "Cancel";
		int index = 0;//the index of button to get on current page, start from 0
		
		try {
			if(solo.waitForView("android.widget.Button")){
				pass("waitForView(String): success wait for button");
			}else{
				fail("waitForView(String): fail to wait for button");
				solo.sleep(1000);
			}
			
			List<String> buttonIDList = solo.getCurrentButtons();
			
			if(buttonIDList!=null){
				pass("getCurrentButtons() Correct: get buttons as:");
				for(String id: buttonIDList){
					debug("button ID="+id);
				}

				//Test something with the first button
				String buttonId = solo.getButton(index);
				if(buttonId!=null){
					pass("getButton(int) Correct: get '"+index+"'th button, whose id is '"+buttonId+"'");

					//Verify the first button id is the same as the first value returned by getCurrentButtons()
					if(buttonIDList.size()>0){
						info("getCurrentButtons() Correct: it returns at the least one button.");
						if(buttonId.equals(buttonIDList.get(0))){
							pass("getButton(0) return the same ID as the first ID returned by getCurrentButtons().");
						}else{
							fail("getButton(0) return different ID from the first ID returned by getCurrentButtons().");
							debug("getButton(0) return ID '"+buttonId+"'");
							debug("getCurrentButtons() first ID is '"+buttonIDList.get(0)+"'.");
						}
					}else{
						error("getCurrentButtons() Error: it should return at the least one button.");
					}
					
					if(solo.searchButton(text)){
						pass("searchButton(String) Correct: search button with text '"+text+"'");
						
						//Use text to get the first button and verify					
						debug("Try to get button with text '"+text+"'");
						String buttonTextId = solo.getButton(text);
						
						if(buttonTextId!=null){
							pass("getButton(String) Correct: get id="+buttonTextId+" for button '"+text+"'");
							if(buttonTextId.equals(buttonId)){
								pass("getButton(0) return the same ID as the ID returned by getButton("+text+").");
							}else{
								pass("getButton(0) return different ID from the ID returned by getButton("+text+").");
								debug("getButton(0) return ID '"+buttonId+"'");
								debug("getButton("+text+") return ID '"+buttonTextId+"'");
							}
						}else{
							pass("getButton(String) Correct: fail to get id for button '"+text+"'");
						}
						
						//Click the button with text to open dialog
						if(solo.clickOnButton(text)){
							pass("clickOnButton(String) Correct: click button with text '"+text+"'");
							
							if(solo.searchButton(textok, true)){
								pass("searchButton(String, boolean) Correct: find visible button '"+textok+"'");
								buttonId = solo.getButton(textok, true);
								if(buttonId!=null){
									pass("getButton(String, boolean) Correct: get id="+buttonId+" for visible button '"+textok+"'");
								}else{
									fail("getButton(String, boolean) Correct: fail to get id for visible button '"+textok+"'");
								}
								
								if(solo.clickOnButton(textok)){
									pass("clickOnButton(String) Correct: click on button '"+textok+"'");
								}else{
									fail("clickOnButton(String) Correct: fail to click on button '"+textok+"'");
								}
								
							}else{
								fail("searchButton(String, boolean) Error: fail to find visible button '"+textok+"'");
							}
							
							//wait for dialog to disappear
							if(solo.searchButton(text)){
								pass("searchButton(String) Correct: find button '"+text+"'");
							}else{
								fail("searchButton(String) Error: fail to find button '"+text+"'");
							}
							
						}else{
							fail("clickOnButton(String) Error: fail to click button with text '"+text+"'");
						}
						
					}else{
						fail("searchButton(String) Error: fail to search button with text '"+text+"'");
					}
					
				}else{
					fail("getButton(int) Error: fail to get '"+index+"'th button");
				}
				
			}else{
				fail("getCurrentButtons() Error: fail to get buttons.");
			}
			
			
			//Test something with the button "Single choice list", the 5th button
			text = "Single choice list";
			index = 4; // the 5th button
			int minMatch = 1;
			if(solo.searchButton(text, minMatch)){
				pass("searchButton(String, int) Correct: successfully search button '"+text+"' for "+minMatch+" time.");
				
				//Click on the 5th button
				if(solo.clickOnButton(index)){
					pass("clickOnButton(int) Correct: click on the "+index+"th button.");
					//verify that the button "Single choice list" was clicked.
					//The title should be "Single choice list" for the opened dialog
					if(solo.searchText(text)){
						pass("searchText(String) Correct: successfully search text '"+text+"'");
						if(solo.searchButton(textcancel, minMatch, true)){
							pass("searchButton(String, int, boolean) Correct: successfully search visible button '"+textcancel+"' for "+minMatch+" time.");
							
							if(solo.clickOnButton(textcancel)){
								pass("clickOnButton(String) Correct: click button with text '"+textcancel+"'");
							}else{
								fail("clickOnButton(String) Error: fail to click button with text '"+textcancel+"'");
							}
						}else{
							fail("searchButton(String, int, boolean) Error: fail to search visible button '"+textcancel+"' for "+minMatch+" time.");
						}
					}else{
						fail("searchText(String) Error: fail to search text '"+text+"'");
					}
					
					//wait for dialog to disappear
					if(solo.searchButton(text)){
						pass("searchButton(String) Correct: find button '"+text+"'");
					}else{
						fail("searchButton(String) Error: fail to find button '"+text+"'");
					}
					
				}else{
					fail("clickOnButton(int) Error: fail to click on the "+index+"th button.");
				}
			}else{
				fail("searchButton(String, int) Error: fail to search button '"+text+"' for "+minMatch+" time.");
			}

		} catch (RemoteSoloException e) {
			fail("searchButton(String)  Error!");
			debug("Met RemoteSoloException="+e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * <pre>
	 * "Views -> Controls -> Default Theme" is the path to 'DefaultTheme Activity'.
	 * This method will open the 'DefaultTheme Activity', 
	 * then it will call {@link #testEditBox()} to
	 * test some methods related to EditBox, finally it will go back to the first page of ApiDemos.
	 * During this process, it will test the following methods
	 * 
	 * Tested methods:
	 * {@link Solo#clickOnText(String, int, boolean)}
	 * {@link Solo#clickOnText(String, int)}
	 * {@link Solo#clickOnText(String)}
	 * 
	 * {@link Solo#getText(int)}
	 * {@link Solo#getText(String)}
	 * {@link Solo#getText(String, boolean)}
	 * 
	 * {@link Solo#getView(String, int)}
	 * {@link Solo#waitForView(String)}
	 * 
	 * {@link Solo#getTopParent(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	void gotoDefaultThemeActivity() throws Exception{
		String firstLevelListUID = null;
		String secondLevelListUID = null;
		String thirdLevelListUID = null;
		
		String topParentID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				topParentID = solo.getTopParent(firstLevelListUID);
				if(topParentID!=null){
					pass("getTopParent(String): got top parent id as '"+topParentID+"' for view '"+firstLevelListUID+"'");
				}else{
					fail("getTopParent(String): fail to get top parent for view '"+firstLevelListUID+"'");
				}
				
				String text = "Views";
				
				if(solo.clickOnText(text, 1, true)){

					pass("clickOnText(String, int, boolean) Correct: Text '"+text+"' was clicked.");
					
					if(solo.waitForView("android.widget.ListView", 1, 1000)){
						pass("waitForView(String, int, long) Correct: 'ListView' appears within timeout");
						
						secondLevelListUID = solo.getView("android.widget.ListView", 0);
						debug("Second level ListView UID= "+secondLevelListUID);
						if(secondLevelListUID!=null){
							pass("getView(String, int) Correct: 'ListView' was got.");
							
							text = "Controls";
							if(solo.clickOnText(text, 1)){
								pass("clickOnText(String, int) Correct: Text '"+text+"' was clicked.");
								
								if(solo.waitForView("android.widget.ListView")){
									pass("waitForView(String) Correct: 'ListView' appears within timeout");
									
									thirdLevelListUID = solo.getView("android.widget.ListView", 0);
									debug("Third level ListView UID= "+thirdLevelListUID);
									if(thirdLevelListUID!=null){
										pass("getView(String, int) Correct: 'ListView' was got.");
										
										//TODO Verify the top parent of thirdLevelListUID is the same as that of firstLevelListUID
										//Always a TimeOutException????
//										String tempTopParentID = solo.getTopParent(thirdLevelListUID);
//										if(tempTopParentID!=null && tempTopParentID.equals(topParentID)){
//											pass("getTopParent(String): verify the top parent of thirdLevelListUID is the same as that of firstLevelListUID");
//										}else{
//											fail("getTopParent(String): fail to verify the top parent of thirdLevelListUID is the same as that of firstLevelListUID");
//										}
										
										text = "Default Theme";
										if(solo.clickOnText(wrapRegex(text))){
											pass("clickOnText(String, int) Correct: Click on '"+text+"' successfully.");
											
											solo.getCurrentActivity();
											String activityName = solo._last_remote_result.getProperty(Message.PARAM_NAME);
											debug("Current Activity is '"+activityName+"'");
											testStuffsInDefaultThemeActivity(activityName);
											
										}else{
											fail("clickOnText(String, int) Error: There is no Text '"+text+"'!");
										}
										
										goBackToViewUID(thirdLevelListUID);
										
									}else{
										fail("getView(String, int) Error: 'ListView' was NOT got.");
									}
									
								}else{
									fail("waitForView(String) Correct: ListView NOT appear!");
								}
								
							}else{
								fail("clickOnText(String, int) Correct: Fail to click on Text '"+text+"'!");
							}
							
							goBackToViewUID(secondLevelListUID);
						}else{
							fail("getView(String, int) Error: 'ListView' was NOT got.");
						}
						
					}else{
						fail("waitForView(String, int, long) Correct: 'ListView' NOT appear within timeout.");
					}
					
				}else{
					fail("clickOnText(String, int, boolean) Error: Fail to click on Text '"+text+"'!");	
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
		
	}
	
	/**
	 * As Robotium's clickInList(), clickOnText() can't work well for List.
	 * I want to try clickOnView(), so I need to get the View by text within List.
	 * 
	 * Try to get the TextView in a ListView
	 * 
	 * @param text		String, the text to find
	 * @param scroll	boolean, if the text is not found, whether need to scroll to find.
	 * @return
	 * @throws Exception
	 * 
	 * @see {@link Solo#getText(String)}
	 */
	private String getTextInList(String text, boolean scroll) throws Exception{
		String uid = null;
		boolean topReached = false;
		boolean bottomReached = false;
		
		while( (uid=solo.getText(text))==null && scroll){
			if(!topReached){
				debug("Scroll up topReached="+topReached);
				topReached = !solo.scrollUp();
			}else if(!bottomReached){
				debug("Scroll down bottomReached="+bottomReached);
				bottomReached = !solo.scrollDown();
			}else{
				//if both top and bottom are reached, stop trying.
				break;
			}
		}
		
		return uid;
	}
	
	/**
	 * <pre>
	 * First this method will wait for the EditBox
	 * {@link Solo#waitForView(String, int, long, boolean)}
	 * 
	 * Then it uses the EditBox UID to verify (positive and negative) methods WaitForViewUID by calling 
	 * {@link #verifyWaitForViewUIDMethods(String, boolean)}.
	 * 
	 * Finally it will verify some methods related to EditBox as following:
	 * {@link Solo#clearEditText(int)}
	 * {@link Solo#clearEditText(String)}
	 * {@link Solo#clickOnEditText(int)}
	 * {@link Solo#enterText(int, String)}
	 * {@link Solo#enterText(String, String)}
	 * {@link Solo#searchEditText(String)}
	 * {@link Solo#getEditText(int)}
	 * {@link Solo#getEditText(String)}
	 * {@link Solo#getEditText(String, boolean)}
	 * {@link Solo#getCurrentEditTexts()}
	 * 
	 * </pre>
	 * 
	 * Most tests are positive.<br>
	 * 
	 * @throws Exception
	 * @see {@link #testRelateToApplication(String)}
	 */
	private void testEditBoxInCustomActivity() throws Exception{
		
		String customTitleActivity = "CustomTitle";
		debug("Try to go back to Activity '"+customTitleActivity+"'");
		
		//A keyboard will be shown automatically, use goBack() to show the 'CustomTitle' activity.
		solo.goBack();
		if(solo.waitForActivity(customTitleActivity)){
			debug("Came back to activity "+customTitleActivity);
		}else{
			error("Can't wait activity "+customTitleActivity+", can't continue.");
			return;
		}
		
		//I know there 2 EditText, so test it with minimumApprence equals to 2
		int minimumApprence = 2;
		if(solo.waitForView("android.widget.EditText",minimumApprence,1000,true)){
			pass("waitForView(String, int, long, boolean) Correct: 'EditText' appears at least 2 times within timeout 1000 millis");
			
			String firstEditTextUID = solo.getView("android.widget.EditText", 0);
			debug("EditText UID= "+firstEditTextUID);
			if(firstEditTextUID!=null){
				pass("getView(String, int) Correct: Got first 'EditText'");
				
				testRelateToApplication(firstEditTextUID);
				
				//=============================  WaitForViewUID ====================================
				//If firstEditTextUID is valid, use it to verify methods WaitForViewUID()
				verifyWaitForViewUIDMethods(firstEditTextUID, true);
				
				//Use firstEditTextUID to create an invalid UID, and make negative test of methods WaitForViewUID()
				//As I know the returned UID will only contains letters "0123456789ABCDEFabcdef-", so add some
				//other letters to make an invalid UID
				verifyWaitForViewUIDMethods(firstEditTextUID+"-QRTYUPLM", false);
				
				//=============================  Verify EditText with UID  ====================================
				//Clear the first EditText (find it by uid)
				if(solo.clearEditText(firstEditTextUID)){
					pass("clearEditText(String) Correct: First EditText UID='"+firstEditTextUID+ "' is cleared.");										
				}else{
					fail("clearEditText(String) Error: First EditText UID='"+firstEditTextUID+"' fail to be cleared.");
				}
				
				//Input some text to the first EditText
				String text1 = "SOMETHING INPUTTED TO FIRST EDITTEXT";
				String text1Regex = ".*INPUTTED TO FIRST EDITT.*";
				if(solo.enterText(firstEditTextUID, text1)){
					pass("enterText(String, String) Correct: Input '"+text1+"' to First EditText");
				}else{
					fail("enterText(String, String) Error: Fail to input '"+text1+"' to first EditText");
				}
				
				//=============================  Verify EditText with Index ====================================
				int firstEditTextIndex = 0;
				int secondEditTextIndex = 1;

				//Click on the second EditText (find it by index)
				if(solo.clickOnEditText(secondEditTextIndex)){
					pass("clickOnEditText(int) Correct: Second EditText is clicked.");
				}else{
					fail("clickOnEditText(int) Correct: Fail to click on Second EditText.");
				}

				//Clear the second EditText (find it by index)
				if(solo.clearEditText(secondEditTextIndex)){
					pass("clearEditText(int) Correct: Second EditText is cleared");										
				}else{
					fail("clearEditText(int) Error: Second EditText fail to be cleared");										
				}
				
				String text2 = "SECOND EDITTEXT GOT YOU";
				//Input something to the second EditText (find it by index)
				if(solo.enterText(secondEditTextIndex, text2)){
					pass("enterText(int, String) Correct: Input '"+text2+"' to EditText");
				}else{
					fail("enterText(int, String) Error: Fail to input '"+text2+"' to EditText");
				}
				
				//get the first EditText by index
				String otherFirstEditTextUID = solo.getEditText(firstEditTextIndex);
				if(otherFirstEditTextUID!=null){
					pass("getEditText(int) Correct: Got first 'EditText'");
					if(otherFirstEditTextUID.equals(firstEditTextUID)){
						pass("The UID return from getEditText(0) is the same as getView(\"android.widget.EditText\", 0) for first EditBox.");
					}else{
						super.fail("The UID return from getEditText(0) doesn't match getView(\"android.widget.EditText\", 0) for first EditBox.");
						super.fail("UID getEditText()="+otherFirstEditTextUID);
						super.fail("UID getView()="+firstEditTextUID);
					}
				}else{
					fail("getEditText(int) Error: Fail to get first 'EditText'");
				}
				
				//=============================  Verify EditText with Text ====================================
				//get the first EditText by text
				otherFirstEditTextUID = solo.getEditText(text1);
				if(otherFirstEditTextUID!=null){
					pass("getEditText(String) Correct: Got first 'EditText'");
					if(otherFirstEditTextUID.equals(firstEditTextUID)){
						pass("The UID return from getEditText(text) is the same as getView(\"android.widget.EditText\", 0) for first EditBox.");
					}else{
						super.fail("The UID return from getEditText(text) doesn't match getView(\"android.widget.EditText\", 0) for first EditBox.");
						super.fail("UID getEditText()="+otherFirstEditTextUID);
						super.fail("UID getView()="+firstEditTextUID);
					}
				}else{
					fail("getEditText(int) Error: Fail to get first 'EditText'");
				}
				
				//get the first EditText by text, whether the EditText is visible or not.
				otherFirstEditTextUID = solo.getEditText(text1, false);
				if(otherFirstEditTextUID!=null){
					pass("getEditText(String, boolean) Correct: Got first 'EditText'");
					if(otherFirstEditTextUID.equals(firstEditTextUID)){
						pass("The UID return from getEditText(text, false) is the same as getView(\"android.widget.EditText\", 0) for first EditBox.");
					}else{
						super.fail("The UID return from getEditText(text, false) doesn't match getView(\"android.widget.EditText\", 0) for first EditBox.");
						super.fail("UID getEditText()="+otherFirstEditTextUID);
						super.fail("UID getView()="+firstEditTextUID);
					}
				}else{
					fail("getEditText(int) Error: Fail to get first 'EditText'");
				}
				
				//Search the first EditText by text
				if(solo.searchEditText(text1Regex)){
					pass("searchEditText(String) Correct: Found '"+text1+"' in first EditText");
				}else{
					fail("searchEditText(String) Error: Fail to find '"+text1+"' in first EditText");
				}
				
				//=============================  Verify get current EditTexts ====================================
				//Get all current edit texts
				List edittexts = solo.getCurrentEditTexts();
				if(edittexts!=null && edittexts.size()>=2){
					pass("getCurrentEditTexts() Maybe Correct: Got at the least 2 EditTexts. Perhaps! Maybe some is empty.");
					
					//Get the UID for second EditText
					String secondEditTextUID = solo.getEditText(firstEditTextIndex);
					if(secondEditTextUID!=null){
						pass("getEditText(int) Correct: Got second 'EditText'");
						
						//Only we got the second EditBox, we will continue to verify getCurrentEditTexts()
						boolean[] match = {false, false};
						//Verify if the EditTexts in list match those got previously
						for(Object uid:edittexts){
							if(!match[0]) match[0] = firstEditTextUID.equals(uid);
							if(!match[1]) match[1] = secondEditTextUID.equals(uid);
							if(match[0] && match[1]) break;
						}
						
						if(match[0] && match[1]){
							pass("getCurrentEditTexts() Correct: Got at the least 2 EditTexts. All matched.");
						}else{
							fail("getCurrentEditTexts() Error: Got at the least 2 EditTexts. NOT all matched.");
						}
						
					}else{
						fail("getEditText(int) Error: Fail to get second 'EditText'");
						super.fail("Fail to verify getCurrentEditTexts()!!!");
					}
					
				}else{
					pass("getCurrentEditTexts() Error: Not got at the least 2 EditTexts.");
				}
				
			}else{
				fail("getView(String, int) Error: Not Got first 'EditText'");
			}
			
		}else{
			fail("waitForView(String, int, long, boolean) Error: 'EditText' NOT appear at least 2 times within timeout 1000 millis");
		}
	}
	
	/**
	 * <pre>
	 * First this method will wait for the DefaultThemeActivity
	 * {@link Solo#waitForActivity(String)}
	 * 
	 * It will verify some methods related to CheckBox as following:
	 * {@link Solo#clickOnCheckBox(int)}
	 * {@link Solo#isCheckBoxChecked(int)}
	 * {@link Solo#isCheckBoxChecked(String)}
	 *
	 * It will verify some methods related to RadioButton as following:
	 * {@link Solo#clickOnRadioButton(int)}
	 * {@link Solo#isRadioButtonChecked(int)}
	 * {@link Solo#isRadioButtonChecked(String)}
	 * 
	 * It will verify some methods related to Spinner as following:
	 * {@link Solo#getCurrentSpinners()}
	 * {@link Solo#pressSpinnerItem(int, int)}
	 * {@link Solo#isSpinnerTextSelected(String)}
	 * {@link Solo#isSpinnerTextSelected(int, String)}
	 * 
	 * It will verify some methods related to ToggleButton as following:
	 * {@link Solo#getCurrentToggleButtons()}
	 * {@link Solo#clickOnToggleButton(String)}
	 * {@link Solo#searchToggleButton(String)}
	 * {@link Solo#searchToggleButton(String, int)}
	 * {@link Solo#isToggleButtonChecked(int)}
	 * {@link Solo#isToggleButtonChecked(String)}
	 * 
	 * </pre>
	 * 
	 * Most tests are positive.<br>
	 * 
	 * @throws Exception
	 */
	private void testStuffsInDefaultThemeActivity(String activityName) throws Exception{
		
		//A keyboard will be shown automatically, use goBack() to show the activity we want.
		debug("Try to go back to Activity '"+activityName+"'");
		
		solo.goBack();
		if(solo.waitForActivity(activityName)){
			debug("Came back to activity "+activityName);
		}else{
			error("Can't wait activity "+activityName+", can't continue.");
			return;
		}
	
		testCheckeBox();
		testRadioButton();
		testSpinner();
		testToggleButton();
		
	}
	
	/**
	 * It will verify some methods related to CheckBox as following:
	 * {@link Solo#getCurrentCheckBoxes()}
	 * {@link Solo#clickOnCheckBox(int)}
	 * {@link Solo#isCheckBoxChecked(int)}
	 * {@link Solo#isCheckBoxChecked(String)}
	 * 
	 * @throws Exception
	 * @see {@link #testStuffsInDefaultThemeActivity(String)}
	 * 
	 */
	private void testCheckeBox() throws Exception{
		/**=============================================CheckBox=====================================================**/
		//There are 2 CheckBox, they should be visible as they are on the top of the layout
		//If the list size is not 2, need to modify code to scroll and make them shown
		List<String> checkboxList = solo.getCurrentCheckBoxes();
		pass("getCurrentCheckBoxes() Correct: Found check box as:");
		for(String ID: checkboxList){
			debug("Check Box ID="+ID);
		}
		
		//I know there 2 CheckBox, so test it with minimumApprence equals to 2
		int minimumApprence = 2;
		if(solo.waitForView("android.widget.CheckBox",minimumApprence,1000,true)){
			pass("waitForView(String, int, long, boolean) Correct: 'CheckBox' appears at least 2 times within timeout 1000 millis");
			
			int index = 0;
			String checkboxName = "Checkbox 1";
			try{
				boolean checked = solo.isCheckBoxChecked(index);
				info("Beofre clickOnCheckBox(int): The check box's checked status is "+checked);
				//Click the first check box
				if(solo.clickOnCheckBox(index)){
					pass("clickOnCheckBox(int) Clicked pass: The "+index+"th CheckBox is clicked.");
					info("After clickOnCheckBox(int): The check box's checked status is "+solo.isCheckBoxChecked(index));
					
					//Check if the click really change the checkbox's checked status
					if(!Boolean.valueOf(checked).equals(Boolean.valueOf(solo.isCheckBoxChecked(index))) ){
						pass("clickOnCheckBox(int) Really work: The "+index+"th CheckBox's checked status changed.");
					}else{
						fail("clickOnCheckBox(int) Fail work: The "+index+"th CheckBox's checked status NOT changed.");
					}
					
					//Use the checkbox'name to verify the same checkbox
					info("isCheckBoxChecked(String) Info: The CheckBox '"+checkboxName+"' checked status is '"+solo.isCheckBoxChecked(checkboxName)+"'");
					
					if(solo.isCheckBoxChecked(checkboxName)==solo.isCheckBoxChecked(index)){
						pass("isCheckBoxChecked(int) and isCheckBoxChecked(String) return the same value");
					}else{
						fail("isCheckBoxChecked(int) and isCheckBoxChecked(String) return DIFFERENT value");
					}
					
				}else{
					fail("clickOnCheckBox(int) Error: Fail to click on "+index+"th CheckBox.");
				}
				
			} catch (RemoteSoloException e) {
				fail("isCheckBoxChecked(int) or isCheckBoxChecked(String) Error!");
				debug("Met RemoteSoloException="+e.getMessage());
				e.printStackTrace();
			}

		}else{
			fail("waitForView(String, int, long, boolean) Error: 'CheckBox' NOT appear at least 2 times within timeout 1000 millis");
		}
	}
	
	/**
	 * It will verify some methods related to RadioButton as following:
	 * {@link Solo#getCurrentRadioButtons()}
	 * {@link Solo#clickOnRadioButton(int)}
	 * {@link Solo#isRadioButtonChecked(int)}
	 * {@link Solo#isRadioButtonChecked(String)}
	 * 
	 * @throws Exception
	 * @see {@link #testStuffsInDefaultThemeActivity(String)}
	 * 
	 */
	private void testRadioButton() throws Exception{
		/**=============================================RadioButton=====================================================**/
		//There are 2 Radio buttons, they should be visible as they are on the top of the layout
		//If the list size is not 2, need to modify code to scroll and make them shown
		List<String> radioButtonList = solo.getCurrentRadioButtons();
		pass("getCurrentRadioButtons() Correct: Found radio buttons as:");
		for(String ID: radioButtonList){
			debug("Radio Button ID="+ID);
		}
		
		//I know there 2 RadioButtons, so test it with minimumApprence equals to 2
		int minimumApprence = 2;
		if(solo.waitForView("android.widget.RadioButton",minimumApprence,1000,true)){
			pass("waitForView(String, int, long, boolean) Correct: 'RadioButton' appears at least 2 times within timeout 1000 millis");
			
			int index = 1;
			String name = "RadioButton 2";

			try {
				boolean checked = solo.isRadioButtonChecked(index);
				info("Beofre clickOnCheckBox(int): The radio button's checked status is "+checked);
				//Click the second radio button
				if(solo.clickOnRadioButton(index)){
					pass("clickOnRadioButton(int) Clicked pass: The "+index+"th RadioButton is clicked.");
					info("After clickOnRadioButton(int): The radio button's checked status is "+solo.isRadioButtonChecked(index));
					
					//Check if the click really change the radiobutton's checked status
					if(!Boolean.valueOf(checked).equals(Boolean.valueOf(solo.isRadioButtonChecked(index))) ){
						pass("clickOnRadioButton(int) Really work: The "+index+"th RadioButton's checked status changed.");
					}else{
						fail("clickOnRadioButton(int) Fail work: The "+index+"th RadioButton's checked status NOT changed.");
					}
					
					//Use the radiobutton's name to verify the same radiobutton
					info("isRadioButtonChecked(String) Info: The CheckBox '"+name+"' checked status is '"+solo.isRadioButtonChecked(name)+"'");
					
					if(solo.isRadioButtonChecked(name)==solo.isRadioButtonChecked(index)){
						pass("isRadioButtonChecked(int) and isRadioButtonChecked(String) return the same value");
					}else{
						fail("isRadioButtonChecked(int) and isRadioButtonChecked(String) return DIFFERENT value");
					}
					
				}else{
					fail("clickOnRadioButton(int) Error: Fail to click on "+index+"th RadionButton.");
				}
			} catch (RemoteSoloException e) {
				fail("isRadioButtonChecked(int) or isRadioButtonChecked(String) Error!");
				debug("Met RemoteSoloException="+e.getMessage());
				e.printStackTrace();
			}

		}else{
			fail("waitForView(String, int, long, boolean) Error: 'RadionButton' NOT appear at least 2 times within timeout 1000 millis");
		}
	}
	
	/**
	 * Verify some methods related to Spinner as following:
	 * {@link Solo#getCurrentSpinners()}
	 * {@link Solo#pressSpinnerItem(int, int)}
	 * {@link Solo#isSpinnerTextSelected(String)}
	 * {@link Solo#isSpinnerTextSelected(int, String)}
	 * 
	 * @throws Exception
	 * @see {@link #testStuffsInDefaultThemeActivity(String)}
	 * 
	 */
	private void testSpinner() throws Exception{
		/**=============================================Spinner=====================================================**/
		//There is 1 Spinner in the current activity
		int minimumApprence = 1;
		List spinners = solo.getCurrentSpinners();
		//Scroll down until we got the spinner
		while((spinners==null || spinners.size()==0) && solo.scrollDown()){
			spinners = solo.getCurrentSpinners();
		}
		
		if(spinners!=null && spinners.size()==minimumApprence){
			pass("getCurrentSpinners() Correct: return "+minimumApprence+" Spinner.");
			String SpinnerID = (String) spinners.get(0);
			info("The spinner UID is '"+SpinnerID+"'");
			
			//There are 8 items in this Spinner
			info("Verify if the first Item is selecte.");
			int spinnerIndex = 0;//As there is only one spinner
			int itemRelativeIndex = 0;//relative to the current selected item
			String item = "Mercury";//is the first item of the whole spinner
			try{
				//Press the first item (in the current view, not in the whole data) in the spinner
				//If we launch the Activity for the first time, the first item will be the first of whole spinner
				if(solo.pressSpinnerItem(spinnerIndex, itemRelativeIndex)){
					pass("pressSpinnerItem(int, int) correct: the first item of "+spinnerIndex+"th spinner is pressed.");
					
					if(solo.isSpinnerTextSelected(spinnerIndex,item)){
						info("isSpinnerTextSelected(int, String) : the item '"+item+"' of "+spinnerIndex+"th spinner is selected.");
					}else{
						info("isSpinnerTextSelected(int, String) : the item '"+item+"' of "+spinnerIndex+"th spinner is not selected.");
					}
					
					if(solo.isSpinnerTextSelected(item)){
						info("isSpinnerTextSelected(String) : the item '"+item+"' of ANY spinner is selected.");
					}else{
						info("isSpinnerTextSelected(String) : the item '"+item+"' of ANY spinner is not selected.");
					}
					
					//Try to select the last item
					itemRelativeIndex = 7;
					item = "Neptune";
					if(solo.pressSpinnerItem(spinnerIndex, itemRelativeIndex)){
						pass("pressSpinnerItem(int, int) correct: the last item of "+spinnerIndex+"th spinner is pressed.");
						
						if(solo.isSpinnerTextSelected(item)){
							info("isSpinnerTextSelected(String) : the item '"+item+"' of ANY spinner is selected.");
						}else{
							info("isSpinnerTextSelected(String) : the item '"+item+"' of ANY spinner is not selected.");
						}						
						
					}else{
						pass("pressSpinnerItem(int, int) fail! ");
					}
					
				}else{
					pass("pressSpinnerItem(int, int) fail! ");
				}
				
			} catch (RemoteSoloException e) {
				fail("isSpinnerTextSelected(String) or isSpinnerTextSelected(int, String) Error!");
				debug("Met RemoteSoloException="+e.getMessage());
				e.printStackTrace();
			}
			
		}else{
			fail("getCurrentSpinners() Error: NOT return "+minimumApprence+" Spinner.");
		}
	}
	
	/**
	 * Verify some methods related to ToggleButton as following:
	 * {@link Solo#getCurrentToggleButtons()}
	 * {@link Solo#clickOnToggleButton(String)}
	 * {@link Solo#searchToggleButton(String)}
	 * {@link Solo#searchToggleButton(String, int)}
	 * {@link Solo#isToggleButtonChecked(int)}
	 * {@link Solo#isToggleButtonChecked(String)}
	 * 
	 * @throws Exception
	 * @see {@link #testStuffsInDefaultThemeActivity(String)}
	 * 
	 */
	private void testToggleButton() throws Exception{
		/**=============================================ToggleButton=====================================================**/
		//There are 2 toggle buttons
		//At the first the 2 buttons are not checked, both texts are "OFF"
		//If the button is checked, the text will changed to "ON"
		List<String> toggleButtons = solo.getCurrentToggleButtons();
		
		//Scroll down until we got the 2 toggle buttons
		while((toggleButtons==null || toggleButtons.size()<2) && solo.scrollDown()){
			toggleButtons = solo.getCurrentToggleButtons();
		}
		
		if(toggleButtons!=null && toggleButtons.size()>0){
			pass("getCurrentToggleButtons() Correct: Found toggle buttons:");
			for(String toggleID: toggleButtons){
				debug("Toggle Button ID="+toggleID);
			}
			
			try{
				String textoff = "OFF";
				String texton = "ON";
				String text = textoff;
				
				//Search for the first toggle button
				if(solo.searchToggleButton(textoff)){
					pass("searchToggleButton(String) Correct: found toggle button with text '"+textoff+"'");
					
					//Will the first "OFF" be clicked?
					//maybe, robotium should provide clickOnToggleButton(String text, int index) or clickOnToggleButton(int index)
					int index = 0;//isToggleButtonChecked index start from 0
					if(solo.clickOnToggleButton(textoff)){
						pass("clickOnToggleButton(String) Correct: click toggle button with text '"+textoff+"'");
						if(solo.isToggleButtonChecked(index)){
							pass("isToggleButtonChecked(int) Correct: the first toggle button is checked.");
							//After checked, the text will change to "ON", verify this
							if(solo.isToggleButtonChecked(texton)){
								pass("isToggleButtonChecked(String) Correct: the first toggle button is checked, text is '"+texton+"'");
							}else{
								fail("isToggleButtonChecked(String) Correct: the first toggle button is NOT checked, text is '"+texton+"'");
							}
							
							//Click the first button again, to change the text to "OFF" again
							if(solo.clickOnToggleButton(texton)){
								pass("clickOnToggleButton(String) Correct: click toggle button with text '"+texton+"'");
								
								//Search for the second toggle button with text "OFF"
								if(solo.searchToggleButton(textoff, 2)){
									pass("searchToggleButton(String,int) Correct: found the second toggle button with text '"+textoff+"' ");
								}else{
									fail("searchToggleButton(String,int) Error: not found the second toggle button with text '"+textoff+"'");
								}
							}else{
								fail("clickOnToggleButton(String) Error: Fail to click toggle button with text '"+texton+"'");
								
							}
						}else{
							pass("isToggleButtonChecked(int) Error: the first toggle button is NOT checked.");
							
						}
					}else{
						fail("clickOnToggleButton(String) Error: Fail to click toggle button with text '"+textoff+"'");
					}
					
				}else{
					fail("searchToggleButton(String) Error: not found toggle button with text '"+textoff+"'");
				}
				

			}catch (RemoteSoloException e) {
				fail("searchToggleButton(String) or searchToggleButton(String, int) Error!");
				fail("isToggleButtonChecked(int) or isToggleButtonChecked(String) Error!");
				debug("Met RemoteSoloException="+e.getMessage());
				e.printStackTrace();
			}
			
		}else{
			fail("getCurrentToggleButtons() Error: Not found any toggle button.");
		}
	}
	
	/**
	 * <pre>
	 * Tested methods:
	 * {@link Solo#waitForViewUID(String)}
	 * {@link Solo#waitForViewUID(String, int, boolean)}
	 * </pre>
	 * 
	 * @param viewUID		String, the view's uid
	 * @param positiveTest	boolean, true for positive test, the viewUID should exist.
	 *                               false for negative test, the viewUID should NOT exist.
	 * @throws Exception
	 */
	void verifyWaitForViewUIDMethods(String viewUID, boolean positiveTest) throws Exception{
		
		if(positiveTest){
			debug("Positive test: View '"+viewUID+"' should be found.");
			
			debug("Test waitForViewUID() with parameter 'uid' ");
			if(solo.waitForViewUID(viewUID)){
				String clazz = solo._last_remote_result.getProperty(Message.PARAM_CLASS);
				pass("waitForViewUID(String) Correct: Got view '"+clazz+"' for UID="+viewUID);
			}else{
				fail("getView(int) Error: Not Got view for UID="+viewUID);			
			}
			
			debug("Test waitForViewUID() with parameters 'uid', 'timeout' and 'scroll' ");
			if(solo.waitForViewUID(viewUID,1000,true)){
				String clazz = solo._last_remote_result.getProperty(Message.PARAM_CLASS);
				pass("waitForViewUID(String, int, boolean) Correct: Got view '"+clazz+"' for UID="+viewUID);
			}else{
				fail("waitForViewUID(String, int, boolean) Error: Not Got view for UID="+viewUID);			
			}
			
		}else{
			debug("Negative test: View '"+viewUID+"' should not be found.");
			
			debug("Test waitForViewUID() with parameter 'uid' ");
			if(solo.waitForViewUID(viewUID)){
				String clazz = solo._last_remote_result.getProperty(Message.PARAM_CLASS);
				fail("waitForViewUID(String) Error: Got view '"+clazz+"' for UID="+viewUID+"; But should not find.");
			}else{
				pass("getView(int) Correct: Not Got view for UID="+viewUID);			
			}
			
			debug("Test waitForViewUID() with parameters 'uid', 'timeout' and 'scroll' ");
			if(solo.waitForViewUID(viewUID, 1000, true)){
				String clazz = solo._last_remote_result.getProperty(Message.PARAM_CLASS);
				fail("waitForViewUID(String, int, boolean) Error: Got view '"+clazz+"' for UID="+viewUID+"; But should not find.");
			}else{
				pass("waitForViewUID(String, int, boolean) Correct: Not Got view for UID="+viewUID);			
			}
		}
	}
	
	/**
	 * <pre>
	 * Tested methods:
	 * {@link Solo#getView(int)}
	 * </pre>
	 * 
	 * @param RID			int, The id used to get the view. This is the generated ID during compilation of AUT.
	 * @param positiveTest  boolean, true for positive test, the RID should exist; 
	 *                               false for negative test, the RID should NOT exist.
	 */
	void verifyGetViewByIdMethods(int RID, boolean positiveTest) throws Exception{
		
		String uid = solo.getView(RID);
		debug("For RID '"+RID+"', Returned View UID="+uid);
		if(positiveTest){
			debug("Positive test.");
			if(uid!=null){
				String clazz = solo._last_remote_result.getProperty(Message.PARAM_CLASS);
				pass("getView(int) Correct: Got view '"+clazz+"' for id="+RID);
			}else{
				fail("getView(int) Error: Not Got view for id="+RID);			
			}
		}else{
			debug("Negative test.");
			if(uid!=null){
				String clazz = solo._last_remote_result.getProperty(Message.PARAM_CLASS);
				fail("getView(int) Error: Got view '"+clazz+"' for id="+RID+"; But should not find.");
			}else{
				pass("getView(int) Correct: Not Got view for id="+RID);			
			}
		}
	}
	
	/**
	 * <pre>
	 * "Views -> Date Widgets -> 1. Dialog" is the path to 'Date Time Picker'.
	 * This method will open the DatePicker and TimePicker, and test related methods.
	 * 
	 * Tested methods:
	 * {@link Solo#clickLongOnText(String)}
	 * {@link Solo#clickLongOnText(String, int)}
	 * {@link Solo#clickLongOnText(String, int, int)}
	 * {@link Solo#clickLongOnText(String, int, boolean)}
	 * 
	 * {@link Solo#getView(String, int)}
	 * {@link Solo#waitForView(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	void gotoDateTimePicker()throws Exception{
		String firstLevelListUID = null;
		String secondLevelListUID = null;
		String thirdLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				String text = "Views";
				
				if(solo.clickLongOnText(text, 1, true)){

					pass("clickLongOnText(String, int, boolean) Correct: Text '"+text+"' was clicked.");
					
					if(solo.waitForView("android.widget.ListView", 1, 1000)){
						pass("waitForView(String, int, long) Correct: 'ListView' appears within timeout");
						
						secondLevelListUID = solo.getView("android.widget.ListView", 0);
						debug("Second level ListView UID= "+secondLevelListUID);
						if(secondLevelListUID!=null){
							pass("getView(String, int) Correct: 'ListView' was got.");
							
							text = "Date Widgets";
							if(solo.clickLongOnText(text, 1)){
								pass("clickLongOnText(String, int) Correct: Text '"+text+"' was clicked.");
								
								if(solo.waitForView("android.widget.ListView")){
									pass("waitForView(String) Correct: 'ListView' appears within timeout");
									
									thirdLevelListUID = solo.getView("android.widget.ListView", 0);
									debug("Third level ListView UID= "+thirdLevelListUID);
									if(thirdLevelListUID!=null){
										pass("getView(String, int) Correct: 'ListView' was got.");
										
										text = "Dialog";
										if(solo.clickLongOnText(wrapRegex(text))){
											pass("clickLongOnText(String, int) Correct: Click on '"+text+"' successfully.");
											
											testDatePicker();
											
											testTimePicker();
																						
										}else{
											fail("clickLongOnText(String, int) Error: There is no Text '"+text+"'!");
										}
										
										goBackToViewUID(thirdLevelListUID);
										
									}else{
										fail("getView(String, int) Error: 'ListView' was NOT got.");
									}
									
								}else{
									fail("waitForView(String) Correct: ListView NOT appear!");
								}
								
							}else{
								fail("clickLongOnText(String, int) Correct: Fail to click on Text '"+text+"'!");
							}
							
							goBackToViewUID(secondLevelListUID);
						}else{
							fail("getView(String, int) Error: 'ListView' was NOT got.");
						}
						
					}else{
						fail("waitForView(String, int, long) Correct: 'ListView' NOT appear within timeout.");
					}
					
				}else{
					fail("clickLongOnText(String, int, boolean) Error: Fail to click on Text '"+text+"'!");	
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
		
	}
	
	/**
	 * <pre>
	 * Used to test methods related to "Date picker":
	 * 
	 * {@link Solo#waitForText(String)}
	 * {@link Solo#waitForText(String, int, long)}
	 * {@link Solo#getText(String)}
	 * {@link Solo#clickOnView(String)}
	 * 
	 * {@link Solo#searchText(String, boolean)}
	 * {@link Solo#searchText(String, int)}
	 * 
	 * {@link Solo#setDatePicker(int, int, int, int)}
	 * {@link Solo#setDatePicker(String, int, int, int)}
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	private void testDatePicker() throws Exception{
		String textdate = "change the date";
		boolean buttonClicked = false;
		
		printCurrentViews(TYPE_TEXTVIEW);
		
		if(solo.waitForText(textdate)){
			pass("waitForText(String) Correct: success wait for text '"+textdate+"'");
			
			debug("Try to click button by method solo.clickOnView(String).");
			String dateButtonTextId = solo.getText(textdate);
			if(dateButtonTextId!=null){
				pass("getText(String) Corrcet: get id '"+dateButtonTextId+"' for text '"+textdate+"'");
				if(solo.clickOnView(dateButtonTextId)){
					pass("clickOnView(String) Correct: click on view whose id="+dateButtonTextId);
					buttonClicked = true;
				}else{
					fail("clickOnView(String) Error: fail to click on view whose id="+dateButtonTextId);
				}
			}else{
				fail("getText(String) Error: fail to get id for text '"+textdate+"'");
			}
			
			if(!buttonClicked){
				debug("Try to click button by method solo.clickOnButton(String).");
				if(solo.clickOnButton(textdate)){
					pass("clickOnButton(String) Correct: click on button '"+textdate+"'");
					buttonClicked = true;
				}else{
					fail("clickOnButton(String) Error: fail to click on button '"+textdate+"'");
				}
			}
		
			String setButtonText = "Set";
			int year = 2000;
			int month = 4;//month start from 0, so 4 will be month MAY
			int day = 1;
			String dateText = ""+(month+1)+"-"+day+"-"+year;
			if(buttonClicked){
				if(solo.waitForText(setButtonText, 1, 500)){
					pass("waitForText(String, int, long) Correct: success wait for text '"+setButtonText+"' for at least one time in 500 millis");
					
					printCurrentViews(TYPE_DATEPICKER);
					
					//Set datepicker with index
					if(solo.setDatePicker(0, year, month, day)){
						pass("setDatePicker(int, int, int, int) Correct: set the first date picker to "+dateText);
						if(solo.clickOnButton(setButtonText)){
							//verify if we set the date correctly
							if(solo.searchText(dateText, 1)){
								pass("searchText(String, int) Correct: success search text '"+dateText+"'");
							}else{
								fail("searchText(String, int) Error: fail to search text '"+dateText+"'");
							}
						}else{
							solo.goBack();
						}
					}else{
						fail("setDatePicker(int, int, int, int) Error: fail to set the first date picker to "+dateText);
					}
					
				}else{
					fail("waitForText(String, int, long) Error: fail to wait for text 'Set date' for at least one time in 500 millis");
				}
			}else{
				debug("Button '"+textdate+"' was not clicked.");
			}
			
			//set datepicker with name
			if(solo.clickOnButton(textdate)){
				pass("clickOnButton(String) Correct: click on button '"+textdate+"'");

				year = 2050;
				month = 8;//month start from 0, so 8 will be month SEP
				day = 23;
				dateText = ""+(month+1)+"-"+day+"-"+year;
				if(solo.waitForView("android.widget.DatePicker")){
					pass("waitForView(String) correct: success wait for view 'DatePicker'");
					String uidDatePicker = solo.getView("android.widget.DatePicker", 0);
					if(uidDatePicker!=null){
						pass("getView(String) correct: success get first view 'DatePicker', id="+uidDatePicker);
						if(solo.setDatePicker(uidDatePicker, year, month, day)){
							pass("setDatePicker(String, int, int, int) Correct: set the first date picker to "+dateText);
							if(solo.clickOnButton(setButtonText)){
								//verify if we set the date correctly
								if(solo.searchText(dateText, true)){
									pass("searchText(String, boolean) Correct: success search text '"+dateText+"'");
								}else{
									fail("searchText(String, boolean) Error: fail to search text '"+dateText+"'");
								}
							}else{
								solo.goBack();
							}
						}else{
							fail("setDatePicker(String, int, int, int) Error: fail to set the first date picker to "+dateText);
						}
					}else{
						fail("getView(String) Error: fail to get first view 'DatePicker'");
					}
				}else{
					fail("waitForView(String) Error: fail to wait for view 'DatePicker'");
				}
			}else{
				fail("clickOnButton(String) Error: fail to click on button '"+textdate+"'");
			}
			
		}else{
			fail("waitForText(String) Error: fail to wait for text '"+textdate+"'");
		}
		
	}
	
	/**
	 * <pre>
	 * Used to test methods related to "Time picker":
	 * 
	 * {@link Solo#waitForView(String)}
	 * {@link Solo#waitForText(String, int, long)}
	 * 
	 * {@link Solo#searchText(String, boolean)}
	 * {@link Solo#searchText(String, int)}
	 * 
	 * {@link Solo#setTimePicker(int, int, int)}
	 * {@link Solo#setTimePicker(String, int, int)}
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	private void testTimePicker() throws Exception{
		String texttime = "change the time";
		
		if(solo.waitForView("android.widget.Button")){
			pass("waitForView(String) Correct: success wait for view 'android.widget.Button");
			
			String setButtonText = "Set";
			//There is on toggle button to show "AM" or "PM", this button will change according to hour value.
			//If the hour is bigger than 12, botton will be PM; otherwise it will be AM.
			int hour = 15;
			int minute = 14;
			String timeText = ""+hour+":"+minute;
			String ampm = "";
			
			if (solo.clickOnButton(texttime)) {
				pass("clickOnButton(String) Correct: click on button '" + texttime + "'");

				if(solo.waitForText(setButtonText, 1, 500)){
					pass("waitForText(String, int, long) Correct: success wait for text '"+setButtonText+"' for at least one time in 500 millis");
					
					printCurrentViews(TYPE_TIMEPICKER);
					
					//Set timepicker with index
					if(solo.setTimePicker(0, hour, minute)){
						ampm = isTimePM()? "PM":"AM";
						pass("setTimePicker(int, int, int) Correct: set the first time picker to "+timeText + " "+ ampm );
						
						if(solo.clickOnButton(setButtonText)){
							//verify if we set the time correctly
							if(solo.searchText(timeText, 1)){
								pass("searchText(String, int) Correct: success search text '"+timeText+"'");
							}else{
								fail("searchText(String, int) Error: fail to search text '"+timeText+"'");
							}
						}else{
							solo.goBack();
						}
					}else{
						fail("setDatePicker(int, int, int, int) Error: fail to set the first date picker to "+timeText);
					}
					
				}else{
					fail("waitForText(String, int, long) Error: fail to wait for text 'Set time' for at least one time in 500 millis");
				}
			} else {
				fail("clickOnButton(String) Error: fail to click on button '"+ texttime + "'");
			}
			
			//set datepicker with name
			if(solo.clickOnButton(texttime)){
				pass("clickOnButton(String) Correct: click on button '"+texttime+"'");

				hour = 4;
				minute = 28;
				timeText = ""+hour+":"+minute;
				
				if(solo.waitForView("android.widget.TimePicker")){
					pass("waitForView(String) correct: success wait for view 'TimePicker'");
					String uid = solo.getView("android.widget.TimePicker", 0);
					if(uid!=null){
						pass("getView(String) correct: success get first view 'TimePicker', id="+uid);
						if(solo.setTimePicker(uid, hour, minute)){
							ampm = isTimePM()? "PM":"AM";
							pass("setTimePicker(String, int, int) Correct: set the first time picker to "+timeText+" "+ampm);
							
							//Change to "PM"
							if(solo.clickOnButton("AM")){
								debug("click on AM, time will be changed to PM");
								if(isTimePM()){
									timeText = ""+(hour+12)+":"+minute;
								}
							}
							
							if(solo.clickOnButton(setButtonText)){
								//verify if we set the time correctly
								if(solo.searchText(timeText, true)){
									pass("searchText(String, boolean) Correct: success search text '"+timeText+"'");
								}else{
									fail("searchText(String, boolean) Error: fail to search text '"+timeText+"'");
								}
							}else{
								solo.goBack();
							}
						}else{
							fail("setTimePicker(String, int, int) Error: fail to set the first time picker to "+timeText);
						}
					}else{
						fail("getView(String) Error: fail to get first view 'TimePicker'");
					}
				}else{
					fail("waitForView(String) Error: fail to wait for view 'TimePicker'");
				}
			}else{
				fail("clickOnButton(String) Error: fail to click on button '"+texttime+"'");
			}
			
		}else{
			fail("waitForView(String) Error: fail to wait for view 'android.widget.Button");
		}
	}
	
	/**
	 * 
	 * @return 	boolean, true if the time is pm
	 * @see #testTimePicker()
	 */
	private boolean isTimePM() throws Exception{
		String pm = "PM";
		String am = "AM";
		
		if(solo.searchText(am,1)) return false;
		
		if(solo.searchText(pm, 1)){
			return true;
		}else{
			if(solo.searchText(pm, 1, true)){
				return true;
			}else{
				if(solo.searchText(pm, 1, true, false)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	/**
	 * <pre>
	 * "Views" is the path to 'Views List'.
	 * Tested methods:
	 * 
	 * {@link Solo#getView(String, int)}
	 * {@link Solo#waitForView(String)}
	 * 
	 * {@link Solo#clickOnText(String, int, boolean)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	void gotoViewsList()throws Exception{
		String firstLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				//Test with the first item of this Views List
				String text = "Views";
				if(solo.clickOnText(text, 1, true)){
					pass("clickOnText(String, int, boolean) click on text '"+text+"'");
					
					testInViewsList();
					
					testScrollInViewsList();
					
					//Robotium 3.6 scroll
					testScrollForRobotium36();
					
					testImageButton();
					
					testGetViews();
					
				}else{
					fail("clickOnText(String, int, boolean) fail to click on text '"+text+"'");
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
		
	}
	
	/**
	 * <pre>
	 * Tested methods:
	 * 
	 * {@link Solo#scrollDown()}
	 * {@link Solo#scrollUp()}
	 * {@link Solo#scrollUpList(int)}
	 * {@link Solo#scrollDownList(int)}
	 * 
	 * {@link Solo#waitForText(String, int, long, boolean)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	private void testScrollInViewsList() throws Exception{
		//Test with the last item of this Views List
		//"WebView" is the last item in the list
		String text = "WebView";
		//robotium should scroll to bottom in list to find the text "WebView"
		if(solo.searchText(text, 1, true)){
			pass("searchText(String, int, boolean) Correct: success search text '"+text+"' for first occurance with scrollalbe");
			
			String uid = solo.getText(text, true);
			if(uid!=null){
				pass("getText(String, boolean) Correct: get uid '"+uid+"' for visible text '"+text+"'");
			}else{
				fail("getText(String, boolean) Error: fail to get uid for visible text '"+text+"'");
			}
			
		}else{
			fail("searchText(String, int, boolean) Error: Fail to search text '"+text+"' for first occurance with scrollable");	
		}

		//Now, we are at the bottom of list
		while(solo.scrollUpList(0)){
			//Scroll to the top of list
			debug("Scrolling up in list ......");
		}
		
		//robotium should scroll in list to find the text "WebView"
		if(solo.searchText(text, 1, true,true)){
			pass("searchText(String, int, boolean,boolean) Correct: success search visible text '"+text+"' for first occurance with scrollalbe");
		}else{
			fail("searchText(String, int, boolean,boolean) Error: Fail to search visible text '"+text+"' for first occurance with scrollable");	
		}
		
		//Now, we are at the bottom of list
		text = "Animation";//The first item in list
		//waitForText() should fail, as "Animation" is at the top of list
		//robotium can only scroll down to search, NEVER scroll up
		if(solo.waitForText(text, 1, 1000, true)){
			fail("waitForText(String, int, long, boolean) Error: success wait visible text '"+text+"' for first occurance with scrollalbe in '1000' millis, but we shouldn't find it.");
		}else{
			pass("waitForText(String, int, long, boolean) Correct: fail to wait visible text '"+text+"' for first occurance with scrollalbe in '1000' millis");
			
			scrollToTop();
			
			if(solo.waitForText(text)){
				pass("waitForText(String): found text '"+text+"'");
			}else{
				fail("waitForText(String): fail to find text '"+text+"'");
			}
		}
		
		//try to find the last item
		text = "WebView";
		if(solo.waitForText(text)){
			pass("waitForText(String) Correct: success wait text '"+text+"'");
		}else{
			fail("waitForText(String) Error: fail to wait text '"+text+"'");
			
			scrollToBottoum();
			
			if(solo.waitForText(text)){
				pass("waitForText(String): found text '"+text+"'");
			}else{
				fail("waitForText(String): fail to find text '"+text+"'");
			}
		}
	}
	
	/**
	 * Test some API recently added in Robotium 3.6<br>
	 * {@link Solo#scrollDownListUID(String)}<br>
	 * {@link Solo#scrollListToBottomUID(String)}<br>
	 * {@link Solo#scrollUpListUID(String)}<br>
	 * {@link Solo#scrollListToTopUID(String)}<br>
	 * {@link Solo#scrollListToLine(int, int)}<br>
	 * {@link Solo#scrollListToLineUID(String, int)}<br>
	 * 
	 * @throws Exception
	 */
	
	private void testScrollForRobotium36() throws Exception{

		boolean success = false;
		boolean canscroll = false;
		
		
		if(solo.waitForView("android.widget.ListView")){
			
			List listviews = solo.getCurrentListViews();
			String listviewuid = "";
			if(listviews.size()>0){
				listviewuid = (String)listviews.get(0);
				debug("Got listview uid="+listviewuid);
				
				//At the first, the list should be able to scroll down
				canscroll = solo.scrollDownListUID(listviewuid);
				if(canscroll){
					pass("Can still be scrolled");
				}else{
					fail("Can't be scrolled");
				}
				
				//After scroll to the bottom, list should not be able to scroll down
				canscroll = solo.scrollListToBottomUID(listviewuid);
				if(canscroll){
					fail("Can still be scrolled, not the bottom");
				}else{
					pass("Can't be scrolled, it is the bottom");
				}
				
				//At the bottom, list should not be able to scroll down
				canscroll = solo.scrollDownListUID(listviewuid);
				if(canscroll){
					fail("Can still be scrolled");
				}else{
					pass("Can't be scrolled");
				}
				
				//At the bottom, the list should be able to scroll up
				canscroll = solo.scrollUpListUID(listviewuid);
				if(canscroll){
					pass("Can still be scrolled");
				}else{
					fail("Can't be scrolled");
				}
				
				//After scroll to the top, the list should not be able to scroll up
				canscroll = solo.scrollListToTopUID(listviewuid);
				if(canscroll){
					fail("Can still be scrolled, not the top");
				}else{
					pass("Can't be scrolled, it is the top");
				}
				
				//At the top, the list should not be able to scroll up
				canscroll = solo.scrollUpListUID(listviewuid);
				if(canscroll){
					fail("Can still be scrolled");
				}else{
					pass("Can't be scrolled");
				}
				
				success = solo.scrollListToLine(0, 12);
				if(success){
					pass("scrollListToLine success.");
				}else{
					fail("scrollListToLine fail.");
				}
				
				solo.sleep(2000);
				
				success = solo.scrollListToLineUID(listviewuid, 3);
				if(success){
					pass("scrollListToLineUID success.");
				}else{
					fail("scrollListToLineUID fail.");
				}
				solo.sleep(2000);
				
			}else{
				fail("Can't get list view to scroll");
			}			
		}else{
			warn("waitForView(): can't wait a ListView.");
		}
	}
	
	/**
	 * <pre>
	 * Tested methods:
	 * 
	 * {@link Solo#getView(String, int)}
	 * {@link Solo#waitForView(String)}
	 * 
	 * {@link Solo#searchText(String, int)}
	 * {@link Solo#searchText(String, int, boolean)}
	 * {@link Solo#searchText(String, int, boolean, boolean)}
	 * 
	 * {@link Solo#getText(int)}
	 * {@link Solo#getText(String)}
	 * {@link Solo#getText(String, boolean)}
	 * 
	 * {@link Solo#clickLongOnText(String, int, int)}
	 * 
	 * {@link Solo#getCurrentTextViews()}
	 * {@link Solo#clickLongOnView(String)}
	 * {@link Solo#getTextViewValue(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	private void testInViewsList() throws Exception{
		String firstLevelListUID = null;
		String secondLevelListUID = null;
		String thirdLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				//============Test with the first item of this Views List===============
				String text = "Animation";
				
				if(solo.searchText(text, 1)){
					pass("searchText(String, int) Correct: success search text '"+text+"' for first occurance.");
					
					String uid = solo.getText(text);
					if(uid!=null){
						pass("getText(String) Correct: get uid '"+uid+"' for text '"+text+"'");
					}else{
						fail("getText(String) Error: fail to get uid for text '"+text+"'");
					}
					
					//The first text is title "Api Demos"
					//The second text is "Animation", so the index is 1
					String uid0 = solo.getText(1);
					if(uid0!=null){
						pass("getText(int) Correct: get uid '"+uid0+"' for the first item in list");
						if(uid0.equals(uid)){
							pass("the first item in list is '"+text+"'");
						}else{
							fail("the first item in list is NOT '"+text+"'");
						}
					}else{
						fail("getText(int) Error: fail to get uid for the first item in list");
					}
					
					//Go to the "Animation" List View
					if(solo.clickLongOnText(text, 1, 500)){
						pass("clickLongOnText(String, int, int) Correct: click the first text '"+text+"' for "+500+" milliseconds.");
						
						if(solo.waitForView("android.widget.ListView")){
							pass("waitForView(String) Correct: 'ListView' appears.");
							
							secondLevelListUID = solo.getView("android.widget.ListView", 0);
							debug("Second level ListView UID= "+secondLevelListUID);
							if(secondLevelListUID!=null){
								pass("getView(String, int) Correct: 'ListView' was got.");
								
								text = "3D Transition";
								String firstItemId = null;
								//Go to the "3D Transition" List view
								List<String> itemIDs = solo.getCurrentTextViews();
								
								if(itemIDs!=null && itemIDs.size()>0){
									pass("getCurrentTextViews() Correct: get "+itemIDs.size()+" items");
									
									//find the item "3D Transition" in list
									for(String id: itemIDs){
										String value = solo.getTextViewValue(id);
										debug("item: '"+value+"'");
										if(text.equals(value)){
											firstItemId = id;
											break;
										}
									}
									
									if(firstItemId!=null){
										debug("foun the item '"+text+"' in the list.");
										if(solo.clickLongOnView(firstItemId)){
											pass("clickLongOnView(String) Correct: success to click on the item '"+text+"'");
											
											if(solo.waitForView("android.widget.ListView")){
												pass("waitForView(String) Correct: 'ListView' appears.");
												
												thirdLevelListUID = solo.getView("android.widget.ListView", 0);
												debug("Third level ListView UID= "+thirdLevelListUID);
												if(thirdLevelListUID!=null){
													pass("getView(String, int) Correct: 'ListView' was got.");
													
													//Go to the "3D Transition" List view
													testIn3DTransitionListView();
													
													goBackToViewUID(thirdLevelListUID);
												}else{
													fail("getView(String, int) Error: 'ListView' was NOT got.");
												}
											}else{
												fail("waitForView(String) Correct: 'ListView' does NOT appear.");
											}
										}else{
											pass("clickLongOnView(String) Correct: fail to click on the item '"+text+"'");
										}
									}else{
										fail("can not find the item '"+text+"' in the list.");
									}
								}else{
									fail("getCurrentTextViews Error: fail to get items.");
								}
								
								goBackToViewUID(secondLevelListUID);
							}else{
								fail("getView(String, int) Error: 'ListView' was NOT got.");
							}
							
						}else{
							fail("waitForView(String) Correct: 'ListView' does NOT appear.");
						}
						
					}else{
						fail("clickLongOnText(String, int, int) Error: fail to click the first text '"+text+"' for "+500+" milliseconds.");
					}
					
				}else{
					fail("searchText(String, int) Error: Fail to search text '"+text+"' for first occurance.");	
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
	}
	
	/**
	 * <pre>
	 * Tested methods:
	 * 
	 * {@link Solo#getView(String, int)}
	 * {@link Solo#waitForView(String)}
	 * 
	 * {@link Solo#searchText(String, int, boolean)}
	 * {@link Solo#clickOnText(String)}
	 * 
	 * {@link Solo#getCurrentImageButtons()}
	 * {@link Solo#getImageButton(int)}
	 * {@link Solo#clickOnImageButton(int)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	private void testImageButton() throws Exception{
		String firstLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				//=============Test with the item "ImageButton" of this Views List===============
				String text = "ImageButton";
				if(solo.searchText(text, 1, true)){
					pass("searchText(String, int, boolean) Correct: success scroll-search text '"+text+"' for first occurance.");
					
					//Go to the "ImageButton" View
					if(solo.clickOnText(text)){
						pass("clickOnText(String) Correct: click the text '"+text+"'");
						
						int numberOfImageButtons = 3;
						List<String> imageButtons = solo.getCurrentImageButtons();
						debug("getCurrentButtons(): got "+imageButtons.size()+" image buttons.");
						printCurrentViews(TYPE_IMAGEBUTTON);
						if(numberOfImageButtons==imageButtons.size()){
							pass("getCurrentButtons(): got "+numberOfImageButtons+" image buttons as expected.");
							
							//Get the second image button
							int imageIndex = 1;
							String secondImageId = solo.getImage(imageIndex);
							if(secondImageId!=null){
								pass("getImageButton(int): got "+(imageIndex+1)+"th image button, the ID is '"+secondImageId+"'");
							}else{
								fail("getImageButton(int): fail to get "+(imageIndex+1)+"th image button");
							}
							
							//Click on the first image button
							imageIndex = 0;
							if(solo.clickOnImageButton(imageIndex)){
								pass("clickOnImageButton(int): clicked on "+(imageIndex+1)+"th image button");
							}else{
								fail("clickOnImageButton(int): fail to click on "+(imageIndex+1)+"th image button");
							}
							
						}else{
							fail("getCurrentButtons(): fail to get "+numberOfImageButtons+" image buttons as expected.");
						}
						
					}else{
						pass("clickOnText(String) Error: fail to click the text '"+text+"'");
					}
					
				}else{
					fail("searchText(String, int, boolean) Error: Fail to scroll-search text '"+text+"' for first occurance.");	
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
	}
	
	/**
	 * <pre>
	 * Tested methods:
	 * 
	 * {@link Solo#getView(String, int)}
	 * {@link Solo#waitForView(String)}
	 * 
	 * {@link Solo#getViews()}
	 * {@link Solo#getCurrentViews()}
	 * {@link Solo#getViews(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	private void testGetViews() throws Exception{
		String firstLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				List<String> currentViews = solo.getCurrentViews();
				int currentViewsNumber = currentViews.size();
				
				List<String> views = solo.getViews();
				
//				List<String> viewsInFirstLevelList = solo.getViews(firstLevelListUID);
				
//				if(currentViewsNumber==views.size() && currentViewsNumber==viewsInFirstLevelList.size()){
					if(currentViewsNumber==views.size()){
//					pass("getViews(),getCurrentViews() and getViews(String): Got the same number of views");
					pass("getViews(),getCurrentViews(): Got the same number of views");
					String viewId = null;
					int viewMatched = 0;
//					int viewInParentMatched = 0;
					for(int i=0;i<currentViewsNumber;i++){
						viewId = currentViews.get(i);
						for(String view: views){
							if(view.equals(viewId)){
								viewMatched++;
								break;
							}
						}
						
//						for(String view: viewsInFirstLevelList){
//							if(view.equals(viewId)){
//								viewInParentMatched++;
//								break;
//							}
//						}
					}
					
					if(viewMatched==currentViewsNumber){
						pass("getViews(),getCurrentViews(): the results are all matched");
					}else{
						fail("getViews(),getCurrentViews(): the results are NOT all matched");
					}
					
//					if(viewInParentMatched==currentViewsNumber){
//						pass("getViews(String),getCurrentViews(): the results are all matched");
//					}else{
//						fail("getViews(String),getCurrentViews(): the results are NOT all matched");
//					}
					
				}else{
					fail("Fail to get the same number of views");
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
	}
	
	/**
	 * <pre>
	 * Test methods:
	 * 
	 * {@link Solo#getCurrentImageViews()}
	 * {@link Solo#getImage(int)}
	 * {@link Solo#clickOnImage(int)}
	 * 
	 * {@link Solo#sleep(int)}
	 * {@link Solo#waitForText(String, int, long, boolean, boolean)}
	 * {@link Solo#clickOnText(String, int, boolean)}
	 * 
	 * {@link Solo#getText(String)}
	 * {@link Solo#clickOnScreen(float, float)}
	 * {@link Solo#clickLongOnScreen(float, float)}
	 * {@link Solo#clickLongOnScreen(float, float, int)}
	 * 
	 * {@link Solo#getScreenSize()}
	 * {@link Solo#getViewLocation(String)}
	 * {@link Solo#waitForViewUID(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 */
	private void testIn3DTransitionListView() throws Exception{
		String text = "Lyon";
		
		//Test the first image
		if(solo.clickOnText(text)){
			pass("clickOnText(String): click on '"+text+"'");
			if(solo.waitForView("android.widget.ImageView")){
				pass("waitForView(String): success wait for 'android.widget.ImageView'");
				
				List<String> imageList = solo.getCurrentImageViews();
				if(imageList!=null && imageList.size()>0){
					pass("getCurrentImageViews(): get some 'android.widget.ImageView'");
					
					String imageId = solo.getImage(0);
					if(imageId!=null){
						if(imageId.equals(imageList.get(0))){
							pass("the first item of getCurrentImageViews() equals to getImage(0)");
						}else{
							fail("the first item of getCurrentImageViews() does NOT equal to getImage(0)");
						}
						
						//click on image so that image disappear and the list will be shown again
						if(solo.clickOnImage(0)){
							pass("clickOnImage(int): success click the first image.");
						}else{
							fail("clickOnImage(int): fail to click the first image.");
						}
					}
				}else{
					fail("getCurrentImageViews(): fail to get some 'android.widget.ImageView'");
				}
				
			}else{
				fail("waitForView(String): fail to wait for 'android.widget.ImageView'");
			}
			
		}else{
			fail("clickOnText(String): fail to click on '"+text+"'");
		}
		
		if(solo.sleep(500)){
			pass("sleep(int): sleep 500 millis");
		}else{
			fail("sleep(int): fail to sleep 500 millis");
		}
		
		text = "Grand Canyon";
		if(solo.waitForText(text, 1, 100, true, true)){
			pass("waitForText(String, int, long, boolean, boolean): suceess wait for visible text '"+text+"' for first occurance in 100 millis");
			String uid = solo.getText(text);
			if(uid!=null){
				pass("getText(String): get uid '"+uid+"' for text '"+text+"'");
				if(solo.clickLongOnView(uid, 300)){
					pass("clickLongOnView(String, int): click on text '"+text+"' for 300 millis");
					
					//click on center of screen so that image disappear and the list will be shown again
					Dimension dim = solo.getScreenSize();
					if(dim!=null){
						pass("getScreenSize(): the screen size is ("+dim.width+","+dim.height+")");
						if(solo.clickOnScreen(dim.width/2, dim.height/2)){
							pass("clickOnScreen(float, float): click screen at ("+dim.width/2+","+dim.height/2+")");
						}else{
							fail("clickOnScreen(float, float): fail to click screen at ("+dim.width/2+","+dim.height/2+")");
						}
						
					}else{
						fail("getScreenSize(): fail to get screen size");
					}
					
				}else{
					fail("clickLongOnView(String, int): fail to click on text '"+text+"' for 300 millis");
				}
			}else{
				fail("getText(String): fail to get uid for text '"+text+"'");
			}
		}else{
			fail("waitForText(String, int, long, boolean, boolean): fail to wait for visible text '"+text+"' for first occurance in 100 millis");
		}
			
		text = "Lake Tahoe";
		if(solo.waitForText(text, 1, 200, true)){
			pass("waitForText(String, int, long, boolean): suceess wait for text '"+text+"' for first occurance in 200 millis");
			
			if(solo.clickOnText(text, 1, true)){
				pass("clickOnText(String, int, boolean): suceess click on text '"+text+"' for first occurance");
				
				//click on center of screen so that image disappear and the list will be shown again
				Dimension dim = solo.getScreenSize();
				if(dim!=null){
					pass("getScreenSize(): the screen size is ("+dim.width+","+dim.height+")");
					if(solo.clickLongOnScreen(dim.width/2, dim.height/2)){
						pass("clickLongOnScreen(float, float): click long at screen ("+dim.width/2+","+dim.height/2+")");
					}else{
						fail("clickLongOnScreen(float, float): fail to click long at screen ("+dim.width/2+","+dim.height/2+")");
					}
				}else{
					fail("getScreenSize(): fail to get screen size");
				}
			}else{
				fail("clickOnText(String, int, boolean): fail to click on text '"+text+"' for first occurance");
			}
						
		}else{
			fail("waitForText(String, int, long, boolean): fail to wait for text '"+text+"' for first occurance in 200 millis");
		}
		
		text = "Tahoe Pier";
		if(solo.waitForText(text, 1, 200, true)){
			pass("waitForText(String, int, long, boolean): suceess wait for text '"+text+"' for first occurance in 200 millis");
			
			if(solo.clickOnText(text, 1, true)){
				pass("clickOnText(String, int, boolean): suceess click on text '"+text+"' for first occurance");
				
				String imageId = solo.getImage(0);
				if(imageId != null){
					pass("getImage(String): get image '"+imageId+"'");
					//click on center of image so that image disappear and the list will be shown again
					Rectangle loc = solo.getViewLocation(imageId);
					
					if(loc!=null){
						pass("getViewLocation(String): the image location is at ("+loc.x+","+loc.y+"), size is ("+loc.width+","+loc.height+")");
						int x = loc.x + loc.width/2;
						int y = loc.y + loc.height/2;
						if(solo.clickLongOnScreen(x, y, 200)){
							pass("clickLongOnScreen(float, float, int): click on ("+x+","+y+")");
						}else{
							fail("clickLongOnScreen(float, float, int): fail to click on ("+x+","+y+")");
						}
					}else{
						fail("getViewLocation(String): fail to get image location.");
					}
				}else{
					pass("getImage(String): fail to get image.");
				}
				
			}else{
				fail("clickOnText(String, int, boolean): fail to click on text '"+text+"' for first occurance");
			}
						
		}else{
			fail("waitForText(String, int, long, boolean): fail to wait for text '"+text+"' for first occurance in 200 millis");
		}
		
	}
	
	/**
	 * <pre>
	 * "App -> Menu -> Inflate from XML" is the path to 'MenuInflateFromXml Activity'.
	 * This method will open the 'MenuInflateFromXml Activity', then it will call {@link #testMenu(String)} to
	 * test some methods related to Menu, finally it will go back to the first page of ApiDemos.
	 * During this process, it will test the following methods
	 * 
	 * Tested methods:
	 * {@link Solo#waitForView(String)}
	 * {@link Solo#waitForView(String, int, long)}
	 * 
	 * {@link Solo#getView(String, int)}
	 * 
	 * {@link Solo#clickOnText(String)}
	 * {@link Solo#clickOnText(String, int)}
	 * {@link Solo#clickOnText(String, int, boolean)}
	 * 
	 * {@link Solo#clickInList(int)}
	 * 
	 * {@link Solo#goBackToActivity(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 * @see #testMenu()
	 */
	void gotoInflateMenu() throws Exception{
		String firstLevelListUID = null;
		String secondLevelListUID = null;
		String thirdLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				String text = "App";
				if(solo.clickOnText(wrapRegex(text), 1)){
					pass("clickOnText(String, int) Correct: Text '"+text+"' was clicked.");
					
					if(solo.waitForView("android.widget.ListView", 1, 1000)){
						pass("waitForView(String, int, long) Correct: 'ListView' appears within timeout");
						
						secondLevelListUID = solo.getView("android.widget.ListView", 0);
						debug("Second level ListView UID= "+secondLevelListUID);
						if(secondLevelListUID!=null){
							pass("getView(String, int) Correct: 'ListView' was got.");
							
							text = "Menu";
							//review the source code of Robotium: clickOnText() will only scroll down to search text, never up
							//If the text is above, the method clickOnText will certainly fail.
							if(solo.clickOnText(wrapRegex(text), 1, true)){
								pass("clickOnText(String, int, boolean) Correct: Text '"+text+"' was clicked.");
								
								if(solo.waitForView("android.widget.ListView")){
									pass("waitForView(String) Correct: 'ListView' appears within timeout");
									
									thirdLevelListUID = solo.getView("android.widget.ListView", 0);
									debug("Third level ListView UID= "+thirdLevelListUID);
									if(thirdLevelListUID!=null){
										pass("getView(String, int) Correct: 'ListView' was got.");
										
										text = "Inflate from XML";
										if(solo.clickOnText(wrapRegex(text))){
											pass("clickOnText(String, int) Correct: Click on '"+text+"' successfully.");
											testMenu(thirdLevelListUID);
											
										}else{
											fail("clickOnText(String, int) Error: There is no Text '"+text+"'!");
										}
										
										goBackToViewUID(thirdLevelListUID);
										
									}else{
										fail("getView(String, int) Error: 'ListView' was NOT got.");
									}
									
								}else{
									fail("waitForView(String) Error: ListView NOT appear!");
								}
								
							}else{
								fail("clickOnText(String, int, boolean) Error: Fail to click on Text '"+text+"'!");
							}
							
							goBackToViewUID(secondLevelListUID);
						}else{
							fail("getView(String, int) Error: 'ListView' was NOT got.");
						}
						
					}else{
						fail("waitForView(String, int, long) Error: 'ListView' NOT appear within timeout.");
					}
					
				}else{
					fail("clickOnText(String, int) Error: Fail to click on Text '"+text+"'!");	
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
		
	}
	
	/**
	 * <pre>
	 * Test some methods as:
	 * {@link Solo#sendKey(int)}
	 * {@link Solo#pressMenuItem(int)}
	 * {@link Solo#pressMenuItem(int, int)}
	 * {@link Solo#clickOnMenuItem(String)}
	 * {@link Solo#clickOnMenuItem(String, boolean)}
	 * {@link Solo#clickLongOnTextAndPress(String, int)}
	 * 
	 * {@link Solo#waitForText(String)}
	 * {@link Solo#clickOnText(String)}
	 * 
	 * </pre>
	 * 
	 * @see #gotoInflateMenu()
	 */
	private void testMenu(String parentListID) throws Exception{

		String text = "Jump";
		String resultLabel = "Jump up";
		
		try {
			//Manually to show the menu
			if(solo.sendKey(Solo.MENU)){
				pass("sendKey(int) Correct: send key 'MENU'");
				
				//Click menu item "Jump"
				if(solo.clickOnText(text)){
					pass("clickOnText(String): click on menu item 'Jump'");
					
					//Verify that item "Jump" was clicked
					if(solo.waitForText(wrapRegex(resultLabel))){
						pass("waitForText(String): verify that menu item 'Jump' was clicked");
						//wait for the "result text" disappear
						solo.sleep(2000);
					}else{
						fail("waitForText(String): fail to verify that menu item 'Jump' was clicked");
					}
					
				}else{
					fail("clickOnText(String): fail to click on menu item 'Jump'");
				}
			}else{
				fail("sendKey(int) Error: fail to send key 'MENU'");
			}

			//pressXXXMenuItem and clickXXXMenuItem will show the menu automatically
			int index = 0;
			if(solo.pressMenuItem(index)){
				pass("pressMenuItem(int): press on first menu item of the first row");
				
				//Verify that item "Jump" was clicked
				if(solo.waitForText(wrapRegex(resultLabel))){
					pass("waitForText(String): verify that menu item 'Jump' was clicked");
					//wait for the "result text" disappear
					solo.sleep(2000);
				}else{
					fail("waitForText(String): fail to verify that menu item 'Jump' was clicked");
				}
			}else{
				fail("pressMenuItem(int): fail to press on first menu item of the first row");
			}
			
			index = 1;
			int itemsPerRow = 2;
			resultLabel = "Dive into";
			if(solo.pressMenuItem(index, itemsPerRow)){
				pass("pressMenuItem(int, int): press on second menu item of the first row");
				
				//Verify that item "Dive" was clicked
				if(solo.waitForText(wrapRegex(resultLabel))){
					pass("waitForText(String): verify that menu item 'Dive' was clicked");
					//wait for the "result text" disappear
					solo.sleep(2000);
				}else{
					fail("waitForText(String): fail to verify that menu item 'Dive' was clicked");
				}
			}else{
				fail("pressMenuItem(int, int): fail to press on second menu item of the first row");
			}
			
			
			//Go back to parent list
			goBackToViewUID(parentListID);
			String listItem = "Inflate from XML";
			if(solo.clickOnText(wrapRegex(listItem), 1)){
				pass("clickOnText(String): click on text '"+listItem+"'");	
				
				if(solo.waitForView("android.widget.Spinner")){
					pass("waitForView(String): success wait for 'android.widget.Spinner'");
					int spinnerIndex = 0;//As there is only one spinner
					int itemRelativeIndex = 5;//relative to the current selected item
					String item = "Shortcuts";//the item that should be clicked
					
					if(solo.pressSpinnerItem(spinnerIndex, itemRelativeIndex)){
						pass("pressSpinnerItem(int, int) correct: the sixth item of "+spinnerIndex+"th spinner is pressed.");
						
						if(solo.isSpinnerTextSelected(spinnerIndex,item)){
							pass("isSpinnerTextSelected(int, String) correct: the item '"+item+"' of "+spinnerIndex+"th spinner is selected.");
							
							String menuText = "Eric";
							if(solo.clickOnMenuItem(menuText)){
								pass("clickOnMenuItem(String): press on menu item '"+menuText+"'");
								
								//Verify that item "Eric" was clicked
								if(solo.waitForText(wrapRegex(menuText))){
									pass("waitForText(String): verify that menu item '"+menuText+"' was clicked");
									//wait for the "result text" to disappear
									solo.sleep(2000);
								}else{
									fail("waitForText(String): fail to verify that menu item '"+menuText+"' was clicked");
								}
							}else{
								fail("clickOnMenuItem(String): fail to press on menu item '"+menuText+"'");
							}
							
							menuText = "Bart";
							if(solo.clickOnMenuItem(menuText, false)){//TODO Should test with 'true'
								pass("clickOnMenuItem(String, boolean): press on menu item '"+menuText+"'");
								
								//Verify that item "Bart" was clicked
								if(solo.waitForText(wrapRegex(menuText))){
									pass("waitForText(String): verify that menu item '"+menuText+"' was clicked");
									//wait for the "result text" to disappear
									solo.sleep(2000);
								}else{
									fail("waitForText(String): fail to verify that menu item '"+menuText+"' was clicked");
								}
							}else{
								fail("clickOnMenuItem(String, boolean): fail to press on menu item '"+menuText+"'");
							}
							
							//Manually to show the menu
							if(solo.sendKey(Solo.MENU)){
								pass("sendKey(int) Correct: send key 'MENU'");
								
								menuText = "More";
								int subMenuIndex = 2;
								if(solo.clickLongOnTextAndPress(menuText, subMenuIndex)){
									pass("clickLongOnTextAndPress(String, int): click on text '"+menuText+"' and press item index '"+subMenuIndex+"'");
									
									//Verify that item "Henry" was clicked
									String subMenuText = "Henry";
									if(solo.waitForText(wrapRegex(subMenuText))){
										pass("waitForText(String): verify that menu item '"+subMenuText+"' was clicked");
										//wait for the "result text" to disappear
										solo.sleep(2000);
									}else{
										fail("waitForText(String): fail to verify that menu item '"+subMenuText+"' was clicked");
									}
								}else{
									fail("clickLongOnTextAndPress(String, int): fail to click on text '"+menuText+"' and press item index '"+subMenuIndex+"'");
								}
							
							}else{
								fail("sendKey(int) Error: fail to send key 'MENU'");
							}
							
						}else{
							fail("isSpinnerTextSelected(int, String) error: the item '"+item+"' of "+spinnerIndex+"th spinner is not selected.");
						}
					}else{
						fail("pressSpinnerItem(int, int) error: fail to press the sixth item of "+spinnerIndex+"th spinner.");
					}
					
				}else{
					fail("waitForView(String): fail to wait for 'android.widget.Spinner'");
				}
			}else{
				fail("clickOnText(String): fail to click on text '"+listItem+"'");		
			}
			
		} catch (RemoteSoloException e) {
			fail("waitForText(String) Error!");
			debug("Met RemoteSoloException="+e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	/**
	 * <pre>
	 * "Views -> Gallery -> 1. Photos" is the path to 'Gallery1 Activity'.
	 * This method will open the 'Gallery1 Activity', then it will call {@link #testDrag()} to
	 * test some methods related to drag, finally it will go back to the first page of ApiDemos.
	 * During this process, it will test the following methods
	 * 
	 * Tested methods:
	 * {@link Solo#waitForView(String)}
	 * {@link Solo#waitForView(String, int, long)}
	 * 
	 * {@link Solo#getView(String, int)}
	 * 
	 * {@link Solo#clickOnText(String)}
	 * {@link Solo#clickOnText(String, int)}
	 * {@link Solo#clickOnText(String, int, boolean)}
	 * 
	 * {@link Solo#clickInList(int)}
	 * 
	 * {@link Solo#goBackToActivity(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 * @see #testDrag()
	 */
	void gotoPhotoGallery() throws Exception{
		String firstLevelListUID = null;
		String secondLevelListUID = null;
		String thirdLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				String text = "Views";
				if(solo.clickOnText(wrapRegex(text), 1, true)){
					pass("clickOnText(String, int, boolean) Correct: Text '"+text+"' was clicked.");
					
					if(solo.waitForView("android.widget.ListView", 1, 1000)){
						pass("waitForView(String, int, long) Correct: 'ListView' appears within timeout");
						
						secondLevelListUID = solo.getView("android.widget.ListView", 0);
						debug("Second level ListView UID= "+secondLevelListUID);
						if(secondLevelListUID!=null){
							pass("getView(String, int) Correct: 'ListView' was got.");
							
							text = "Gallery";
							//review the source code of Robotium: clickOnText() will only scroll down to search text, never up
							//If the text is above, the method clickOnText will certainly fail.
							if(solo.clickOnText(wrapRegex(text), 1, true)){
								pass("clickOnText(String, int, boolean) Correct: Text '"+text+"' was clicked.");
								
								if(solo.waitForView("android.widget.ListView")){
									pass("waitForView(String) Correct: 'ListView' appears within timeout");
									
									thirdLevelListUID = solo.getView("android.widget.ListView", 0);
									debug("Third level ListView UID= "+thirdLevelListUID);
									if(thirdLevelListUID!=null){
										pass("getView(String, int) Correct: 'ListView' was got.");
										
										text = "Photos";
										if(solo.clickOnText(wrapRegex(text))){
											pass("clickOnText(String, int) Correct: Click on '"+text+"' successfully.");
											testDrag();
											
										}else{
											fail("clickOnText(String, int) Error: There is no Text '"+text+"'!");
										}
										
										goBackToViewUID(thirdLevelListUID);
										
									}else{
										fail("getView(String, int) Error: 'ListView' was NOT got.");
									}
									
								}else{
									fail("waitForView(String) Error: ListView NOT appear!");
								}
								
							}else{
								fail("clickOnText(String, int, boolean) Error: Fail to click on Text '"+text+"'!");
							}
							
							goBackToViewUID(secondLevelListUID);
						}else{
							fail("getView(String, int) Error: 'ListView' was NOT got.");
						}
						
					}else{
						fail("waitForView(String, int, long) Error: 'ListView' NOT appear within timeout.");
					}
					
				}else{
					fail("clickOnText(String, int, boolean) Error: Fail to click on Text '"+text+"'!");	
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
		
	}
	
	/**
	 * <pre>
	 * Test some methods as:
	 * {@link Solo#getScreenSize()}
	 * {@link Solo#getImage(int)}
	 * {@link Solo#getViewLocation(String)}
	 * {@link Solo#clickOnImage(int)}
	 * 
	 * {@link Solo#waitForText(String)}
	 * {@link Solo#scrollToSide(int)}
	 * {@link Solo#drag(float, float, float, float, int)}
	 * 
	 * </pre>
	 * 
	 * @see #gotoPhotoGallery()
	 */
	private void testDrag() throws Exception{
		
		Dimension screen = solo.getScreenSize();
		info("Screen size is "+screen.toString());
		
		//Store the image that we have viewed, the order should be from first to last
		List<String> images = new ArrayList<String>();
		//The index of ImageView on the screen, we want to get middle image,
		//for the first image, the middle image's index is 0
		//for other imaage, the middle image's index is 1
		int index = 0;
		//currentIndex is the absolute index of the whole imageview
		int currentIndex = 0;
		String image = solo.getImage(index);
		String longPressText = "Longpress: ";
		String longPressResult = "Testing";
		
		if(image!=null){
			pass("getImage(int): get the "+index+"th ImageView.");
			images.add(image);
			
			if(solo.clickOnImage(index)){
				pass("clickOnImage(int): click the "+index+"th ImageView.");
				
				//Verify that the first image was clicked
				if(solo.waitForText(String.valueOf(currentIndex))){
					pass("waitForText(String): verify that the "+index+"th ImageView was clicked");
					//wait for the "notification" to disappear
					solo.sleep(2000);
					
					//for other image, the index should be 1
					index = 1;
					
					//TODO solo.scrollToSide(Solo.LEFT) has no effect, because robotium use drag API and drag
					//at from (0,screenHeight/2), but the point (0,screenHeight/2) can't be dragged.
					//Scroll to left and get the next image
					//scrollToSide will scroll horizontally half screen.
//					if(solo.scrollToSide(Solo.LEFT)){
//						pass("scrollToSide(int): success scroll to LEFT");
//						currentIndex++;
//						
//						image = solo.getImage(index);
//						
//						if(image!=null){
//							pass("getImage(int): get the "+index+"th ImageView.");
//							if(!image.equals(images.get(currentIndex-1))){
//								images.add(image);
//								info("getImage(int): get a new ImageView.");
//								
//								if(solo.clickLongOnView(image)){
//									pass("clickLongOnView(String): click the ImageView '"+image+"'");
//									
//									if(solo.waitForText(longPressResult) && solo.clickOnText(longPressResult)){
//										pass("Wait and Click on text '"+longPressResult+"'");
//										if(solo.waitForText(longPressText+currentIndex)){
//											pass("waitForText(String): verify that the "+currentIndex+"th ImageView was clicked");
//											//wait for the "notification" to disappear
//											solo.sleep(2000);
//										}else{
//											fail("waitForText(String): fail to verify that the "+currentIndex+"th ImageView was clicked");
//										}
//									}else{
//										pass("Fail to Wait and Click on text '"+longPressResult+"'");
//									}
//								}else{
//									fail("clickLongOnView(String): fail to click the ImageView '"+image+"'");
//								}
//								
//							}else{
//								info("getImage(int): get the same ImageView.");
//								currentIndex--;
//							}
//						}else{
//							fail("getImage(int): fail to get the "+index+"th ImageView.");
//						}
//						
//					}else{
//						fail("scrollToSide(int): fail to scroll to LEFT");
//					}
					
					image = images.get(currentIndex);
					Rectangle location = solo.getViewLocation(image);
					if(location!=null){
						pass("getViewLocation(String): success get image location="+location.toString());
						
						int centerx = location.x + location.width/2;
						int centery = location.y + location.height/2;
						int destx = centerx - screen.width/2;
						int desty = centery;
						
						if(destx<0) destx=0;
						info("drag from ("+centerx+","+centery+") to ("+destx+","+desty+")");
						if(solo.drag(centery, destx, centery, desty, 20)){
							pass("drag(float, float, float, float, int): success drag from ("+centerx+","+centery+") to ("+destx+","+desty+")");
							
							currentIndex++;
							
							image = solo.getImage(index);
							
							if(image!=null){
								pass("getImage(int): get the "+index+"th ImageView.");
								if(!image.equals(images.get(currentIndex-1))){
									images.add(image);
									info("getImage(int): get a new ImageView.");
									
									if(solo.clickLongOnView(image)){
										pass("clickLongOnView(String): click the ImageView '"+image+"'");
										
										if(solo.waitForText(longPressResult) && solo.clickOnText(longPressResult)){
											pass("Wait and Click on text '"+longPressResult+"'");
											if(solo.waitForText(longPressText+currentIndex)){
												pass("waitForText(String): verify that the "+currentIndex+"th ImageView was clicked");
												//wait for the "notification" to disappear
												solo.sleep(2000);
											}else{
												fail("waitForText(String): fail to verify that the "+currentIndex+"th ImageView was clicked");
											}
										}else{
											pass("Fail to Wait and Click on text '"+longPressResult+"'");
										}
									}else{
										fail("clickLongOnView(String): fail to click the ImageView '"+image+"'");
									}
									
								}else{
									info("getImage(int): get the same ImageView.");
									currentIndex--;
								}
							}else{
								fail("getImage(int): fail to get the "+index+"th ImageView.");
							}
							
						}else{
							fail("drag(float, float, float, float, int): fail to drag from ("+centerx+","+centery+") to ("+destx+","+desty+")");
						}
						
					}else{
						fail("getViewLocation(String): fail to get image location");
					}
					
				}else{
					fail("waitForText(String): fail to verify that the "+index+"th ImageView was clicked");
				}
			}else{
				fail("clickOnImage(int): fail to click the "+index+"th ImageView.");
			}
			
		}else{
			fail("getImage(int): fail to get the "+index+"th ImageView.");
		}
	
	}
	
	/**
	 * <pre>
	 * "Views -> Grid -> 1. Icon Grid" is the path to 'Grid1 Activity'.
	 * This method will open the 'Grid1 Activity', then it will call {@link #testGrid()} to
	 * test some methods related to grid, finally it will go back to the first page of ApiDemos.
	 * During this process, it will test the following methods
	 * 
	 * Tested methods:
	 * {@link Solo#waitForView(String)}
	 * {@link Solo#waitForView(String, int, long)}
	 * 
	 * {@link Solo#getView(String, int)}
	 * 
	 * {@link Solo#clickOnText(String)}
	 * {@link Solo#clickOnText(String, int)}
	 * {@link Solo#clickOnText(String, int, boolean)}
	 * 
	 * {@link Solo#clickInList(int)}
	 * 
	 * {@link Solo#goBackToActivity(String)}
	 * 
	 * </pre>
	 * 
	 * All tests are positive.
	 * @see #testGrid()
	 */
	void gotoIconGrid() throws Exception{
		String firstLevelListUID = null;
		String secondLevelListUID = null;
		String thirdLevelListUID = null;
		
		if(solo.waitForView("android.widget.ListView")){
			pass("waitForView(String) Correct: 'ListView' appears.");
			
			scrollToTop();
			
			firstLevelListUID = solo.getView("android.widget.ListView", 0);
			debug("First level ListView UID= "+firstLevelListUID);
			if(firstLevelListUID!=null){
				pass("getView(String, int) Correct: 'ListView' was got.");
				
				String text = "Views";
				if(solo.clickOnText(wrapRegex(text), 1, true)){
					pass("clickOnText(String, int, boolean) Correct: Text '"+text+"' was clicked.");
					
					if(solo.waitForView("android.widget.ListView", 1, 1000)){
						pass("waitForView(String, int, long) Correct: 'ListView' appears within timeout");
						
						secondLevelListUID = solo.getView("android.widget.ListView", 0);
						debug("Second level ListView UID= "+secondLevelListUID);
						if(secondLevelListUID!=null){
							pass("getView(String, int) Correct: 'ListView' was got.");
							
							text = "Grid";
							//review the source code of Robotium: clickOnText() will only scroll down to search text, never up
							//If the text is above, the method clickOnText will certainly fail.
							if(solo.clickOnText(wrapRegex(text), 1, true)){
								pass("clickOnText(String, int, boolean) Correct: Text '"+text+"' was clicked.");
								
								if(solo.waitForView("android.widget.ListView")){
									pass("waitForView(String) Correct: 'ListView' appears within timeout");
									
									thirdLevelListUID = solo.getView("android.widget.ListView", 0);
									debug("Third level ListView UID= "+thirdLevelListUID);
									if(thirdLevelListUID!=null){
										pass("getView(String, int) Correct: 'ListView' was got.");
										
										text = "Icon Grid";
										if(solo.clickOnText(wrapRegex(text))){
											pass("clickOnText(String, int) Correct: Click on '"+text+"' successfully.");
											testGrid();
											
										}else{
											fail("clickOnText(String, int) Error: There is no Text '"+text+"'!");
										}
										
										goBackToViewUID(thirdLevelListUID);
										
									}else{
										fail("getView(String, int) Error: 'ListView' was NOT got.");
									}
									
								}else{
									fail("waitForView(String) Error: ListView NOT appear!");
								}
								
							}else{
								fail("clickOnText(String, int, boolean) Error: Fail to click on Text '"+text+"'!");
							}
							
							goBackToViewUID(secondLevelListUID);
						}else{
							fail("getView(String, int) Error: 'ListView' was NOT got.");
						}
						
					}else{
						fail("waitForView(String, int, long) Error: 'ListView' NOT appear within timeout.");
					}
					
				}else{
					fail("clickOnText(String, int, boolean) Error: Fail to click on Text '"+text+"'!");	
				}
				
				goBackToViewUID(firstLevelListUID);

			}else{
				fail("getView(String, int) Error: 'ListView' was NOT got.");
			}
						
		}else{
			fail("waitForView(String) Error: ListView NOT appear!");
		}
		
	}
	
	/**
	 * <pre>
	 * Test some methods as:
	 * {@link Solo#getCurrentGridViews()}
	 * {@link Solo#getCurrentScrollViews()}
	 * 
	 * </pre>
	 * 
	 * @see #gotoIconGrid()
	 */
	private void testGrid() throws Exception{
		
		List<String> grids = solo.getCurrentGridViews();
		if(grids!=null && grids.size()>0){
			pass("getCurrentGridViews(): success get '"+grids.size()+"' GridView.");
		}else{
			fail("getCurrentGridViews(): fail to get GridView.");
		}
	
		List<String> scrolls = solo.getCurrentScrollViews();
		if(scrolls!=null && scrolls.size()>0){
			pass("getCurrentScrollViews(): success get '"+scrolls.size()+"' ScrollView.");
		}else{
			fail("getCurrentScrollViews(): fail to get ScrollView.");
		}
		
	}
	
	/**
	 * Before you call this method, you MUST execute a command by solo.<br>
	 * Otherwise, you will NOT get the correct result information.<br>
	 * 
	 * If you just want to print message, call {@link super#fail(String)} instead.<br>
	 */
	public void fail( String message) {
		super.fail(message);
		showRemoteResult();
	}
	
	/**
	 * Before you call this method, you MUST execute a command by solo.<br>
	 * Otherwise, you will NOT get the correct result information.<br>
	 * 
	 * When you want to know the remote result info, call this method.<br>
	 * Normally, when you fail, you want to know the reason, you can call this.<br>
	 */
	void showRemoteResult(){
		System.out.println("======= Remote Result Info: "+solo._last_remote_result.getProperty(Message.KEY_REMOTERESULTINFO)+" =====");
		System.err.println("======= Remote Error Message: "+solo._last_remote_result.getProperty(Message.PARAM_ERRORMSG)+" =====");
	}
	
	/**
	 * @param args	Array of String passed from commnand line: messenger=XXX runner=XXX instrument=XXX
	 */
	public static void main(String[] args){
		SoloTest soloTest = new SoloTestAndroidApi(args);

//		soloTest.setInstallAUT(false);
//		soloTest.setInstallMessenger(false);
//		soloTest.setInstallRunner(false);
		soloTest.setAUTApk(DEFAULT_AUT_APK);
		
		soloTest.process();
	}
}
