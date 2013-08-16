/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.client.processor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;
import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Base64;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
//import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SlidingDrawer;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.jayway.android.robotium.remotecontrol.Condition;
import com.jayway.android.robotium.remotecontrol.ObjectCollection;
import com.jayway.android.robotium.remotecontrol.client.RobotiumTestCase;
import com.jayway.android.robotium.remotecontrol.client.RobotiumTestRunner;
import com.jayway.android.robotium.remotecontrol.client.SoloMessage;
import com.jayway.android.robotium.remotecontrol.solo.Message;
import com.jayway.android.robotium.solo.By;
import com.jayway.android.robotium.solo.RCSolo;
import com.jayway.android.robotium.solo.RobotiumUtils;
import com.jayway.android.robotium.solo.Solo;
import com.jayway.android.robotium.solo.Timeout;
import com.jayway.android.robotium.solo.WebElement;

/** 
 * This class is used to process the "remote command" from the robotium-remote-control<br>
 * 
 * The input parameters are in a Properties object, and the results will be put into<br>
 * that properties object, which will be sent back to robotium-remote-control<br>
 * They will be in key-value format in the properties object.<br>
 * 
 * The key is defined as constant in {@link SoloMessage}<br>
 * Refer to the following page to know what key to get and what key to set for each "command"<br>
 * http://safsdev.sourceforge.net/doc/com/jayway/android/robotium/remotecontrol/solo/Solo.html<br>
 * 
 * <p>
 * For example:<br>
 * For command "assertCurrentActivityClass", at the Returns part, you can see<br>
 *  (in ):KEY_TARGET=target_solo<br>
 *  (in ):KEY_COMMAND=cmd_assertcurrentactivityclass<br>
 *  (out):KEY_ISREMOTERESULT=true<br>
 *  (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=SoloMessage.STATUS_REMOTERESULT_OK<br>
 *  .....<br>
 * When it marks a 'in', you should get; while it marks a 'out', you should set it with a result<br>
 * </p>
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  Feb 16, 2012<br>
 * 		   May 17, 2013		(SBJLWA)	Update to support Robotium 4.1<br>
 * 		   May 23, 2013		(SBJLWA)	Modify method takeScreenshot()<br>
 * 		   Jun 21, 2013		(SBJLWA)	Update to support Robotium 4.1+<br>
 * 		   Jun 25, 2013		(CANAGL)	Update to support Robotium 4.2<br>
 * 		   Jul 18, 2013		(SBJLWA)	Modify method getViewByName(): try to get view under package 'android'.<br>
 */
public class SoloProcessor extends AbstractProcessor implements CacheReferenceInterface{
	public static String TAG = SoloProcessor.class.getSimpleName();
	private static boolean DEBUG = true;
	
	/**
	 * The main {@link Activity} object.
	 */
	Activity mainApp = null;
	
	/**
	 * The instance of {@link com.jayway.android.robotium.solo.RCSolo}, it is used<br>
	 * to do the real work for handling the messages from 'solo remote control'<br>
	 */
	RCSolo solo = null;

	RobotiumTestCase activityrunner = null;
	RobotiumTestRunner robotiumTestrunner = null;
	
	/**
	 * local cache for containing the {@link ActivityMonitor}
	 * <b>Note:</b> Don't manipulate it directly like activityMonitorCache.get(key) etc.<br>
	 * Use the cache-manipulation-methods defined in {@link AbstractProcessor}<br>
	 * @see AbstractProcessor#getCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#removeCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#putCachedItem(Hashtable, Object, Object)
	 */
	protected Hashtable<String,ActivityMonitor> activityMonitorCache = new Hashtable<String,ActivityMonitor>(INITIAL_CACHE_SIZE);

	/**
	 * local cache for containing the {@link Activity}
	 * <b>Note:</b> Don't manipulate it directly like activityCache.get(key) etc.<br>
	 * Use the cache-manipulation-methods defined in {@link AbstractProcessor}<br>
	 * @see AbstractProcessor#getCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#removeCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#putCachedItem(Hashtable, Object, Object)
	 */
	protected Hashtable<String,Activity> activityCache = new Hashtable<String,Activity>(INITIAL_CACHE_SIZE);
	
	/**
	 * local cache for containing the {@link View}
	 * <b>Note:</b> Don't manipulate it directly like viewCache.get(key) etc.<br>
	 * Use the cache-manipulation-methods defined in {@link AbstractProcessor}<br>
	 * @see AbstractProcessor#getCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#removeCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#putCachedItem(Hashtable, Object, Object)
	 */
	protected Hashtable<String,View> viewCache = new Hashtable<String,View>(INITIAL_CACHE_SIZE);

	/**
	 * local cache for containing the {@link WebElement}
	 * <b>Note:</b> Don't manipulate it directly like webElementCache.get(key) etc.<br>
	 * Use the cache-manipulation-methods defined in {@link AbstractProcessor}<br>
	 * @see AbstractProcessor#getCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#removeCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#putCachedItem(Hashtable, Object, Object)
	 */
	protected Hashtable<String,WebElement> webElementCache = new Hashtable<String,WebElement>(INITIAL_CACHE_SIZE);
	
	public SoloProcessor(RobotiumTestRunner robotiumTestrunner){
		super(robotiumTestrunner);
		this.robotiumTestrunner = robotiumTestrunner;
		activityrunner = robotiumTestrunner.getActivityrunner();
	}
	
	/**
	 * Test if the solo is null. If it is, try to set it with that of {@link RobotiumTestRunner}<br>
	 * 
	 * @see RobotiumTestRunner#getSolo()
	 */
	protected boolean checkSolo(){
		if(solo!=null){
			return true;
		}else{
			solo = robotiumTestrunner.getSolo();
		}

		return (solo!=null);
	}
	
	/**
	 * Before calling this method, you should call setRemoteCommand()
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * 
	 */
	public void processProperties(Properties props){
		String debugPrefix = TAG + ".processProperties() ";
		
		if(remoteCommand==null){
			debug(debugPrefix +"remoteCommand is null, this is a programming error. Call SoloProcessor.setRemoteCommand(command)");
			//Try to get the command from properties.
			remoteCommand = props.getProperty(SoloMessage.KEY_COMMAND);
			if(remoteCommand==null){
				setGeneralError(props, SoloMessage.RESULT_INFO_COMMAND_ISNULL);
				return;
			}
		}

		debug(debugPrefix +"Begin processing '"+remoteCommand+"' ... ");

		try{
			if(remoteCommand.equals(SoloMessage.cmd_startmainlauncher)){
				//This will initialize the solo object of RobotiumTestRunner
				startMainLauncher(props);
			}else{
				debug(debugPrefix +"For command different from '"+SoloMessage.cmd_startmainlauncher+"', we need to check the Solo object.");
				if(!checkSolo()){
					debug(debugPrefix +"The robotium solo object is null, you need to call Solo.launchApplication().");
					setGeneralError(props, SoloMessage.RESULT_INFO_SOLO_ISNULL);
					return;
				}
				
				if(remoteCommand.equals(SoloMessage.cmd_assertcurrentactivityname) ||
						remoteCommand.equals(SoloMessage.cmd_assertnewcurrentactivityname) ||
						remoteCommand.equals(SoloMessage.cmd_assertcurrentactivityclass) ||
						remoteCommand.equals(SoloMessage.cmd_assertnewcurrentactivityclass)){
					assertCurrentActivity(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_assertmemorynotlow)){
					assertMemoryNotLow(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonscreen) ||
						 remoteCommand.equals(SoloMessage.cmd_clickonscreenntimes)){
					clickOnScreen(props, false);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clicklongonscreen) ||
						remoteCommand.equals(SoloMessage.cmd_clicklongtimeonscreen)){
					clickOnScreen(props, true);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonactionbarhomebutton)){
					clickOnActionBarHomeButton(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonactionbaritem)){
					clickOnActionBarItem(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonbutton) ||
						remoteCommand.equals(SoloMessage.cmd_clickontogglebutton)){
					clickOnViewByName(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickoncheckboxindex) ||
						remoteCommand.equals(SoloMessage.cmd_clickonedittextindex) ||
						remoteCommand.equals(SoloMessage.cmd_clickonimage) ||
						remoteCommand.equals(SoloMessage.cmd_clickonimagebutton) ||
						remoteCommand.equals(SoloMessage.cmd_clickonbuttonindex) ||
						remoteCommand.equals(SoloMessage.cmd_clickonradiobuttonindex)){
					clickOnViewByIndex(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickinlist) ||
						remoteCommand.equals(SoloMessage.cmd_clickinlistindex)){
					clickInList(props, false);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clicklonginlist) ||
						remoteCommand.equals(SoloMessage.cmd_clicklonginlistindex) ||
						remoteCommand.equals(SoloMessage.cmd_clicklongtimeinlistindex)){
					clickInList(props, true);	
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonmenuitem)){
					clickOnMenuItem(props, false);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonsubmenuitem)){
					clickOnMenuItem(props, true);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonview) ||
						 remoteCommand.equals(SoloMessage.cmd_clickonviewimmediately)){
					clickOnView(props, false);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clicklongonview) ||
						remoteCommand.equals(SoloMessage.cmd_clicklongtimeonview)){
					clickOnView(props, true);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickontext) ||
						remoteCommand.equals(SoloMessage.cmd_clickontextmatch) ||
						remoteCommand.equals(SoloMessage.cmd_clickontextmatchscroll) ){
					clickOnText(props, false);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clicklongontext) ||
						remoteCommand.equals(SoloMessage.cmd_clicklongontextmatch) ||
						remoteCommand.equals(SoloMessage.cmd_clicklongontextmatchscroll) ||
						remoteCommand.equals(SoloMessage.cmd_clicklongtimeontextmatch) ||
						remoteCommand.equals(SoloMessage.cmd_clicklongpressontext)){
					clickOnText(props, true);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clearedittextindex)||
						remoteCommand.equals(SoloMessage.cmd_clearedittextreference)){
					clearEditText(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_drag)){
					drag(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_entertextindex) ||
						remoteCommand.equals(SoloMessage.cmd_entertextreference) ||
						remoteCommand.equals(SoloMessage.cmd_typetext) ||
						remoteCommand.equals(SoloMessage.cmd_typetextuid)){
					enterText(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_finishopenedactivities)){
					finishOpenedActivities(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_finalizeremotesolo)){
					finalizeRemoteSolo(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_goback)){
					goBack(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_gobacktoactivity)){
					goBackToActivity(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getactivitymonitor)){
					getActivityMonitor(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getallopenactivities)){
					getAllOpenActivities(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentactivity)){
					getCurrentActivity(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getbutton) ||
						remoteCommand.equals(SoloMessage.cmd_getedittext) ||
						remoteCommand.equals(SoloMessage.cmd_getimage) ||
						remoteCommand.equals(SoloMessage.cmd_getimagebutton) ||
						remoteCommand.equals(SoloMessage.cmd_gettext) ||
						remoteCommand.equals(SoloMessage.cmd_getviewclass)){
					getViewByIndex(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getbuttontext) ||
						remoteCommand.equals(SoloMessage.cmd_getbuttonvisible) ||
						remoteCommand.equals(SoloMessage.cmd_getedittexttext) ||
						remoteCommand.equals(SoloMessage.cmd_getedittextvisible) ||
						remoteCommand.equals(SoloMessage.cmd_gettexttext) ||
						remoteCommand.equals(SoloMessage.cmd_gettextvisible)){
					getViewByText(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getviewid)){
					getViewById(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getviewbyname) ||
						 remoteCommand.equals(SoloMessage.cmd_getviewbynamematch)){
					getViewByName(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getparentviews)){
					getViewsInParent(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentbuttons) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentcheckboxes) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentdatepickers) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentedittexts) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentgridviews) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentimagebuttons) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentimageviews) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentlistviews) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentprogressbars) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentradiobuttons) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentscrollviews) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentslidingdrawers) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentspinners) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrenttextviews) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrenttimepickers) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrenttogglebuttons) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentviews) ||
//						remoteCommand.equals(SoloMessage.cmd_getcurrentnumberpickers) ||
						remoteCommand.equals(SoloMessage.cmd_getviews) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentviewsbyclass) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentviewsbyclassandparent)){
					getCurrentViews(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getstring)){
					getString(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_gettopparent)){
					getTopParent(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_ischeckboxchecked) ||
						remoteCommand.equals(SoloMessage.cmd_isradiobuttonchecked) ||
						remoteCommand.equals(SoloMessage.cmd_isspinnertextselectedindex) ||
						remoteCommand.equals(SoloMessage.cmd_istogglebuttonchecked)){
					isViewByIndexChecked(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_ischeckboxcheckedtext) ||
						remoteCommand.equals(SoloMessage.cmd_isradiobuttoncheckedtext) ||
						remoteCommand.equals(SoloMessage.cmd_isspinnertextselected) ||
						remoteCommand.equals(SoloMessage.cmd_istextchecked) ||
						remoteCommand.equals(SoloMessage.cmd_istogglebuttoncheckedtext)){
					isViewByTextChecked(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_pressmenuitem) ||
						remoteCommand.equals(SoloMessage.cmd_presssubmenuitem)){
					pressMenuItem(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_pressspinneritem)){
					pressSpinnerItem(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_scrolldown) ||
						remoteCommand.equals(SoloMessage.cmd_scrollup) ||
						remoteCommand.equals(SoloMessage.cmd_scrolltotop) ||
						remoteCommand.equals(SoloMessage.cmd_scrolltobottom) ||
						remoteCommand.equals(SoloMessage.cmd_scrolllisttotop) ||
						remoteCommand.equals(SoloMessage.cmd_scrolllisttotopuid) ||
						remoteCommand.equals(SoloMessage.cmd_scrolllisttobottom) ||
						remoteCommand.equals(SoloMessage.cmd_scrolllisttobottomuid) ||
						remoteCommand.equals(SoloMessage.cmd_scrolllisttoline) ||
						remoteCommand.equals(SoloMessage.cmd_scrolllisttolineuid) ||
						remoteCommand.equals(SoloMessage.cmd_scrolldownlist) ||
						remoteCommand.equals(SoloMessage.cmd_scrolldownlistuid) ||
						remoteCommand.equals(SoloMessage.cmd_scrolluplistuid) ||
						remoteCommand.equals(SoloMessage.cmd_scrolluplist)){
					scroll(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_scrolltoside) ||
						remoteCommand.equals(SoloMessage.cmd_scrollviewtoside)){
					scrollToSide(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_searchbutton) ||
						remoteCommand.equals(SoloMessage.cmd_searchbuttonvisible) ||
						remoteCommand.equals(SoloMessage.cmd_searchbuttonmatch) ||
						remoteCommand.equals(SoloMessage.cmd_searchbuttonmatchvisible) ||
						remoteCommand.equals(SoloMessage.cmd_searchedittext) ||
						remoteCommand.equals(SoloMessage.cmd_searchtext) ||
						remoteCommand.equals(SoloMessage.cmd_searchtextvisible) ||
						remoteCommand.equals(SoloMessage.cmd_searchtextmatch) ||
						remoteCommand.equals(SoloMessage.cmd_searchtextmatchscroll) ||
						remoteCommand.equals(SoloMessage.cmd_searchtextmatchscrollvisible) ||
						remoteCommand.equals(SoloMessage.cmd_searchtogglebutton) ||
						remoteCommand.equals(SoloMessage.cmd_searchtogglebuttonmatch)){
					searchView(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_setactivityorientation)){
					setActivityOrientation(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_setdatepickerreference) ||
						remoteCommand.equals(SoloMessage.cmd_setdatepickerindex)){
					setDatePicker(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_settimepickerreference) ||
						remoteCommand.equals(SoloMessage.cmd_settimepickerindex)){
					setTimePicker(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_setprogressbarreference) ||
						remoteCommand.equals(SoloMessage.cmd_setprogressbarindex)){
					setProgressBar(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_setslidingdrawerreference) ||
						remoteCommand.equals(SoloMessage.cmd_setslidingdrawerindex)){
					setSlidingDrawer(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_sendkey)){
					sendKey(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_sleep)){
					sleep(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_startscreenshotsequencemax)){
					startScreenshotSequenceMax(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getscreenshotsequence) ||
						 remoteCommand.equals(SoloMessage.cmd_getscreenshotsequenceindex) ||
						 remoteCommand.equals(SoloMessage.cmd_getscreenshotsequenceszie)){
					getScreenshotSequence(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforactivity) ||
						remoteCommand.equals(SoloMessage.cmd_waitforactivitytimeout) ||
						remoteCommand.equals(SoloMessage.cmd_waitforactivitybyclass) ||
						remoteCommand.equals(SoloMessage.cmd_waitforactivitybyclasstimeout)){
					waitForActivity(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforfragmentbytag)){
					waitForFragmentByTag(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforfragmentbyid)){
					waitForFragmentById(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitfordialogtoclose) ||
						 remoteCommand.equals(SoloMessage.cmd_waitfordialogtoopen)){
					waitForDialog(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforlogmessage)){
					waitForLogMessage(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitfortext) ||
						remoteCommand.equals(SoloMessage.cmd_waitfortextmatchtimeout) ||
						remoteCommand.equals(SoloMessage.cmd_waitfortextmatchtimeoutscroll) ||
						remoteCommand.equals(SoloMessage.cmd_waitfortextmatchtimeoutscrollvisible)){
					waitForText(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforviewclass) ||
						remoteCommand.equals(SoloMessage.cmd_waitforviewclassmatchtimeout) ||
						remoteCommand.equals(SoloMessage.cmd_waitforviewclassmatchtimeoutscroll)){
					waitForView(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforviewreference) ||
						remoteCommand.equals(SoloMessage.cmd_waitforviewreferencetimeoutscroll)){
					waitForViewUID(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforviewid) ||
						 remoteCommand.equals(SoloMessage.cmd_waitforviewidtimeout) ||
						 remoteCommand.equals(SoloMessage.cmd_waitforviewidtimeoutscroll)){
					waitForViewByID(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getscreensize) ){
					getScreenSize(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getviewlocation) ){
					getViewLocation(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_gettextviewvalue) ){
					getTextViewValue(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getguiimage) ){
					getGuiImage(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getviewclassname)){
					getObjectClassName(props, true);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getobjectclassname)){
					getObjectClassName(props, false);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_takescreenshot) ||
						 remoteCommand.equals(SoloMessage.cmd_takescreenshotquality)){
					takeScreenshot(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforcondition) ){
					waitForCondition(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_cleartextinwebelement) ){
					clearTextInWebElement(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonwebelement) ||
						remoteCommand.equals(SoloMessage.cmd_clickonwebelementindex) ||
						remoteCommand.equals(SoloMessage.cmd_clickonwebelementindexscroll)){
					clickOnWebElement(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clickonwebelementuid) ){
					clickOnWebElementByUID(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_entertextinwebelement) ||
						 remoteCommand.equals(SoloMessage.cmd_typetextinwebelement) ||
						 remoteCommand.equals(SoloMessage.cmd_typetextinwebelementindex)){
					enterTextInWebElement(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_typetextinwebelementuid) ){
					typeTextInWebElementByUID(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentwebelements) ||
						remoteCommand.equals(SoloMessage.cmd_getcurrentwebelementsby) ||
						remoteCommand.equals(SoloMessage.cmd_getwebelement)){
					getCurrentWebElements(props);
				}else if(remoteCommand.equals(SoloMessage.cmd_getweburl) ){
					setGeneralSuccessWithSpecialInfo(props, solo.getWebUrl());
					
				}else if(remoteCommand.equals(SoloMessage.cmd_hidesoftkeyboard) ){
					solo.hideSoftKeyboard();
					setGeneralSuccess(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_waitforwebelement) ||
						remoteCommand.equals(SoloMessage.cmd_waitforwebelementtimeout) ||
						remoteCommand.equals(SoloMessage.cmd_waitforwebelementminmatchtimeout)){
					waitForWebElement(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_utilsfilterviews) ||
						remoteCommand.equals(SoloMessage.cmd_utilsfilterviewsbytext) ||
						remoteCommand.equals(SoloMessage.cmd_utilsfilterviewstoset) ||
						remoteCommand.equals(SoloMessage.cmd_utilsgetnumberofmatches) ||
						remoteCommand.equals(SoloMessage.cmd_utilsremoveinvisibleviews) ||
						remoteCommand.equals(SoloMessage.cmd_utilssortviewsbylocationonscreen) ||
						remoteCommand.equals(SoloMessage.cmd_utilssortviewsbylocationonscreenyfirst)){
					handleRobotiumUtilsCommand(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_setlargetimeout) ||
						 remoteCommand.equals(SoloMessage.cmd_setsmalltimeout) ||
						 remoteCommand.equals(SoloMessage.cmd_getlargetimeout) ||
						 remoteCommand.equals(SoloMessage.cmd_getsmalltimeout)){
					handleRobotiumTimeoutCommand(props);
				
				}else if(remoteCommand.equals(SoloMessage.cmd_pinchtozoom) ||
						remoteCommand.equals(SoloMessage.cmd_rotatelarge) ||
						remoteCommand.equals(SoloMessage.cmd_rotatesmall) ||
						remoteCommand.equals(SoloMessage.cmd_swipe)){
					handleZoomRotateSwipe(props);
					
				}else if(remoteCommand.equals(SoloMessage.cmd_clearlog) ||
						 remoteCommand.equals(SoloMessage.cmd_stopscreenshotsequence)){
					handleComandWithoutParams(props);					
				}
				else{
					debug(debugPrefix +"Unknown command '"+remoteCommand+"'.");
					// "unkown/not executed" result already set.
				}
			}
			
		}catch(ProcessorException pe){
			debug(remoteCommand+": Met ProcessorException: '"+pe.getMessage()+"'.");
			setGeneralError(props, SoloMessage.RESULT_INFO_PROCESSOR_EXCEPTION, pe.getMessage());
		}catch(ComparisonFailure cf){
			debug(remoteCommand+": Met ComparisonFailure: '"+cf.getMessage()+"'.");
			setGeneralError(props, SoloMessage.RESULT_INFO_COMPARAISON_FAIL, cf.getMessage());
		}catch(AssertionFailedError afe){
			debug(remoteCommand+": Met AssertionFailedError: '"+afe.getMessage()+"'.");
			setGeneralError(props, SoloMessage.RESULT_INFO_ASSERTION_FAIL, afe.getMessage());
		}catch(Throwable e){
			debug(remoteCommand+": Met Throwable: '"+e.getMessage()+"'. This maybe a program error!!!");
			setGeneralError(props, SoloMessage.RESULT_INFO_EXCEPTION, e.getMessage());
		}
		
		debug(debugPrefix +"Finish processing '"+remoteCommand+"'.");
	}
	
	/**
	 * Set success result for test.<br>
	 * Set 'generated UID' to {@link SoloMessage#KEY_REMOTERESULTINFO} for remote-caller's reference<br>
	 * Set the view's class name to {@link SoloMessage#PARAM_CLASS} for remote-caller's reference<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 * @param view		View, the View object
	 * @return
	 * @see #convertToKey(Hashtable, Object)
	 * @see #setViewClassResult(Properties, View)
	 * @see #setGeneralSuccessWithSpecialInfo(Properties, String)
	 */
	private void setSuccessResultForView(Properties props, View view) throws ProcessorException{
		String debugPrefix = TAG+".getViewByText() ";
		
		//Get the unique key for view from cache
		String uid = convertToKey(viewCache, view);
		if(uid==null){
			debug(debugPrefix+" Can NOT generate UID for view "+ view.getClass().getSimpleName());
			throw new ProcessorException(SoloMessage.RESULT_INFO_GENERATE_UID_NULL);	
		}else{
			debug(debugPrefix+" get uid '"+uid+"' for view "+view.getClass().getSimpleName());
		}
		
		//we will set the PARAM_CLASS of the view for remote caller's reference
		setViewClassResult(props, view);
		
		setGeneralSuccessWithSpecialInfo(props, uid);
	}
	
	/**
	 * Set the view's class name to {@link SoloMessage#PARAM_CLASS} for remote-caller's reference<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 * @param view		View, the View object whose class name be returned by {@link SoloMessage#PARAM_CLASS}
	 * @return
	 * @see #setSuccessResultForView(Properties, View)
	 */
	private void setViewClassResult(Properties props, View view) throws ProcessorException{
		String debugPrefix = TAG + ".setViewClassResult() ";
		
		if(view==null){
			throw new ProcessorException("The parameter view is null.");
		}
		
		try{
			props.setProperty(SoloMessage.PARAM_CLASS, view.getClass().getName());
		}catch(Exception e){
			debug(debugPrefix+" Fail to set class name. Exception="+e.getMessage());
		}
		
	}
	
	/**
	 * Set the activity's class name and the activity's name for remote-caller's reference<br>
	 * These two properties will be returned as part of result.<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 * @return          Activity, the current activity got by Solo
	 */
	private Activity setCurrentActivityClassName(Properties props) throws ProcessorException{
		String debugPrefix = TAG + ".setCurrentActivityClassName() ";
		Activity activity = solo.getCurrentActivity();
		
		if(activity!=null){
			debug(debugPrefix +"set activity's name and classname to the returned Properties.");
			try{
				props.setProperty(SoloMessage.PARAM_CLASS, activity.getClass().getName());
				props.put(SoloMessage.PARAM_NAME, getActivityName(activity));
			}catch(Exception e){
				//setProperty() may throw NullPointerException if the value to be set is null
				debug(debugPrefix +" met Exception="+e.getMessage());				
			}
		}else{
			debug(debugPrefix +"There is no current activity, can't set the activity's classname!");
			throw new ProcessorException("The current activity is null.");
		}
		
		return activity;
	}
	
	/**
	 * Get the activity name.<br>
	 * It should be the activity's class name without the package name.<br>
	 */
	private String getActivityName(Activity activity){
		if(activity ==null){
			throw new NullPointerException();
		}
//		ComponentName component = activity.getComponentName();
//		String simpleClsName = component.getShortClassName();
//		if(simpleClsName.startsWith(".")){
//			simpleClsName = simpleClsName.substring(1);
//		}

		String simpleClsName = activity.getClass().getSimpleName();
		
		return simpleClsName;
	}
	
	/**
	 * This method will launch the main activity.<br>
	 * It will initialize the {@link Solo} object of {@link RobotiumTestRunner}<br>
	 * This Solo object will also be shared by this class<br>
	 * If you want to use the Solo object, you MUST call this method firstly.<br> 
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @see RobotiumTestRunner#launchApplication()
	 */
	protected void startMainLauncher(Properties props) throws ProcessorException{
		String debugPrefix = TAG + ".startMainLauncher() ";

		robotiumTestrunner.launchApplication();
		//Before using the field solo, need to set it firstly
		if(!checkSolo()){
			debug(debugPrefix+" Begin start main launcher ... solo=null");
			throw new ProcessorException("Can't get Solo, it is null! Error");
		}
		
		mainApp = setCurrentActivityClassName(props);

		setGeneralSuccess(props);
	}
	
	/**
	 * assert the current activity has the given name/class, with the possibility to verify that<br>
	 * the expected Activity is a new instance of the Activity.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#assertCurrentActivity(String, Class)}<br>
	 * {@link Solo#assertCurrentActivity(String, Class, boolean)<br>}<br>
	 * {@link Solo#assertCurrentActivity(String, String))}<br>
	 * {@link Solo#assertCurrentActivity(String, String, boolean))}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	@SuppressWarnings("rawtypes")
	void assertCurrentActivity(Properties props) throws ProcessorException{
		String debugPrefix = TAG + ".assertCurrentActivity() ";
		String assertFailMsg = props.getProperty(SoloMessage.PARAM_ERRORMSG);
		String activityClassString = null;
		String activityName = null;
		String resultInfo = "";

		Class expectedClass = null;
		boolean assertWithClass = false;
		
		//Get the parameter name or class
		if (remoteCommand.equals(SoloMessage.cmd_assertnewcurrentactivityname) ||
			remoteCommand.equals(SoloMessage.cmd_assertcurrentactivityname)) {
			activityName = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
			debug(debugPrefix +" activity name is '"+activityName+"'");
			resultInfo = activityName;
			
		} else if (remoteCommand.equals(SoloMessage.cmd_assertnewcurrentactivityclass) ||
				   remoteCommand.equals(SoloMessage.cmd_assertcurrentactivityclass)) {
			activityClassString = SoloMessage.getString(props, SoloMessage.PARAM_CLASS);
			debug(debugPrefix +" activity class name is '"+activityClassString+"'");
			resultInfo = activityClassString;
			assertWithClass = true;
			
		} else {
			throw new ProcessorException(remoteCommand + " could not be processed in assertCurrentActivity().");
		}
		
		setCurrentActivityClassName(props);
		
		//Try to get the Class object for a class name.
		if(assertWithClass){
			try {
				// it is possible an Activity Class might not be available to us for Class.forName.
				// While a normal Solo test might actually have a reference to a real running 
				// Activity from which it can grab the Class, we may not have this.
				// In that case, we may want to accept that we can instead grab hold of the current 
				// running Activity and getClass().getName() and compare that against our String 
				// classname instead of the call to assertCurrentActivity.  
				expectedClass = Class.forName(activityClassString);
				debug(debugPrefix +" got Class for '"+activityClassString+"'");

			} catch (ClassNotFoundException cnfe) {
				assertWithClass = false;
				activityName = SoloMessage.getSimpleClassName(activityClassString);
				if(activityName==null){
					debug(debugPrefix +" can't get the activity name from class name '"+activityClassString+"' !");
					throw new ProcessorException("Activity name is null.");
				}
				debug(debugPrefix +" Class '"+activityClassString+"' can't be found. Try to assert with class name '"+activityName+"'");
			}
		}

		//Use Solo to do the real work.
		if(remoteCommand.equals(SoloMessage.cmd_assertcurrentactivityclass) ||
		   remoteCommand.equals(SoloMessage.cmd_assertcurrentactivityname)){
			//If the name/class can't match, ComparisonFailure will be thrown
			//ComparisonFailure will be caught in processProperties()
			if(assertWithClass){
				solo.assertCurrentActivity(assertFailMsg, expectedClass);
			}else{
				solo.assertCurrentActivity(assertFailMsg, activityName);
			}
			setGeneralSuccess(props, resultInfo);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_assertnewcurrentactivityclass) ||
				 remoteCommand.equals(SoloMessage.cmd_assertnewcurrentactivityname)){
			boolean isNewInstance = SoloMessage.getBoolean(props, SoloMessage.PARAM_ISNEWINSTANCE);
			
			try{
				//If the name/class can't match, ComparisonFailure will be thrown
				//If the isNewInstance can't match, AssertionFailedError will be thrown
				//we must catch them here, as we need to set the returned value for SoloMessage.PARAM_ISNEWINSTANCE
				if(assertWithClass){
					solo.assertCurrentActivity(assertFailMsg, expectedClass, isNewInstance);
				}else{
					solo.assertCurrentActivity(assertFailMsg, activityName, isNewInstance);
				}
				setGeneralSuccess(props, resultInfo);
				
			}catch (ComparisonFailure e) {
				//If we get ComparisonFailure, which means that activity's name can't match
				setGeneralError(props, SoloMessage.RESULT_INFO_COMPARAISON_FAIL, e.getMessage());
			}catch (AssertionFailedError e) {
				//If we get AssertionFailedError, which means that 'isNewInstance' can't match
				//Set PARAM_ISNEWINSTANCE, and it contains the 'isNewInstance' of actual activity,
				//which is the opposite value of the given parameter 'isNewInstance'
				String isnew = String.valueOf(!isNewInstance);
				props.setProperty(SoloMessage.PARAM_ISNEWINSTANCE, isnew);
				setGeneralError(props, SoloMessage.RESULT_INFO_ASSERTION_FAIL, e.getMessage());				
			}
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in assertCurrentActivityClass().");
		}

	}

	/**
	 * assert that the memory is enough<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#assertMemoryNotLow()}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void assertMemoryNotLow(Properties props){
		try{
			//AssertionFailedError will be thrown out, if the memory is low
			solo.assertMemoryNotLow();
			setGeneralSuccessWithSpecialInfo(props, String.valueOf(true));
		}catch(AssertionFailedError e){
			debug(SoloMessage.RESULT_INFO_MEMORY_ISLOW+" Met Exception="+e.getMessage());
			setGeneralSuccessWithSpecialInfo(props, String.valueOf(false));
		}
	}
	
	/**
	 * clear the text in {@link android.widget.EditText}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#clearEditText(EditText)}<br>
	 * {@link Solo#clearEditText(int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void clearEditText(Properties props) throws ProcessorException{
		
		if(remoteCommand.equals(SoloMessage.cmd_clearedittextindex)){
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			//If editbox can't be edited, an AssertionFailedError will be thrown out
			//this exception will be handled in method processProperties()
			debug("Params: index="+index);
			solo.clearEditText(index);
			setGeneralSuccess(props, String.valueOf(index));
		}else if(remoteCommand.equals(SoloMessage.cmd_clearedittextreference)){
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			debug("Params: uid="+uid);
			EditText editText = (EditText)getViewById(uid, EditText.class);
			//If editbox can't be edited, an AssertionFailedError will be thrown out
			//this exception will be handled in method processProperties()
			solo.clearEditText(editText);				
			setGeneralSuccess(props, uid);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in clearEditText().");
		}
	}
	
	/**
	 * Handle the commands without parameters<br>
	 * 
	 * <p>
	 * calling:
	 * <p>
	 * {@link Solo#clearLog()} <b>Requires Robotium4.1+</b><br>
	 * {@link Solo#stopScreenshotSequence()} <b>Requires Robotium4.2</b><br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void handleComandWithoutParams(Properties props){

		try{
			if(remoteCommand.equals(SoloMessage.cmd_clearlog)){
				solo.clearLog();
				
			}else if(remoteCommand.equals(SoloMessage.cmd_stopscreenshotsequence)){
				solo.stopScreenshotSequence();				
			}else {
				throw new ProcessorException(remoteCommand+" could not be processed in handleComandWithoutParams().");
			}
			
			setGeneralSuccess(props);
			
		}catch(Throwable x){
			debug("handleComandWithoutParams() "+SoloMessage.getStackTrace(x));
			setGeneralError(props, x.getClass().getSimpleName()+": "+ x.getMessage());
		}
	}
	
	/**
	 * click an item in android.widget.ListView<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickInList(int)}<br>
	 * {@link Solo#clickInList(int, int)}<br>
	 * {@link Solo#clickLongInList(int)}<br>
	 * {@link Solo#clickLongInList(int, int)}<br>
	 * {@link Solo#clickLongInList(int, int, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 * @param longtime	Boolean, if a long-time click should be performed.
	 */
	void clickInList(Properties props, boolean longtime) throws ProcessorException{
		int line = 0;
		int index = 0;
		List<TextView> viewList = null;
		
		line = SoloMessage.getInteger(props, SoloMessage.PARAM_LINE);
		debug("Params: longtime="+longtime+"; line="+line);
		if(remoteCommand.equals(SoloMessage.cmd_clickinlist) ||
		   remoteCommand.equals(SoloMessage.cmd_clicklonginlist)){
			//If there is no ListView, an AssertionFailedError will be thrown out
			if(longtime){
				viewList = solo.clickLongInList(line);
			}else{
				viewList = solo.clickInList(line);				
			}
		}else if(remoteCommand.equals(SoloMessage.cmd_clickinlistindex) ||
				 remoteCommand.equals(SoloMessage.cmd_clicklonginlistindex)){
			index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug("Params: index="+index);
			//If there is no ListView, an AssertionFailedError will be thrown out
			if(longtime){
				viewList = solo.clickLongInList(line, index);
			}else{
				viewList = solo.clickInList(line, index);				
			}
		}else if(remoteCommand.equals(SoloMessage.cmd_clicklongtimeinlistindex)){
			index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			//If there is no ListView, an AssertionFailedError will be thrown out
			int time = SoloMessage.getInteger(props, SoloMessage.PARAM_TIME);
			debug("Params: index="+index+"; time="+time);
			viewList = solo.clickLongInList(line, index, time);
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in clickInList().");
		}

		//Store the text value to 'out parameter' SoloMessage.PARAM_TEXT
		if(viewList!=null){
			String[] textArray = new String[viewList.size()];
			for(int i=0;i<viewList.size();i++){
				textArray[i] = viewList.get(i).getText().toString();
			}
			props.put(SoloMessage.PARAM_TEXT, Message.convertToDelimitedString(textArray) );			
		}else{
			//Normally, the List returned from solo#clickInList() and solo#clickLongInList()
			//will not return a null. You should never come here.
			debug("The returned textViewList is null.");
		}
		
		String[] items = convertToKeys(viewCache, viewList);
		setGeneralSuccessWithSpecialInfo(props, Message.convertToDelimitedString(items));
	}
	
	/**
	 * click some point on screen<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnScreen(float, float)}<br>
	 * {@link Solo#clickLongOnScreen(float, float)}<br>
	 * {@link Solo#clickLongOnScreen(float, float, int)}<br>
	 * {@link Solo#clickOnScreen(float, float, int)} <b>Requires Robotium4.1+</b><br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 * @param longtime	Boolean, if a long-time click should be performed.
	 */
	void clickOnScreen(Properties props, boolean longtime) throws ProcessorException{
		float x = SoloMessage.getFloat(props, SoloMessage.PARAM_FLOATX);
		float y = SoloMessage.getFloat(props, SoloMessage.PARAM_FLOATY);
		
		debug("Params: longtime="+longtime+"; x="+x+"; y="+y);
		if(longtime){
			if(remoteCommand.equals(SoloMessage.cmd_clicklongonscreen)){
				solo.clickLongOnScreen(x, y);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clicklongtimeonscreen)){
				int time = SoloMessage.getInteger(props, SoloMessage.PARAM_TIME);//milliseconds
				debug("Params: time="+time);
				solo.clickLongOnScreen(x, y, time);
				
			}else{
				throw new ProcessorException(remoteCommand+" could not be processed in clickOnScreen().");
			}
			
		}else{
			if(remoteCommand.equals(SoloMessage.cmd_clickonscreen)){
				solo.clickOnScreen(x, y);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clickonscreenntimes)){
				int clicknumbers = SoloMessage.getInteger(props, SoloMessage.PARAM_CLICKNUMBER);
				solo.clickOnScreen(x, y, clicknumbers);
				
			}else{
				throw new ProcessorException(remoteCommand+" could not be processed in clickOnScreen().");
			}
		}
		
		setGeneralSuccess(props, "At point ("+x+","+y+")");
	}

	/**
	 * Click on the ActionBar Home Button.<br>
	 * Requires Robotium 3.4.1
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnActionBarHomeButton()}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void clickOnActionBarHomeButton(Properties props){
		try{ solo.clickOnActionBarHomeButton();}
		catch(Throwable x){
			String msg = x.getClass().getSimpleName()+": "+ x.getMessage();
			debug("clickOnActionBarHomeButton "+msg);
			setGeneralError(props, msg);
			return;
		}
		setGeneralSuccess(props);
	}
	
	/**
	 * Clicks on an ActionBar item with a given resource id.<br>
	 * Requires Robotium 3.6
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnActionBarItem(int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void clickOnActionBarItem(Properties props) throws ProcessorException{
		int resourceid = SoloMessage.getInteger(props, SoloMessage.PARAM_RESID);
		
		try{ solo.clickOnActionBarItem(resourceid);}
		catch(Throwable x){
			String msg = x.getClass().getSimpleName()+": "+ x.getMessage();
			debug("clickOnActionBarItem "+msg);
			setGeneralError(props, msg);
			return;
		}
		setGeneralSuccess(props);
	}
	
	/**
	 * click on a string within the current<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnText(String)}<br>
	 * {@link Solo#clickOnText(String, int)}<br>
	 * {@link Solo#clickOnText(String, int, boolean)}<br>
	 * {@link Solo#clickLongOnText(String)}<br>
	 * {@link Solo#clickLongOnText(String, int)}<br>
	 * {@link Solo#clickLongOnText(String, int, boolean)}<br>
	 * {@link Solo#clickLongOnText(String, int, int)}<br>
	 * {@link Solo#clickLongOnTextAndPress(String, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 * @param longtime	Boolean, if a long-time click should be performed.
	 */
	void clickOnText(Properties props, boolean longtime) throws ProcessorException{
		String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
		boolean scroll = false;
		int match = 0;
		int time = 0;
			
		debug("Params: longtime="+longtime+"; text="+text);
		if(longtime){
			if(remoteCommand.equals(SoloMessage.cmd_clicklongontext)){
				solo.clickLongOnText(text);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clicklongontextmatch)){
				match =  SoloMessage.getInteger(props, SoloMessage.PARAM_MATCH);
				debug("Params: match="+match);
				solo.clickLongOnText(text, match);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clicklongontextmatchscroll)){
				match =  SoloMessage.getInteger(props, SoloMessage.PARAM_MATCH);
				scroll = Boolean.parseBoolean(props.getProperty(SoloMessage.PARAM_SCROLL));
				debug("Params: match="+match+"; scroll="+scroll);
				solo.clickLongOnText(text, match, scroll);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clicklongtimeontextmatch)){
				match =  SoloMessage.getInteger(props, SoloMessage.PARAM_MATCH);
				time = SoloMessage.getInteger(props, SoloMessage.PARAM_TIME);
				debug("Params: match="+match+"; time="+time);
				solo.clickLongOnText(text, match, time);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clicklongpressontext)){
				int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
				debug("Params: index="+index);
				solo.clickLongOnTextAndPress(text, index);
				
			}else{
				throw new ProcessorException(remoteCommand+" could not be processed in clickOnText().");
			}
		}else{
			if(remoteCommand.equals(SoloMessage.cmd_clickontext)){
				solo.clickOnText(text);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clickontextmatch)){
				match =  SoloMessage.getInteger(props, SoloMessage.PARAM_MATCH);
				debug("Params: match="+match);
				solo.clickOnText(text, match);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clickontextmatchscroll)){
				match =  SoloMessage.getInteger(props, SoloMessage.PARAM_MATCH);
				scroll = Boolean.parseBoolean(props.getProperty(SoloMessage.PARAM_SCROLL));
				debug("Params: match="+match+"; scroll="+scroll);
				solo.clickOnText(text, match, scroll);
				
			}else{
				throw new ProcessorException(remoteCommand+" could not be processed in clickOnText().");
			}
		}
		
		setGeneralSuccess(props, text);
	}
	
	/**
	 * click on a {@link android.view.View}<br>
	 * The view's id is given by parameter, according to this id,<br>
	 * We will find the View from local cache {@link #viewCache} and click on it.<br>
	 * This method has no relationship with method {@link #clickOnViewByIndex(Properties)}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnView(View)}<br>
	 * {@link Solo#clickOnView(View, boolean)} -- Robotium 4.1<br>
	 * {@link Solo#clickLongOnView(View)}<br>
	 * {@link Solo#clickLongOnView(View, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 * @param longtime	Boolean, if a long-time click should be performed.
	 * @see Solo#clickOnView(View)
	 */
	void clickOnView(Properties props, boolean longtime) throws ProcessorException{
		String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
		View view = null;
		try{
			view = (View)getCachedObject(uid, true);
		}catch(ClassCastException x){
			throw new ProcessorException(remoteCommand+" retrieved object was not an instanceof View!");
		}
		
		debug("Params: longtime="+longtime+"; uid="+uid);
		if(longtime){
			if(remoteCommand.equals(SoloMessage.cmd_clicklongonview)){
				solo.clickLongOnView(view);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clicklongtimeonview)){
				int time = SoloMessage.getInteger(props, SoloMessage.PARAM_TIME);
				debug("Params: time="+time);
				solo.clickLongOnView(view, time);
				
			}else{
				throw new ProcessorException(remoteCommand+" could not be processed in clickOnView().");
			}
		}else{
			if(remoteCommand.equals(SoloMessage.cmd_clickonview)){
				solo.clickOnView(view);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_clickonviewimmediately)){
				boolean immediately = SoloMessage.getBoolean(props, SoloMessage.PARAM_IMMEDIATELY);
				solo.clickOnView(view, immediately);
				
			}else{
				throw new ProcessorException(remoteCommand+" could not be processed in clickOnView().");
			}
		}
		
		if(DEBUG){
			debug("Clicked on view '"+view.getClass()+"'");
		}
		
		setViewClassResult(props, view);
		setGeneralSuccess(props, uid+" : "+view.getClass().getSimpleName());
	}
	
	/**
	 * click on the following {@link android.widget.TextView} by name<br>
	 * {@link android.widget.Button}<br>
	 * {@link android.widget.ToggleButton}<br>
	 * 
	 * The name is given by parameter, according to that name<br>
	 * Solo will click on the appropriate TextView.<br>
	 * This method has no relationship with method {@link #clickOnView(Properties, boolean)}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnButton(String)}<br>
	 * {@link Solo#clickOnToggleButton(String)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void clickOnViewByName(Properties props) throws ProcessorException{
		String name = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
		
		debug("Params: name="+name);
		if(remoteCommand.equals(SoloMessage.cmd_clickonbutton)){
			solo.clickOnButton(name);
		}else if(remoteCommand.equals(SoloMessage.cmd_clickontogglebutton)){
			solo.clickOnToggleButton(name);
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in clickOnViewByName().");			
		}

		setGeneralSuccess(props, name);
	}
	
	/**
	 * click on the following {@link android.widget.TextView} by index<br>
	 * {@link android.widget.CheckBox}<br>
	 * {@link android.widget.EditText}<br>
	 * {@link android.widget.ImageView}<br>
	 * {@link android.widget.ImageButton}<br>
	 * {@link android.widget.RadioButton}<br>
	 * {@link android.widget.Button}<br>
	 * 
	 * The index is given by parameter, according to that index<br>
	 * Solo will click on the appropriate TextView.<br>
	 * This method has no relationship with method {@link #clickOnView(Properties, boolean)}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnCheckBox(int)}<br>
	 * {@link Solo#clickOnEditText(int)}<br>
	 * {@link Solo#clickOnImage(int)}<br>
	 * {@link Solo#clickOnImageButton(int)}<br>
	 * {@link Solo#clickOnRadioButton(int)}<br>
	 * {@link Solo#clickOnButton(int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void clickOnViewByIndex(Properties props) throws ProcessorException{
		int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
		
		debug("Params: index="+index);
		if(remoteCommand.equals(SoloMessage.cmd_clickoncheckboxindex)){
			solo.clickOnCheckBox(index);
		}else if(remoteCommand.equals(SoloMessage.cmd_clickonedittextindex)){
			solo.clickOnEditText(index);			
		}else if(remoteCommand.equals(SoloMessage.cmd_clickonimage)){
			solo.clickOnImage(index);
		}else if(remoteCommand.equals(SoloMessage.cmd_clickonimagebutton)){
			solo.clickOnImageButton(index);
		}else if(remoteCommand.equals(SoloMessage.cmd_clickonradiobuttonindex)){
			solo.clickOnRadioButton(index);
		}else if(remoteCommand.equals(SoloMessage.cmd_clickonbuttonindex)){
			solo.clickOnButton(index);
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in clickOnViewByIndex().");			
		}

		setGeneralSuccess(props, String.valueOf(index));
		
	}

	/**
	 * click on a 'menu item' indicated by text<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnMenuItem(String)}<br>
	 * {@link Solo#clickOnMenuItem(String, boolean)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void clickOnMenuItem(Properties props, boolean subMenu) throws ProcessorException{
		String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
		String resultInfo = text;
		
		debug("Params: text="+text);
		if(subMenu){
			solo.clickOnMenuItem(text, subMenu);
			resultInfo = "Sub Menu: "+resultInfo;
		}else{
			solo.clickOnMenuItem(text);
			resultInfo = "Menu: "+resultInfo;
		}
		
		setGeneralSuccess(props, resultInfo);
	}

	/**
	 * Simulate touching a given location and dragging it to a new location.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#drag(float, float, float, float, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void drag(Properties props) throws ProcessorException{
		float fromX = SoloMessage.getFloat(props, SoloMessage.PARAM_FROMX);
		float fromY = SoloMessage.getFloat(props, SoloMessage.PARAM_FROMY);
		float toX = SoloMessage.getFloat(props, SoloMessage.PARAM_TOX);
		float toY = SoloMessage.getFloat(props, SoloMessage.PARAM_TOY);
		int stepCount = SoloMessage.getInteger(props, SoloMessage.PARAM_STEPCOUNT);
		String resultInfo = " From("+fromX+","+fromY+") To("+toX+","+toY+") ";
			
		debug("Params: stepCount="+stepCount+"; "+resultInfo);
		solo.drag(fromX, toX, fromY, toY, stepCount);
		
		setGeneralSuccess(props, resultInfo);
	}
	
	/**
	 * input the text in {@link android.widget.EditText}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#enterText(int, String)}<br>
	 * {@link Solo#enterText(EditText, String)}<br>
	 * {@link Solo#typeText(int, String)} -- Robotium 3.6<br>
	 * {@link Solo#typeText(EditText, String)} -- Robotium 3.6<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void enterText(Properties props) throws ProcessorException{
		String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
		
		debug("Params: text="+text);
		if(remoteCommand.equals(SoloMessage.cmd_entertextindex)){
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug("Params: index="+index);
			solo.enterText(index, text);
			setGeneralSuccess(props, " index="+index+" : text='"+text+"' ");
			
		}else if(remoteCommand.equals(SoloMessage.cmd_entertextreference)){
			String id = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			debug("Params: id="+id);
			EditText editText = (EditText)getViewById(id, EditText.class);
			solo.enterText(editText, text);				
			setGeneralSuccess(props, String.valueOf(id));
			
		}else if(remoteCommand.equals(SoloMessage.cmd_typetext)){
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug("Params: index="+index);
			solo.typeText(index, text);
			setGeneralSuccess(props, " index="+index+" : text='"+text+"' ");
			
		}else if(remoteCommand.equals(SoloMessage.cmd_typetextuid)){
			String id = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			debug("Params: id="+id);
			EditText editText = (EditText)getViewById(id, EditText.class);
			solo.typeText(editText, text);				
			setGeneralSuccess(props, String.valueOf(id));
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in clearEditText().");
		}
	}
	
	/**
	 * Finalizes the solo object and removes the ActivityMonitor.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#finalize()}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void finalizeRemoteSolo(Properties props){
		String debugPrefix = TAG + ".finalizeRemoteSolo() ";
		try {
			solo.finalize();
			mainApp = null;
			clearCache(false);
			activityMonitorCache.clear();
			setGeneralSuccess(props);
		} catch (Throwable e) {
			String error = "Met error: "+e.getMessage(); 
			debug(debugPrefix+error);
			setGeneralError(props,error);
		}
	}
	
	/**
	 * All activities that have been active are finished.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#finishOpenedActivities()}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void finishOpenedActivities(Properties props){
		solo.finishOpenedActivities();
		mainApp = null;
		clearCache(false);
		setGeneralSuccess(props);
	}
	
	/**
	 * Returns a String UID for the Robotium Solo Activity Monitor<br>
	 * Use a cache to store the ActivityMonitor.<br>
	 * If the ActivityMonitor doesn't exist in the cache, we generate an ID<br>
	 * and put the ActivityMonitor into cache with that ID.<br>
	 * If the ActivityMonitor exists in the cache, we just return the related ID.<br> 
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getActivityMonitor()}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getActivityMonitor(Properties props) throws ProcessorException{
		String debugPrefix = TAG + ".getActivityMonitor() ";
		ActivityMonitor am = solo.getActivityMonitor();
		String activityMonitorID = "";
		
		if(am==null){
			throw new ProcessorException(SoloMessage.RESULT_INFO_ACTIVITYMONITOR_NULL);
		}
		
		activityMonitorID = convertToKey(activityMonitorCache, am);
		if(activityMonitorID==null){
			debug(debugPrefix +" Could NOT generate ID for Activity Monitor!");
			throw new ProcessorException(SoloMessage.RESULT_INFO_GENERATE_UID_NULL);			
		}else{
			debug(debugPrefix +" get UID '"+activityMonitorID+"' for Activity Monitor");			
		}
		
		setGeneralSuccessWithSpecialInfo(props, activityMonitorID);
	}
	
	/**
	 * get all activities and return their ID in format ";ID;ID;ID"<br>
	 * the ID string will be stored in remoteresultinfo of parameter props.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getAllOpenedActivities()}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getAllOpenActivities(Properties props) throws ProcessorException{
		String debugPrefix = TAG + ".getAllOpenActivities() ";
		List<Activity> activities = solo.getAllOpenedActivities();
		
		if(activities==null || activities.size()==0){
			debug(debugPrefix+" Can't get any activities!");
			throw new ProcessorException("Can't get any activities!");
		}
		
		if(DEBUG){			
			String[] activityNames = new String[activities.size()];
			for(int i=0;i<activities.size();i++){
				activityNames[i] = getActivityName(activities.get(i));
			}
			debug("current activites: "+Message.convertToDelimitedString(activityNames));
		}
		
		String[] uids = convertToKeys(activityCache, activities.toArray());
		setGeneralSuccessWithSpecialInfo(props,Message.convertToDelimitedString(uids));
	}
	

	/**
	 * get the current activity and return its ID<br>
	 * the ID string will be stored in remoteresultinfo of parameter props.<br>
	 * <p>
	 * Upon success the resultProperties should contain:
	 * <p>
	 * PARAM_CLASS=full Classname of the Activity, and <br>
	 * PARAM_NAME=Local (short) classname of the Activity.
	 * <p>
	 * calling:<br>
	 * {@link Solo#getCurrentActivity()}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */	
	void getCurrentActivity(Properties props) throws ProcessorException{
		String debugPrefix = TAG + ".getCurrentActivity() ";
		Activity activity = setCurrentActivityClassName(props);
		
		String uid = convertToKey(activityCache, activity);
		if(uid==null){
			debug(debugPrefix+" Can NOT generate UID for activity "+activity.getLocalClassName());
			throw new ProcessorException(SoloMessage.RESULT_INFO_GENERATE_UID_NULL);	
		}else{
			debug(debugPrefix +" get UID '"+uid+"' for activity "+activity.getLocalClassName());
		}
		
		setGeneralSuccessWithSpecialInfo(props, uid);
	}
	
	/**
	 * get all the {@link android.view.View}s of following type:<br>
	 * {@link android.widget.Button}<br>
	 * {@link android.widget.CheckBox}<br>
	 * {@link android.widget.DatePicker}<br>
	 * {@link android.widget.EditText}<br>
	 * {@link android.widget.GridView}<br>
	 * {@link android.widget.ImageButton}<br>
	 * {@link android.widget.ImageView}<br>
	 * {@link android.widget.ListView}<br>
	 * {@link android.widget.ProgressBar}<br>
	 * {@link android.widget.RadioButton}<br>
	 * {@link android.widget.ScrollView}<br>
	 * {@link android.widget.SlidingDrawer}<br>
	 * {@link android.widget.Spinner}<br>
	 * {@link android.widget.TextView}<br>
	 * {@link android.widget.TimePicker}<br>
	 * {@link android.widget.ToggleButton}<br>
	 * {@link android.view.View}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getViews()} - Robotium 4.1<br>
	 * {@link Solo#getCurrentViews()} - Robotium 4.1<br>
	 * {@link Solo#getCurrentViews(Class)} - Robotium 4.1<br>
	 * {@link Solo#getCurrentViews(Class, View)} - Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getCurrentViews(Properties props) throws ProcessorException{
		List<?> currentViewList = null;
		
		if(remoteCommand.equals(SoloMessage.cmd_getcurrentbuttons)){
			currentViewList = solo.getCurrentViews(Button.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentcheckboxes)){
			currentViewList = solo.getCurrentViews(CheckBox.class);

		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentdatepickers)){
			currentViewList = solo.getCurrentViews(DatePicker.class);
		
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentedittexts)){
			currentViewList = solo.getCurrentViews(EditText.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentgridviews)){
			currentViewList = solo.getCurrentViews(GridView.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentimagebuttons)){
			currentViewList = solo.getCurrentViews(ImageButton.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentimageviews)){
			currentViewList = solo.getCurrentViews(ImageView.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentlistviews)){
			currentViewList = solo.getCurrentViews(ListView.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentprogressbars)){
			currentViewList = solo.getCurrentViews(ProgressBar.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentradiobuttons)){
			currentViewList = solo.getCurrentViews(RadioButton.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentscrollviews)){
			currentViewList = solo.getCurrentViews(ScrollView.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentslidingdrawers)){
			currentViewList = solo.getCurrentViews(SlidingDrawer.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentspinners)){
			currentViewList = solo.getCurrentViews(Spinner.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrenttextviews)){
			currentViewList = solo.getCurrentViews(TextView.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrenttimepickers)){
			currentViewList = solo.getCurrentViews(TimePicker.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrenttogglebuttons)){
			currentViewList = solo.getCurrentViews(ToggleButton.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentviews)){
			currentViewList = solo.getCurrentViews();
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getviews)){
			currentViewList = solo.getViews();
			
//		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentnumberpickers)){
//			currentViewList = solo.getCurrentViews(NumberPicker.class);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentviewsbyclass)){
			String className = SoloMessage.getString(props, SoloMessage.PARAM_CLASS);
			Class<View> viewClass = ClassViewForName(className);
			if(viewClass==null){
				throw new ProcessorException(remoteCommand+": '"+className+"' could not be found.");
			}
			currentViewList = solo.getCurrentViews(viewClass);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentviewsbyclassandparent)){
			String className = SoloMessage.getString(props, SoloMessage.PARAM_CLASS);
			String parentUID = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);

			Class<View> viewClass = ClassViewForName(className);
			if(viewClass==null){
				throw new ProcessorException(remoteCommand+": '"+className+"' could not be found.");
			}
			View parent = null;
			try{
				parent = (View) getCachedObject(parentUID, true);
			}catch(Exception e){
				throw new ProcessorException(remoteCommand+": could not find view from cache by UID '"+parentUID+"'.");
			}
			currentViewList = solo.getCurrentViews(viewClass, parent);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in getCurrentViews().");			
		}
		
		String[] items = convertToKeys(viewCache, currentViewList);
		setGeneralSuccessWithSpecialInfo(props, Message.convertToDelimitedString(items));
	}

	/**
	 * get the current WebElements and return its ID<br>
	 * the ID string will be stored in remoteresultinfo of parameter props.<br>
	 * <p>
	 * calling:<br>
	 * {@link Solo#getCurrentWebElements()} -- Robotium 4.1<br>
	 * {@link Solo#getCurrentWebElements(By)} -- Robotium 4.1<br>
	 * {@link Solo#getWebElement(By, int)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */	
	void getCurrentWebElements(Properties props) throws ProcessorException{
		List<WebElement> list = null;
		
		if(remoteCommand.equals(SoloMessage.cmd_getcurrentwebelements)){
			list = solo.getCurrentWebElements();
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getcurrentwebelementsby)){
			String objectStr = SoloMessage.getString(props, SoloMessage.PARAM_OBJECT);
			By by = SoloMessage.getSoloBy((com.jayway.android.robotium.remotecontrol.By) SoloMessage.decodeBase64Object(objectStr));
			list = solo.getCurrentWebElements(by);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getwebelement)){
			String objectStr = SoloMessage.getString(props, SoloMessage.PARAM_OBJECT);
			By by = SoloMessage.getSoloBy((com.jayway.android.robotium.remotecontrol.By) SoloMessage.decodeBase64Object(objectStr));
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			list = new ArrayList<WebElement>();
			try{
				list.add(solo.getWebElement(by, index));
			}catch(Throwable t){
				String debugmsg = "can't get WebElement by by="+by+"; index="+index+" due to "+Message.getStackTrace(t);
				debug(debugmsg);
				throw new ProcessorException(remoteCommand+debugmsg);			
			}
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in getCurrentWebElements().");			
		}
		
		String[] items = convertToKeys(webElementCache, list);
		setGeneralSuccessWithSpecialInfo(props, Message.convertToDelimitedString(items));
	}
	
	/**
	 * get the {@link android.view.View} by id (R.id)<br>
	 * 
	 * The id is given by parameter, according to that id<br>
	 * Solo will get the appropriate View.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link #getViewById(int, Class)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getViewById(Properties props) throws ProcessorException{
		String debugPrefix = TAG+".getViewById(Properties) ";
		int id = SoloMessage.getInteger(props, SoloMessage.PARAM_ID);

		debug(debugPrefix+" Try to get view for id '"+id+"'");
		//getViewById() will never return a null
		View view = getViewById(id, null);

		setSuccessResultForView(props, view);
		
	}

	/**
	 * get the {@link android.view.View} by resource idname. <b>Requires Robotium4.1+</b><br>
	 * The resource idname is stored in AUT layout xml file. <br>
	 * For example, in xml a view is given id as "@+id/flipper", "flipper" is the idname.<br>
	 * 
	 * The idname is given by parameter, according to that name, Solo will get the appropriate View.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getView(String)}<br>
	 * {@link Solo#getView(String, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getViewByName(Properties props) throws ProcessorException{
		String debugPrefix = TAG+".getViewByName(Properties) ";
		String idname = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
		View view = null;
		
		debug(debugPrefix+" Try to get view for resource name '"+idname+"'");
		if(remoteCommand.equals(SoloMessage.cmd_getviewbyname)){
			view = solo.getView(idname);
			if(view==null && !isFullResouceName(idname)){
				String androidResName = getAndroidResouceName(idname, "id");
				debug("Try to get view for resource name '"+androidResName+"'");
				view = solo.getView(androidResName);
			}
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getviewbynamematch)){
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			view = solo.getView(idname, index);
			if(view==null && !isFullResouceName(idname)){
				String androidResName = getAndroidResouceName(idname, "id");
				debug("Try to get view for resource name '"+androidResName+"'");
				view = solo.getView(androidResName, index);
			}
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in getViewByName().");			
		}
		
		if(view==null) throw new ProcessorException(remoteCommand+" could not get a view for resource name '"+idname+"'.");
		
		setSuccessResultForView(props, view);
	}
	
	/**
	 * The resource name contains 3 parts: "package", "type" and "entry", it has format as "package:type/entry".
	 * If a "resource name" contains both separators ":" and "/", we consider it as a full name.
	 * @param resourceName, String, the resource name
	 * @return boolean, if the resource name is fully qualified.
	 */
	private boolean isFullResouceName(String resourceName) throws ProcessorException{
		if(resourceName==null) throw new ProcessorException("The resourceName is null.");
		return (resourceName.contains(":") && resourceName.contains("/"));
	}
	/**
	 * According to the entryName and type, generate a resource name of package "android".
	 * @param entryName, String, the entry name of the resource.
	 * @param type, String, the type of the resource.
	 * @return String, the full resource name of package "android"
	 */
	private String getAndroidResouceName(String entryName, String type) throws ProcessorException{
		String androidResourceName = entryName;
		if(!isFullResouceName(entryName)){
			androidResourceName = "android:"+type+"/"+entryName;
		}
		return androidResourceName;
	}
	
	/**
	 * get the following {@link android.widget.TextView} by index<br>
	 * {@link android.widget.Button}<br>
	 * {@link android.widget.EditText}<br>
	 * {@link android.widget.TextView}<br>
	 * {@link android.widget.ImageView}<br>
	 * {@link android.widget.ImageButton}<br>
	 * 
	 * The index is given by parameter, according to that index<br>
	 * Solo will get the appropriate TextView.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getButton(int)}<br>
	 * {@link Solo#getEditText(int)}<br>
	 * {@link Solo#getImage(int)}<br>
	 * {@link Solo#getImageButton(int)}<br>
	 * {@link Solo#getText(int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getViewByIndex(Properties props) throws ProcessorException{
		int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
		View view = null;
		
		debug("Params: index="+index);
		if(remoteCommand.equals(SoloMessage.cmd_getbutton)){
			view = solo.getButton(index);

		}else if(remoteCommand.equals(SoloMessage.cmd_getedittext)){
			view = solo.getEditText(index);
		
		}else if(remoteCommand.equals(SoloMessage.cmd_getimage)){
			view = solo.getImage(index);
		
		}else if(remoteCommand.equals(SoloMessage.cmd_getimagebutton)){
			view = solo.getImageButton(index);

		}else if(remoteCommand.equals(SoloMessage.cmd_gettext)){
			view = solo.getText(index);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getviewclass)){
			String className = SoloMessage.getString(props, SoloMessage.PARAM_CLASS);
			debug("Params: className="+className);
			//TODO risk not find the class, But there is no solo.getView(index) to use!!!
			Class<View> viewClass = ClassViewForName(className);
			if(viewClass==null){
				//TODO Need new Implementation
				throw new ProcessorException(remoteCommand+": '"+className+"' could not be found.");
			}	

			view = solo.getView(viewClass, index);

		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in getViewByIndex().");			
		}

		//AssertionFailedError will be thrown if view can't be found, no need to check if view is null
		setSuccessResultForView(props, view);
		
	}
	
	/**
	 * get the following {@link android.widget.TextView} by text<br>
	 * {@link android.widget.Button}<br>
	 * {@link android.widget.EditText}<br>
	 * {@link android.widget.TextView}<br>
	 * 
	 * 
	 * The text is given by parameter, according to that text<br>
	 * Solo will get the appropriate TextView.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getButton(String)}<br>
	 * {@link Solo#getButton(String, boolean)}<br>
	 * {@link Solo#getEditBox(String)}<br>
	 * {@link Solo#getEditBox(String, boolean)}<br>
	 * {@link Solo#getText(String)}<br>
	 * {@link Solo#getText(String, boolean)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getViewByText(Properties props) throws ProcessorException{
		String debugPrefix = TAG+".getViewByText() ";
		String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
		boolean onlyVisible = false;
		View view = null;
		
		debug(debugPrefix+" Try to get view according to text '"+text+"'");
		
		if(remoteCommand.equals(SoloMessage.cmd_getbuttontext)){
			view = solo.getButton(text);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getbuttonvisible)){
			onlyVisible = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
			debug("Params: onlyVisible="+onlyVisible);
			view = solo.getButton(text, onlyVisible);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getedittexttext)){
			view = solo.getEditText(text);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_getedittextvisible)){
			onlyVisible = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
			debug("Params: onlyVisible="+onlyVisible);
			view = solo.getEditText(text, onlyVisible);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_gettexttext)){
			view = solo.getText(text);
			
			if(DEBUG){
				debug("Got TextView '"+((TextView)view).getText().toString()+"'");
			}
			
		}else if(remoteCommand.equals(SoloMessage.cmd_gettextvisible)){
			onlyVisible = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
			debug("Params: onlyVisible="+onlyVisible);
			view = solo.getText(text, onlyVisible);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in getViewByText().");			
		}

		//AssertionFailedError will be thrown if view can't be found, no need to check if view is null
		setSuccessResultForView(props, view);
	
	}
	
	/**
	 * Get all the views belonging to a parent view. The parent view is given <br>
	 * by the parameter {@link SoloMessage#PARAM_REFERENCE}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getViews(View)}<br>
	 */
	void getViewsInParent(Properties props) throws ProcessorException{
		String debugPrefix = TAG +".getViewsInParent(): ";
		List<View> views = null;
		
		if(remoteCommand.equals(SoloMessage.cmd_getparentviews)){
			String pid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			debug("Params: pid="+pid);
			View parent = getViewById(pid, null);
			if(parent==null){
				debug(debugPrefix +"Can't find parent view by id '"+pid+"', program will return all the current views.");
			}
			views = solo.getViews(parent);
			if(views!=null && views.size()>0){
				debug(debugPrefix+" Got "+views.size()+" items in "+parent.getClass().getSimpleName());
			}else{
				debug(debugPrefix+" Didn't get any item in "+parent.getClass().getSimpleName());
			}
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in getViewsInParent().");			
		}
		
		String[] items = convertToKeys(viewCache, views);
		setGeneralSuccessWithSpecialInfo(props, Message.convertToDelimitedString(items));
	}
	
	/**
	 * Returns a localized String from localized String resources, according to 'resource id'<br>
	 * given by {@link SoloMessage#PARAM_RESID}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getString(int)}<br>
	 */
	void getString(Properties props) throws ProcessorException{
		String debugPrefix = TAG +".getString(): ";
		int resId = SoloMessage.getInteger(props, SoloMessage.PARAM_RESID);
		
		debug("Params: resId="+resId);
		String resourceStr = "";
		try{
			resourceStr = solo.getString(resId);
		}catch(NotFoundException nfe){
			debug(debugPrefix +" Can't find resource string for id '"+resId+"'. Exception: "+nfe.getMessage());
			resourceStr = "null";
		}
		
		setGeneralSuccessWithSpecialInfo(props, resourceStr);
	}
	
	/**
	 * Get the top parent view of the given view, identified by the parameter {@link SoloMessage#PARAM_REFERENCE}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#getView(int)}<br>
	 * {@link Solo#getTopParent(View)}<br>
	 */
	void getTopParent(Properties props) throws ProcessorException{
		String debugPrefix = TAG + ".getTopParent() ";
		String id = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
		
		debug(debugPrefix+" Try to get view of id '"+id+"'");
		View view = getViewById(id, null);

		debug(debugPrefix+" Try to get top parent for view "+view.getClass().getSimpleName());
		View topparent = solo.getTopParent(view);
		
		//getTopParent() will never return null, ok.
		setSuccessResultForView(props, topparent);
	}
	
	/**
	 * Simulates pressing the hardware back key.
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#goBack()}<br>
	 */
	void goBack(Properties props) throws ProcessorException{

		solo.goBack();

		setGeneralSuccess(props);
	}
	
	/**
	 * Returns to the given Activity.
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#goBackToActivity(String)}<br> 
	 */
	void goBackToActivity(Properties props) throws ProcessorException{
		String name = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
		
		debug("Params: name="+name);
		solo.goBackToActivity(name);
		
		//set the PARAM_CLASS AND PARAM_NAME of the current activity for caller's reference
		setCurrentActivityClassName(props);
		
		setGeneralSuccess(props, "Back to activity '"+name+"'");		
	}
	
	/**
	 * test if the following {@link android.widget.TextView} is checked.<br>
	 * {@link android.widget.CheckBox}<br>
	 * {@link android.widget.RadioButton}<br>
	 * {@link android.widget.Spinner}<br>
	 * {@link android.widget.ToggleButton}<br>
	 * 
	 * The index is given by parameter, according to that index<br>
	 * Solo will get the appropriate View and test if it is checked.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#isCheckBoxChecked(int)}<br>
	 * {@link Solo#isRadioButtonChecked(int)}<br>
	 * {@link Solo#isSpinnerTextSelected(int, String)}<br>
	 * {@link Solo#isToggleButtonChecked(int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void isViewByIndexChecked(Properties props) throws ProcessorException{
		int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
		boolean checked = false;
		
		debug("Params: index="+index);
		if(remoteCommand.equals(SoloMessage.cmd_ischeckboxchecked)){
			checked = solo.isCheckBoxChecked(index);

		}else if(remoteCommand.equals(SoloMessage.cmd_isradiobuttonchecked)){
			checked = solo.isRadioButtonChecked(index);
		
		}else if(remoteCommand.equals(SoloMessage.cmd_isspinnertextselectedindex)){
			String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
			checked = solo.isSpinnerTextSelected(index, text);
		
		}else if(remoteCommand.equals(SoloMessage.cmd_istogglebuttonchecked)){
			checked = solo.isToggleButtonChecked(index);

		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in isViewByIndexChecked().");			
		}
		
		//Bring back the checked value by parameter KEY_REMOTERESULTINFO
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(checked));
	}
	
	/**
	 * test if the following {@link android.widget.TextView} is checked.<br>
	 * {@link android.widget.CheckBox}<br>
	 * {@link android.widget.RadioButton}<br>
	 * {@link android.widget.Spinner}<br>
	 * {@link android.widget.ToggleButton}<br>
	 * 
	 * The text is given by parameter, according to that text<br>
	 * Solo will get the appropriate View and test if it is checked.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#isCheckBoxChecked(String)}<br>
	 * {@link Solo#isRadioButtonChecked(String)}<br>
	 * {@link Solo#isSpinnerTextSelected(String)}<br>
	 * {@link Solo#isTextChecked(String)}<br>
	 * {@link Solo#isToggleButtonChecked(String)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void isViewByTextChecked(Properties props) throws ProcessorException{
		String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
		boolean checked = false;
		
		debug("Params: text="+text);
		if(remoteCommand.equals(SoloMessage.cmd_ischeckboxcheckedtext)){
			checked = solo.isCheckBoxChecked(text);

		}else if(remoteCommand.equals(SoloMessage.cmd_isradiobuttoncheckedtext)){
			checked = solo.isRadioButtonChecked(text);
		
		}else if(remoteCommand.equals(SoloMessage.cmd_isspinnertextselected)){
			checked = solo.isSpinnerTextSelected(text);
		
		}else if(remoteCommand.equals(SoloMessage.cmd_istextchecked)){
			checked = solo.isTextChecked(text);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_istogglebuttoncheckedtext)){
			checked = solo.isToggleButtonChecked(text);

		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in isViewByTextChecked().");			
		}
		
		//Bring back the checked value by parameter KEY_REMOTERESULTINFO
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(checked));
	}
	
	/**
	 * press an item in {@link android.widget.Menu}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#pressMenuItem(int)}<br>
	 * {@link Solo#pressMenuItem(int, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void pressMenuItem(Properties props) throws ProcessorException{
		int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
		String resultinfo = "";
		
		debug("Params: index="+index);
		if(remoteCommand.equals(SoloMessage.cmd_pressmenuitem)){
			solo.pressMenuItem(index);
			resultinfo = "index="+index;
			
		}else if(remoteCommand.equals(SoloMessage.cmd_presssubmenuitem)){
			int itemsPerRow = SoloMessage.getInteger(props, SoloMessage.PARAM_ITEMSPERROW);
			debug("Params: itemsPerRow="+itemsPerRow);
			solo.pressMenuItem(index, itemsPerRow);
			resultinfo = "index="+index+" : itemsPerRow"+ itemsPerRow;

		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in pressMenuItem().");			
		}
		
		setGeneralSuccess(props,resultinfo);
	}
	
	/**
	 * press an item in {@link android.widget.Spinner}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#pressSpinnerItem(int, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void pressSpinnerItem(Properties props) throws ProcessorException{
		int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
		int itemIndex = SoloMessage.getInteger(props, SoloMessage.PARAM_ITEMINDEX);
		
		debug("Params: index="+index+"; itemIndex="+itemIndex);
		solo.pressSpinnerItem(index, itemIndex);
		setGeneralSuccess(props, "index="+index+" : itemIndex="+itemIndex);
	}
	
	/**
	 * scroll vertically.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#scrollDown()}<br>
	 * {@link Solo#scrollDownList(int)}<br>
	 * {@link Solo#scrollDownList(AbsListView)} -- Robotium 3.6<br>
	 * {@link Solo#scrollUp()}<br>
	 * {@link Solo#scrollToTop()} -- Robotium 3.4.1<br>
	 * {@link Solo#scrollToBottom()} -- Robotium 3.4.1<br>
	 * {@link Solo#scrollUpList(int)}<br>
	 * {@link Solo#scrollUpList(AbsListView)} -- Robotium 3.6<br>
	 * {@link Solo#scrollListToTop(int)} -- Robotium 3.4.1<br>
	 * {@link Solo#scrollListToTop(AbsListView)} -- Robotium 3.6<br>
	 * {@link Solo#scrollListToBottom(int)} -- Robotium 3.4.1<br>
	 * {@link Solo#scrollListToBottom(AbsListView)} -- Robotium 3.6<br>
	 * {@link Solo#scrollListToLine(AbsListView, int)} -- Robotium 3.6<br>
	 * {@link Solo#scrollListToLine(int, int)} -- Robotium 3.6<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void scroll(Properties props) throws ProcessorException{
		int index = 0;
		String uid = "";
		int line = 0;
		boolean canBeScrolled = false;
		String debugPrefix = TAG +".scroll(): ";
		
		if(remoteCommand.equals(SoloMessage.cmd_scrolldown)){
			canBeScrolled = solo.scrollDown();
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolldownlist)){
			index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug(debugPrefix +" Params: index="+index);
			canBeScrolled = solo.scrollDownList(index);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolldownlistuid)){
			uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			AbsListView listView = (AbsListView) getViewById(uid, AbsListView.class);
			debug(debugPrefix +" Params: reference="+uid+"; Got ListView="+listView);
			canBeScrolled = solo.scrollDownList(listView);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrollup)){
			canBeScrolled = solo.scrollUp();
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolltotop)){
			solo.scrollToTop();
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolltobottom)){
			solo.scrollToBottom();
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolluplist)){
			index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug(debugPrefix +" Params: index="+index);
			canBeScrolled = solo.scrollUpList(index);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolluplistuid)){
			uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			AbsListView listView = (AbsListView) getViewById(uid, AbsListView.class);
			debug(debugPrefix +" Params: reference="+uid+"; Got ListView="+listView);
			canBeScrolled = solo.scrollUpList(listView);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolllisttotop)){
			index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug(debugPrefix +" Params: index="+index);
			canBeScrolled = solo.scrollListToTop(index);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolllisttotopuid)){
			uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			AbsListView listView = (AbsListView) getViewById(uid, AbsListView.class);
			debug(debugPrefix +" Params: reference="+uid+"; Got ListView="+listView);
			canBeScrolled = solo.scrollListToTop(listView);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolllisttobottom)){
			index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug(debugPrefix +" Params: index="+index);
			canBeScrolled = solo.scrollListToBottom(index);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolllisttobottomuid)){
			uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			AbsListView listView = (AbsListView) getViewById(uid, AbsListView.class);
			debug(debugPrefix +" Params: reference="+uid+"; Got ListView="+listView);
			canBeScrolled = solo.scrollListToBottom(listView);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolllisttoline)){
			index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			line = SoloMessage.getInteger(props, SoloMessage.PARAM_LINE);
			debug(debugPrefix +" Params: index="+index+" line="+line);
			solo.scrollListToLine(index, line);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrolllisttolineuid)){
			uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			line = SoloMessage.getInteger(props, SoloMessage.PARAM_LINE);
			debug(debugPrefix +" Params: line="+line);
			AbsListView listView = (AbsListView) getViewById(uid, AbsListView.class);
			debug(debugPrefix +" Params: reference="+uid+"; Got ListView="+listView);
			solo.scrollListToLine(listView, line);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in scroll().");			
		}
		
		if(!canBeScrolled){
			debug(debugPrefix +" Reach the end, can't be scrolled no more.");
		}
		
		//Bring back the canBeScrolled value by parameter KEY_REMOTERESULTINFO
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(canBeScrolled));
	}
	
	/**
	 * scroll horizontally.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#scrollToSide(int)}<br>
	 * {@link Solo#scrollViewToSide(View, int)} -- Robotium 3.6<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void scrollToSide(Properties props) throws ProcessorException{
		int side = SoloMessage.getInteger(props, SoloMessage.PARAM_SIDE);
		String resultinfo = "Scroll to left.";
		debug("Params: side="+side);
		if(remoteCommand.equals(SoloMessage.cmd_scrolltoside)){
			solo.scrollToSide(side);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_scrollviewtoside)){
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			View view = getViewById(uid, null);
			debug(" Params: reference="+uid+"; Got View="+view);
			solo.scrollViewToSide(view, side);
		}
		
		if(com.jayway.android.robotium.solo.Solo.RIGHT==side){
			resultinfo = "Scroll to right.";
		}
		
		setGeneralSuccess(props,resultinfo);		
	}
	
	/**
	 * search the following view.<br>
	 * {@link android.widget.Button}<br>
	 * {@link android.widget.EditText}<br>
	 * {@link android.widget.TextView}<br>
	 * {@link android.widget.ToggleButton}<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#searchButton(String)}<br>
	 * {@link Solo#searchButton(String, boolean)}<br>
	 * {@link Solo#searchButton(String, int)}<br>
	 * {@link Solo#searchButton(String, int, boolean)}<br>
	 * {@link Solo#searchEditText(String)}<br>
	 * {@link Solo#searchText(String)}<br>
	 * {@link Solo#searchText(String, boolean)}<br>
	 * {@link Solo#searchText(String, int)}<br>
	 * {@link Solo#searchText(String, int, boolean)}<br>
	 * {@link Solo#searchToggleButton(String)}<br>
	 * {@link Solo#searchToggleButton(String, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void searchView(Properties props) throws ProcessorException{
		String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
		boolean onlyVisible = false;
		int minimumNumberOfMatches = 0;
		boolean scrollToFind = false;
		boolean found = false;

		debug("Params: text="+text);
		if(remoteCommand.equals(SoloMessage.cmd_searchbutton)){
			found = solo.searchButton(text);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchbuttonmatch)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			debug("Params: minimumNumberOfMatches="+minimumNumberOfMatches);
			found = solo.searchButton(text, minimumNumberOfMatches);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchbuttonmatchvisible)){
			onlyVisible = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			debug("Params: onlyVisible="+onlyVisible+"; minimumNumberOfMatches="+minimumNumberOfMatches);
			found = solo.searchButton(text, minimumNumberOfMatches, onlyVisible);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchbuttonvisible)){
			onlyVisible = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
			debug("Params: onlyVisible="+onlyVisible);
			found = solo.searchButton(text, onlyVisible);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchedittext)){
			found = solo.searchEditText(text);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchtext)){
			found = solo.searchText(text);			
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchtextmatch)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			debug("Params: minimumNumberOfMatches="+minimumNumberOfMatches);
			found = solo.searchText(text, minimumNumberOfMatches);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchtextmatchscroll)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);			
			scrollToFind = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
			debug("Params: scrollToFind="+scrollToFind+"; minimumNumberOfMatches="+minimumNumberOfMatches);
			found = solo.searchText(text, minimumNumberOfMatches, scrollToFind);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchtextmatchscrollvisible)){
			onlyVisible = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);			
			scrollToFind = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
			debug("Params: scrollToFind="+scrollToFind+"; minimumNumberOfMatches="+minimumNumberOfMatches+"; scrollToFind="+scrollToFind);
			found = solo.searchText(text, minimumNumberOfMatches, scrollToFind, onlyVisible);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchtextvisible)){
			onlyVisible = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
			debug("Params: onlyVisible="+onlyVisible);
			found = solo.searchText(text, onlyVisible);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_searchtogglebutton)){
			found = solo.searchToggleButton(text);
			 
		}else if(remoteCommand.equals(SoloMessage.cmd_searchtogglebuttonmatch)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			debug("Params: minimumNumberOfMatches="+minimumNumberOfMatches);
			found = solo.searchToggleButton(text, minimumNumberOfMatches);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in searchView().");			
		}
		
		debug(remoteCommand+(found? " Found view.":" Not found view."));
		
		//Set the boolean found to KEY_REMOTERESULTINFO
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
		
	}

	/**
	 * send a key to current view.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#sendKey(int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void sendKey(Properties props) throws ProcessorException{
		int key = SoloMessage.getInteger(props, SoloMessage.PARAM_KEY);
		String resultInfo = "Key";
		
		debug("Params: key="+key);
		solo.sendKey(key);
		switch (key) {
		case RCSolo.RIGHT:
			resultInfo += " Right";
			break;
		case RCSolo.LEFT:
			resultInfo += " Left";
			break;
		case RCSolo.UP:
			resultInfo += " Up";
			break;
		case RCSolo.DOWN:
			resultInfo += " Down";
			break;
		case RCSolo.ENTER:
			resultInfo += " Enter";
			break;
		case RCSolo.MENU:
			resultInfo += " Menu";
			break;
		case RCSolo.DELETE:
			resultInfo += " Delete";
			break;
		default:
			resultInfo += " Unkonwn";
			break;
		}
		
		setGeneralSuccess(props, resultInfo);
	}
	
	/**
	 * set orientation of view.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#setActivityOrientation(int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void setActivityOrientation(Properties props) throws ProcessorException{
		int orientation = SoloMessage.getInteger(props, SoloMessage.PARAM_ORIENTATION);
		String resultInfo = " Orientation ";
		
		debug("Params: orientation="+orientation);
		solo.setActivityOrientation(orientation);
		switch(orientation){
		case RCSolo.LANDSCAPE:
			resultInfo += "Landscape";
			break;
		case RCSolo.PORTRAIT:
			resultInfo += "Portrait";
			break;
		default:
			resultInfo += "Unkonwn";
			break;
		}
		
		setGeneralSuccess(props, resultInfo);
	}
	
	/**
	 * set value of {@link android.widget.DatePicker}.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#setDatePicker(DatePicker, int, int, int)}<br>
	 * {@link Solo#setDatePicker(int, int, int, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void setDatePicker(Properties props) throws ProcessorException{
		int year = SoloMessage.getInteger(props, SoloMessage.PARAM_YEAR);
		int monthOfYear = SoloMessage.getInteger(props, SoloMessage.PARAM_YEARMONTH);
		int dayOfMonth = SoloMessage.getInteger(props, SoloMessage.PARAM_MONTHDAY);
		String resultInfo = "date="+year+"/"+monthOfYear+"/"+dayOfMonth;
		
		debug("Params: "+resultInfo);
		if(remoteCommand.equals(SoloMessage.cmd_setdatepickerindex)){
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug("Params: index="+index);
			solo.setDatePicker(index, year, monthOfYear, dayOfMonth);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_setdatepickerreference)){
			String id = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			DatePicker datePicker = (DatePicker) getViewById(id, DatePicker.class);
			debug("Params: id="+id);
			solo.setDatePicker(datePicker, year, monthOfYear, dayOfMonth);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in setDatePicker().");
		}
		
		setGeneralSuccess(props, resultInfo);
	}
	
	/**
	 * set value of {@link android.widget.ProgressBar}.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#setProgressBar(int, int)}<br>
	 * {@link Solo#setProgressBar(ProgressBar, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void setProgressBar(Properties props) throws ProcessorException{
		int progress = SoloMessage.getInteger(props, SoloMessage.PARAM_PROGRESS);
		String resultInfo = " value="+progress;
		
		debug("Params: progress="+progress);
		if(remoteCommand.equals(SoloMessage.cmd_setprogressbarindex)){
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug("Params: index="+index);
			solo.setProgressBar(index, progress);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_setprogressbarreference)){
			String id = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			ProgressBar progressBar = (ProgressBar) getViewById(id, ProgressBar.class);
			debug("Params: id="+id);
			solo.setProgressBar(progressBar, progress);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in setProgressBar().");
		}
		
		setGeneralSuccess(props, resultInfo);
	}
	
	/**
	 * set value of {@link android.widget.SlidingDrawer}.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#setSlidingDrawer(int, int)}<br>
	 * {@link Solo#setSlidingDrawer(android.widget.SlidingDrawer, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void setSlidingDrawer(Properties props) throws ProcessorException{
		int status = SoloMessage.getInteger(props, SoloMessage.PARAM_STATUS);
		String resultInfo = " value="+status;
		
		debug("Params: status="+status);
		if(remoteCommand.equals(SoloMessage.cmd_setslidingdrawerindex)){
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug("Params: index="+index);
			solo.setSlidingDrawer(index, status);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_setslidingdrawerreference)){
			String id = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			SlidingDrawer slidingDrawer = (SlidingDrawer) getViewById(id, SlidingDrawer.class);
			debug("Params: id="+id);
			solo.setSlidingDrawer(slidingDrawer, status);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in setSlidingDrawer().");
		}
		
		setGeneralSuccess(props, resultInfo);
	}
	
	/**
	 * set value of {@link android.widget.TimePicker}.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#setTimePicker(int, int, int)}<br>
	 * {@link Solo#setTimePicker(android.widget.TimePicker, int, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void setTimePicker(Properties props) throws ProcessorException{
		int hour = SoloMessage.getInteger(props, SoloMessage.PARAM_HOUR);
		int minute = SoloMessage.getInteger(props, SoloMessage.PARAM_MINUTE);
		String resultInfo = "time="+hour+":"+minute;
		
		debug("Params: "+resultInfo);
		if(remoteCommand.equals(SoloMessage.cmd_settimepickerindex)){
			int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
			debug("Params: index="+index);
			solo.setTimePicker(index, hour, minute);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_settimepickerreference)){
			String id = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			TimePicker timePicker = (TimePicker) getViewById(id, TimePicker.class);
			debug("Params: id="+id);
			solo.setTimePicker(timePicker, hour, minute);
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in setTimePicker().");
		}
		
		setGeneralSuccess(props, resultInfo);
	}

	/**
	 * Robotium will sleep for a specified time (milliseconds).<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#sleep(int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void sleep(Properties props) throws ProcessorException{
		int time = SoloMessage.getInteger(props, SoloMessage.PARAM_TIME);
		
		debug("Params: time="+time);
		solo.sleep(time);
		
		setGeneralSuccess(props, time+" millis ");
	}

	/**
	 * Wait for {@link android.app.Activity}.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForActivity(String)}<br>
	 * {@link Solo#waitForActivity(String, int)}<br>
	 * {@link Solo#waitForActivity(Class)} -- Robotium 4.1<br>
	 * {@link Solo#waitForActivity(Class, int)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	@SuppressWarnings("unchecked")
	void waitForActivity(Properties props) throws ProcessorException{
		String resultInfo = "";
		boolean found = false;

		if(remoteCommand.equals(SoloMessage.cmd_waitforactivity) ||
		   remoteCommand.equals(SoloMessage.cmd_waitforactivitytimeout)){
			
			String name = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
			debug("Params: name="+name);
			
			if(remoteCommand.equals(SoloMessage.cmd_waitforactivity)){
				found = solo.waitForActivity(name);
				resultInfo = name;
				
			}else if(remoteCommand.equals(SoloMessage.cmd_waitforactivitytimeout)){
				int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
				debug("Params: timeout="+timeout);
				found = solo.waitForActivity(name, timeout);
				resultInfo = name+" : in "+timeout+" millis ";
			}
			
		}else if(remoteCommand.equals(SoloMessage.cmd_waitforactivitybyclass) ||
				remoteCommand.equals(SoloMessage.cmd_waitforactivitybyclasstimeout)){
			
			String classname = SoloMessage.getString(props, SoloMessage.PARAM_CLASS);
			debug("Params: classname="+classname);
			
			if(remoteCommand.equals(SoloMessage.cmd_waitforactivitybyclass)){
				try {
					resultInfo = classname;
					found = solo.waitForActivity((Class<? extends Activity>) Class.forName(classname));
				}catch(ClassNotFoundException cnfe) {
					debug("Can't find class '"+classname+"'");
				}catch(ClassCastException cce){
					debug("Class '"+classname+"' is not a subclass of Activity.");
				}
			}else if(remoteCommand.equals(SoloMessage.cmd_waitforactivitybyclasstimeout)){
				int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
				debug("Params: timeout="+timeout);
				try {
					resultInfo = classname+" : in "+timeout+" millis ";
					found = solo.waitForActivity((Class<? extends Activity>) Class.forName(classname), timeout);
				}catch(ClassNotFoundException cnfe) {
					debug("Can't find class '"+classname+"'");
				}catch(ClassCastException cce){
					debug("Class '"+classname+"' is not a subclass of Activity.");
				}
			}
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in waitForActivity().");
		}
		
		//solo.waitForActivity() will test if the current activity is the same as the
		//activity described by the parameter.
		//so whether or not we get the waited Activity, we will set the
		//PARAM_CLASS AND PARAM_NAME of the current activity for caller's reference
		setCurrentActivityClassName(props);
		
		if(!found){			
			resultInfo = "Not Found "+resultInfo;
		}else{
			resultInfo = "Found "+resultInfo;
		}
		debug(resultInfo);
		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
	}
	
	/**
	 * Wait for a V4 Fragment by Tag.<br>
	 * Requires Robotium 3.4.1
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForFragmentByTag(String, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForFragmentByTag(Properties props) throws ProcessorException{
		String name = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
		int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
		String resultInfo = "";

		debug("Params: name="+name);
		debug("Params: time="+timeout);
		
		boolean found = solo.waitForFragmentByTag(name, timeout);
		resultInfo = name+" : in "+timeout+" millis ";			
		
		//solo.waitForActivity() will test if the current activity is the same as the
		//activity described by the parameter.
		//so whether or not we get the waited Activity, we will set the
		//PARAM_CLASS AND PARAM_NAME of the current activity for caller's reference
		try{ setCurrentActivityClassName(props);}catch(Exception x){
			debug("waitForFragmentByTag ignoring "+ x.getClass().getSimpleName()+": "+x.getMessage());
		}
		
		if(!found){			
			resultInfo = "Did not find Fragment "+resultInfo;
		}else{
			resultInfo = "Found Fragment "+resultInfo;
		}
		debug(resultInfo);		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
	}
	
	/**
	 * Wait for a V4 Fragment by Id.<br>
	 * Requires Robotium 3.4.1
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForFragmentById(int, int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForFragmentById(Properties props) throws ProcessorException{
		int id = SoloMessage.getInteger(props, SoloMessage.PARAM_ID);
		int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
		String resultInfo = "";

		debug("Params:   id="+id);
		debug("Params: time="+timeout);
		
		boolean found = solo.waitForFragmentById(id, timeout);
		resultInfo = String.valueOf(id)+" : in "+timeout+" millis ";			
		
		//solo.waitForActivity() will test if the current activity is the same as the
		//activity described by the parameter.
		//so whether or not we get the waited Activity, we will set the
		//PARAM_CLASS AND PARAM_NAME of the current activity for caller's reference
		try{ setCurrentActivityClassName(props);}catch(Exception x){
			debug("waitForFragmentById ignoring "+ x.getClass().getSimpleName()+": "+x.getMessage());
		}
		
		if(!found){			
			resultInfo = "Did not find Fragment "+resultInfo;
		}else{
			resultInfo = "Found Fragment "+resultInfo;
		}
		debug(resultInfo);		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
	}
	
	/**
	 * Wait for {@link android.app.Dialog} to close/open.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForDialogToClose(long)}<br>
	 * {@link Solo#waitForDialogToOpen(long)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForDialog(Properties props) throws ProcessorException{
		int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
		boolean success = false;
		
		debug("Params: timeout="+timeout);
		if(remoteCommand.equals(SoloMessage.cmd_waitfordialogtoclose)){
			success = solo.waitForDialogToClose(timeout);
		}else if(remoteCommand.equals(SoloMessage.cmd_waitfordialogtoopen)){
			success = solo.waitForDialogToOpen(timeout);
		}
		
		if(success){
			debug(remoteCommand+" succeed in "+timeout+" millis ");
		}else{
			debug(remoteCommand+" doesn't succeed in "+timeout+" millis ");
		}
		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(success));
		
	}
	
	/**
	 * Wait for {@link android.widget.TextView}.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForText(String)}<br>
	 * {@link Solo#waitForText(String, int, long))}<br>
	 * {@link Solo#waitForText(String, int, long, boolean))}<br>
	 * {@link Solo#waitForText(String, int, long, boolean, boolean))}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForText(Properties props) throws ProcessorException{
		String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
		int minimumNumberOfMatches = 0;
		int timeout = 0;
		boolean scroll = false;
		boolean onlyVisible = false;
		String resultInfo = text;
		boolean found = false;
		
		debug("Params: text="+text);
		if(remoteCommand.equals(SoloMessage.cmd_waitfortext)){
			found = solo.waitForText(text);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_waitfortextmatchtimeout)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
			debug("Params: timeout="+timeout+"; minimumNumberOfMatches="+minimumNumberOfMatches);
			found = solo.waitForText(text, minimumNumberOfMatches, timeout);
			resultInfo += " : minMatch="+minimumNumberOfMatches+" : in "+timeout+" millis ";
			
		}else if(remoteCommand.equals(SoloMessage.cmd_waitfortextmatchtimeoutscroll)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
			scroll = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
			debug("Params: timeout="+timeout+"; minimumNumberOfMatches="+minimumNumberOfMatches+"; scroll="+scroll);
			found = solo.waitForText(text, minimumNumberOfMatches, timeout, scroll);
			resultInfo += " : minMatch="+minimumNumberOfMatches+" : in "+timeout+" millis ";
			
		}else if(remoteCommand.equals(SoloMessage.cmd_waitfortextmatchtimeoutscrollvisible)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
			scroll = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
			onlyVisible = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
			debug("Params: timeout="+timeout+"; minimumNumberOfMatches="+minimumNumberOfMatches+"; scroll="+scroll+"; onlyVisible="+onlyVisible);
			found = solo.waitForText(text, minimumNumberOfMatches, timeout, scroll, onlyVisible);
			resultInfo += " : minMatch="+minimumNumberOfMatches+" : in "+timeout+" millis ";
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in waitForText().");
		}
		
		if(!found){			
			resultInfo = "Not Found "+resultInfo;
		}else{
			resultInfo = "Found "+resultInfo;
		}
		debug(resultInfo);
		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
		
	}
	
	/**
	 * Wait for a specific log message.<br>
	 * Requires Robotium 3.4.1
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForLogMessage(String)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForLogMessage(Properties props) throws ProcessorException{
		String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
		int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);;
		String resultInfo = "'"+ text+"'";
		boolean found = false;
		
		debug("Params: text="+text);
		debug("Params: time="+timeout);
		
		try{ found = solo.waitForLogMessage(text, timeout);}
		catch(Exception x){
			debug("waitForLogMessage ignoring "+ x.getClass().getSimpleName()+": "+ x.getMessage());
		}
		
		if(!found){			
			resultInfo = "Did not find "+ resultInfo;
		}else{
			resultInfo = "Found "+resultInfo;
		}
		debug(resultInfo);
		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));		
	}
	
	/**
	 * Wait for {@link android.view.View}.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForView(Class)}<br>
	 * {@link Solo#waitForView(Class, int, int))}<br>
	 * {@link Solo#waitForView(Class, int, int, boolean))}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForView(Properties props) throws ProcessorException{
		String debugPrefix = TAG + ".waitForView() ";
		String classname = SoloMessage.getString(props, SoloMessage.PARAM_CLASS);
		Class<View> viewClass = null;
		int minimumNumberOfMatches = 0;
		int timeout = 0;
		boolean scroll = false;
		boolean found = false;
		String resultInfo = classname;
		
		debug("Params: classname="+classname);
		//TODO if Class.ForName can't work
		viewClass = ClassViewForName(classname);
		
		if(remoteCommand.equals(SoloMessage.cmd_waitforviewclass)){
			if(viewClass!=null){
				found = solo.waitForView(viewClass);				
			}else{
				//TODO
				debug(debugPrefix +"Need new implementation");
			}

		}else if(remoteCommand.equals(SoloMessage.cmd_waitforviewclassmatchtimeout)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
			
			debug("Params: minimumNumberOfMatches="+minimumNumberOfMatches+"; timeout="+timeout);
			if(viewClass!=null){
				found = solo.waitForView(viewClass, minimumNumberOfMatches, timeout);
				
			}else{
				//TODO
				debug(debugPrefix +"Need new implementation");
			}
			resultInfo += " : minMatch="+minimumNumberOfMatches+" : in "+timeout+" millis ";
			
		}else if(remoteCommand.equals(SoloMessage.cmd_waitforviewclassmatchtimeoutscroll)){
			minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
			scroll = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
			
			debug("Params: minimumNumberOfMatches="+minimumNumberOfMatches+"; timeout="+timeout+"; scroll="+scroll);
			if(viewClass!=null){
				found = solo.waitForView(viewClass, minimumNumberOfMatches, timeout, scroll);
			}else{
				//TODO
				debug(debugPrefix +"Need new implementation");
			}
			resultInfo += " : minMatch="+minimumNumberOfMatches+" : in "+timeout+" millis ";
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in waitForView().");
		}

		if(!found){			
			resultInfo = "Not Found "+resultInfo;
		}else{
			resultInfo = "Found "+resultInfo;
		}
		debug(resultInfo);
		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
	}
	
	/**
	 * Wait for {@link android.view.View}.<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForView(View)}<br>
	 * {@link Solo#waitForView(View, int, boolean))}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForViewUID(Properties props) throws ProcessorException{
		String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
		debug("Params: uid="+uid);

		String resultInfo = "";
		boolean found = false;
		View view = null;
		try{
			view = getViewById(uid, null);
		}catch(ProcessorException e){
			debug(e.getMessage());
		}
		
		if(view!=null){
			debug("got view from cache");
			if(remoteCommand.equals(SoloMessage.cmd_waitforviewreference)){
				found = solo.waitForView(view);
				debug("found view = "+found);
				resultInfo = view.getClass().getSimpleName();
			}else if(remoteCommand.equals(SoloMessage.cmd_waitforviewreferencetimeoutscroll)){
				int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
				boolean scroll = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
				debug("Params: timeout="+timeout+"; scroll="+scroll);
				found = solo.waitForView(view, timeout, scroll);
				resultInfo = view.getClass().getSimpleName()+" in "+timeout+" milliseconds ";
				
			}else{
				throw new ProcessorException(remoteCommand+" could not be processed in waitForViewUID().");
			}
			
			//Whether we get the waited View or not, 
			//we will set the PARAM_CLASS of the view for remote caller's reference
			setViewClassResult(props, view);
		}else{
			debug("couldn't get view from cache");
		}
		
		if(!found){			
			resultInfo = "Not Found "+resultInfo;
		}else{
			resultInfo = "Found "+resultInfo;
		}
		debug(resultInfo);
		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
	}
	
	/**
	 * Wait for {@link android.view.View} according to ID (R.id) <b>Requires Robotium4.1+</b><br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForView(int)}<br>
	 * {@link Solo#waitForView(int, int, int)}<br>
	 * {@link Solo#waitForView(int, int, int, boolean)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForViewByID(Properties props) throws ProcessorException{
		int id = SoloMessage.getInteger(props, SoloMessage.PARAM_ID);
		debug("Params: id="+id);
		
		String resultInfo = "'"+id+"'";
		boolean found = false;

		if(remoteCommand.equals(SoloMessage.cmd_waitforviewid)){
			found = solo.waitForView(id);
			
		}else if(remoteCommand.equals(SoloMessage.cmd_waitforviewidtimeout)){
			int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
			int minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			found = solo.waitForView(id, minimumNumberOfMatches, timeout);
			resultInfo += " in "+timeout+" milliseconds, minimumNumberOfMatches="+minimumNumberOfMatches;
			
		}else if(remoteCommand.equals(SoloMessage.cmd_waitforviewidtimeoutscroll)){
			int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
			int minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MINIMUMMATCHES);
			boolean scroll = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
			found = solo.waitForView(id, minimumNumberOfMatches, timeout, scroll);
			resultInfo += " in "+timeout+" milliseconds, minimumNumberOfMatches="+minimumNumberOfMatches;
			
		}else{
			throw new ProcessorException(remoteCommand+" could not be processed in waitForViewByID().");
		}
		
		if(!found){			
			resultInfo = "Not Found View for id "+resultInfo;
		}else{
			resultInfo = "Found View for id "+resultInfo;
		}
		debug(resultInfo);
		
		setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
	}
	
	/**
	 * Wait for {@link com.jayway.android.robotium.solo.WebElement}. -- Robotium 4.1<br>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForWebElement(By)} -- Robotium 4.1<br>
	 * {@link Solo#waitForWebElement(By, int, boolean)} -- Robotium 4.1<br>
	 * {@link Solo#waitForWebElement(By, int, int, boolean)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void waitForWebElement(Properties props) throws ProcessorException{
		try{
			boolean found = false;
			
			String objectStr = SoloMessage.getString(props, SoloMessage.PARAM_OBJECT);
			By by = SoloMessage.getSoloBy((com.jayway.android.robotium.remotecontrol.By) SoloMessage.decodeBase64Object(objectStr));
			
			if(remoteCommand.equals(SoloMessage.cmd_waitforwebelement)){
				found = solo.waitForWebElement(by);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_waitforwebelementtimeout)){
				int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
				boolean scroll = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
				found = solo.waitForWebElement(by, timeout, scroll);
				
			}else if(remoteCommand.equals(SoloMessage.cmd_waitforwebelementminmatchtimeout)){
				int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
				boolean scroll = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
				int minimumNumberOfMatches = SoloMessage.getInteger(props, SoloMessage.PARAM_MATCH);
				found = solo.waitForWebElement(by, minimumNumberOfMatches, timeout, scroll);
			}
			
			debug("WebElement "+(found?"was found.":"was not found."));
			
			setGeneralSuccessWithSpecialInfo(props, String.valueOf(found));
		}catch(Exception e){
			throw new ProcessorException(Message.getStackTrace(e));
		}
	}
	
	/**
	 * Get the screen's size.<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getScreenSize(Properties props) throws ProcessorException{

		try{
			//If we get the WindowManager from getSystemService(Context.WINDOW_SERVICE), it will
			//give the real size of the device/emulator
			//If we get the WindowManager from getActivity().getWindowManager(), it will give the
			//adjust size of the device/emulator
			
//			Instrumentation inst = activityrunner.getInstrumentation();
//			WindowManager wm = (WindowManager) inst.getContext().getSystemService(Context.WINDOW_SERVICE);
			WindowManager wm = solo.getCurrentActivity().getWindowManager();
			
			if(wm==null){
				debug("Can't get WindowManager!");
			}
			Display display = wm.getDefaultDisplay();
			if(display==null){
				debug("Can't get Display!");
			}

			debug("Screen size ("+display.getWidth()+","+display.getHeight()+")");
			String resultInfo = ";"+display.getWidth()+";"+display.getHeight();
			
			setGeneralSuccessWithSpecialInfo(props, resultInfo);
		}catch(Exception e){
			throw new ProcessorException(e.getMessage());
		}
	}
	
	/**
	 * Get the View's location, (x,y,width,height).<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters<br>
	 * 					&lt;in&gt; UID, String, This is the view's UID according to which we will get a View.
	 */
	void getViewLocation(Properties props) throws ProcessorException{

		try{
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			
			View view = null;
			try{ view = (View) getCachedObject(uid, true); }catch(Exception x){}
			if(view==null){
				throw new ProcessorException(" View for id '"+uid+"' is null.");			
			}
			
			int[] xy = new int[2];
			
			view.getLocationOnScreen(xy);
			debug("view is at ("+xy[0]+","+xy[1]+")");
			debug("view size ("+view.getWidth()+","+view.getHeight()+")");
			
			String resultInfo = ";"+xy[0]+";"+xy[1]+";"+view.getWidth()+";"+view.getHeight();
			
			setGeneralSuccessWithSpecialInfo(props, resultInfo);
		}catch(Exception e){
			throw new ProcessorException(e.getMessage());
		}
	}
	
	
	/**
	 * According to the view's id, try to get a View of type 'TextView'.<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters<br>
	 * 					&lt;in&gt; UID, String, This is the view's UID according to which we will get a View.
	 * @return 
	 */
	void getTextViewValue(Properties props) throws ProcessorException{
		try{
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			
			TextView view = (TextView) getViewById(uid, TextView.class);
			
			if(view==null){
				throw new ProcessorException(" TextView for id '"+uid+"' is null.");			
			}else{
				String text = null;
				text = view.getText().toString();
				debug("TextView's text is '"+text+"'");
				setGeneralSuccessWithSpecialInfo(props, text);
			}
		}catch(Exception e){
			throw new ProcessorException(e.getMessage());
		}
	}
	
	/**
	 * Waits for a condition to be satisfied.<BR>
	 * 
	 * <p>
	 * calling:<br>
	 * {@link Solo#waitForCondition(com.jayway.android.robotium.solo.Condition, int)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters<br>
	 * 					&lt;in&gt; condition, Condition, the condition to wait for.<br>
	 * 					&lt;in&gt; timeout, int, the amount of time in milliseconds to wait.<br>
	 * 					&lt;out&gt; boolean, {@code true} if condition is satisfied;
	 *                                       {@code false} if it is not satisfied or the timeout is reached.<br>
	 *                                                                              
	 */
	void waitForCondition(Properties props) throws ProcessorException{
		try{
			int timeout = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
			String objectStr = SoloMessage.getString(props, SoloMessage.PARAM_OBJECT);
			Condition condition = (Condition) SoloMessage.decodeBase64Object(objectStr);
			
			//Replace the viewUID by the real View object (stored in the cache)
			List<Object> viewIDs= condition.getObjects();
			for(int i=0;i<viewIDs.size();i++){
				debug("convert '"+viewIDs.get(i)+"' to View object");
				condition.setObject(getCachedObject((String)viewIDs.get(i), true), i);
			}
			//Set the first object as the TestRunner
			condition.addObject(robotiumTestrunner, true);
			
			boolean isSatisfied = solo.waitForCondition(condition, timeout);
			
			debug("isSatisfied="+isSatisfied);
			setGeneralSuccessWithSpecialInfo(props, String.valueOf(isSatisfied));
		}catch(Exception e){
			throw new ProcessorException(Message.getStackTrace(e));
		}
	}
	
	/**
	 * Clears text in a WebElement matching the specified By object.
	 * <p>
	 * calling:<br>
	 * {@link Solo#clearTextInWebElement(com.jayway.android.robotium.solo.By)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters<br>
	 * 					&lt;in&gt; by the By object. Examples are: {@code By.id("id")} and {@code By.name("name")}.<br>
	 */
	void clearTextInWebElement(Properties props) throws ProcessorException{
		
		try{
			String objectStr = SoloMessage.getString(props, SoloMessage.PARAM_OBJECT);
			By by = SoloMessage.getSoloBy((com.jayway.android.robotium.remotecontrol.By) SoloMessage.decodeBase64Object(objectStr));
			
			solo.clearTextInWebElement(by);
			
			setGeneralSuccessWithSpecialInfo(props, getWebElementInfo(by));
		}catch(Exception e){
			throw new ProcessorException(Message.getStackTrace(e));
		}
	}
	
	/**
	 * Clicks a WebElement matching the specified By object.
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnWebElement(com.jayway.android.robotium.solo.By)} -- Robotium 4.1<br>
	 * {@link Solo#clickOnWebElement(com.jayway.android.robotium.solo.By, int)} -- Robotium 4.1<br>
	 * {@link Solo#clickOnWebElement(com.jayway.android.robotium.solo.By, int, boolean)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters<br>
	 * 					&lt;in&gt; by the By object. Examples are: {@code By.id("id")} and {@code By.name("name")}.<br>
	 */
	void clickOnWebElement(Properties props) throws ProcessorException{
		
		try{
			String objectStr = SoloMessage.getString(props, SoloMessage.PARAM_OBJECT);
			By by = SoloMessage.getSoloBy((com.jayway.android.robotium.remotecontrol.By) SoloMessage.decodeBase64Object(objectStr));
			
			if(remoteCommand.equals(SoloMessage.cmd_clickonwebelement)){
				solo.clickOnWebElement(by);
			}else if(remoteCommand.equals(SoloMessage.cmd_clickonwebelementindex)){
				int match = SoloMessage.getInteger(props, SoloMessage.PARAM_MATCH);
				solo.clickOnWebElement(by, match);
			}else if(remoteCommand.equals(SoloMessage.cmd_clickonwebelementindexscroll)){
				int match = SoloMessage.getInteger(props, SoloMessage.PARAM_MATCH);
				boolean scroll = SoloMessage.getBoolean(props, SoloMessage.PARAM_SCROLL);
				solo.clickOnWebElement(by, match, scroll);
			}
			
			setGeneralSuccessWithSpecialInfo(props, getWebElementInfo(by));
		}catch(Exception e){
			throw new ProcessorException(Message.getStackTrace(e));
		}
	}
	
	/**
	 * Clicks a WebElement stored in cache by reference UID.
	 * <p>
	 * calling:<br>
	 * {@link Solo#clickOnWebElement(com.jayway.android.robotium.solo.WebElement)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters<br>
	 * 					&lt;in&gt; UID, String, the reference for a WebElement object stored in cache.<br>
	 */
	void clickOnWebElementByUID(Properties props) throws ProcessorException{
		
		try{
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			WebElement webElement = (WebElement) getCachedObject(uid, true);
			solo.clickOnWebElement(webElement);

			setGeneralSuccessWithSpecialInfo(props, getWebElementInfo(webElement));
		}catch(ClassCastException x){
			throw new ProcessorException(remoteCommand+" retrieved object was not an instanceof WebElement!");
		}catch(Throwable e){
			throw new ProcessorException(Message.getStackTrace(e));
		}
	}
	
	/**
	 * Enters text in a WebElement matching the specified By object.
	 * <p>
	 * calling:<br>
	 * {@link Solo#enterTextInWebElement(com.jayway.android.robotium.solo.By, String)} -- Robotium 4.1<br>
	 * {@link Solo#typeTextInWebElement(com.jayway.android.robotium.solo.By, String)} -- Robotium 4.1<br>
	 * {@link Solo#typeTextInWebElement(By, String, int)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters<br>
	 * 					&lt;in&gt; by the By object. Examples are: {@code By.id("id")} and {@code By.name("name")}.<br>
	 * 					&lt;in&gt; text, String, the text to type into a WebElement.<br>
	 * 					&lt;in&gt; index, int, decide into which WebElement to type.<br>
	 */
	void enterTextInWebElement(Properties props) throws ProcessorException{
		
		try{
			String objectStr = SoloMessage.getString(props, SoloMessage.PARAM_OBJECT);
			By by = SoloMessage.getSoloBy((com.jayway.android.robotium.remotecontrol.By) SoloMessage.decodeBase64Object(objectStr));
			
			if(remoteCommand.equals(SoloMessage.cmd_entertextinwebelement)){
				String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
				solo.enterTextInWebElement(by, text);
			}else if(remoteCommand.equals(SoloMessage.cmd_typetextinwebelement)){
				String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
				solo.typeTextInWebElement(by, text);
			}else if(remoteCommand.equals(SoloMessage.cmd_typetextinwebelementindex)){
				String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
				int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
				solo.typeTextInWebElement(by, text, index);
			}
			
			setGeneralSuccessWithSpecialInfo(props, getWebElementInfo(by));
		}catch(Exception e){
			throw new ProcessorException(Message.getStackTrace(e));
		}
	}
	
	/**
	 * Type text in a WebElement stored in cache by reference UID.
	 * <p>
	 * calling:<br>
	 * {@link Solo#typeTextInWebElement(WebElement, String)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters<br>
	 * 					&lt;in&gt; UID, String, the reference for a WebElement object stored in cache.<br>
	 * 					&lt;in&gt; text, String, the text to type into a WebElement.<br>
	 */
	void typeTextInWebElementByUID(Properties props) throws ProcessorException{
		
		try{
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
			WebElement webElement = (WebElement) getCachedObject(uid, true);
			String text = SoloMessage.getString(props, SoloMessage.PARAM_TEXT);
			solo.typeTextInWebElement(webElement, text);

			setGeneralSuccessWithSpecialInfo(props, getWebElementInfo(webElement));
		}catch(ClassCastException x){
			throw new ProcessorException(remoteCommand+" retrieved object was not an instanceof WebElement!");
		}catch(Throwable e){
			throw new ProcessorException(Message.getStackTrace(e));
		}
	}
	
	private String getWebElementInfo(By by){
		String info = "";
		try{
			WebElement webElement = solo.getWebElement(by, 0);
			info = getWebElementInfo(webElement);
		}catch(Exception e){
			debug(Message.getStackTrace(e));
		}
		return info;
	}
	
	private String getWebElementInfo(WebElement webElement){
		String info = "";
		try{
			info = "\nID="+webElement.getId()+
				   "\nName="+webElement.getName()+
				   "\nTagName="+webElement.getTagName()+
				   "\nText="+webElement.getText();
		}catch(Exception e){
			debug(Message.getStackTrace(e));
		}
		return info;
	}
	
	/**
	 * According to the view's id, try to get a View as an 'expected class'.<br>
	 * If not, a {@link ProcessorException} will be thrown out.<br>
	 * If a view is returned, user can CAST it DIRECTLY to 'expected class'<br>
	 * 
	 * @param uid				int, This is the view's uid according to which we will get a View.
	 * 							This uid is the R.id generated when building the AUT.<br>
	 * @param exceptedClass		Class, The expected class for the View that we will get.
	 *                          If this parameter is null, we won't check the View's class name.
	 * @return 
	 */
	private <T extends View> View getViewById(int uid, Class<T> exceptedClass) throws ProcessorException{
		View view = solo.getView(uid);
		String exceptedClassName = null;
		
		if(view==null){
			throw new ProcessorException(" View for id '"+uid+"' is null.");			
		}
		
		if(!checkClass(view, exceptedClass)){
			throw new ProcessorException(" View for id '"+uid+"' is not a '"+exceptedClassName+"'.");
		}

		return view;	
	}

	/**
	 * According to the view's id, try to get a View as an 'expected class'.<br>
	 * If not, a {@link ProcessorException} will be thrown out.<br>
	 * If a view is returned, user can CAST it DIRECTLY to 'expected class'<br>
	 * 
	 * @param uid				String, This is the view's uid according to which we will get a View.
	 * 							This uid is the key in local cache {@link #viewCache}
	 * @param exceptedClass		Class, The expected class for the View that we will get.
	 *                          If this parameter is null, we won't check the View's class name.
	 * @return 
	 */
	private <T extends View> View getViewById(String uid, Class<T> exceptedClass) throws ProcessorException{
		View view = null;
		String exceptedClassName = null;
		try{ view = (View) getCachedObject(uid, true); }catch(Exception x){}
		
		if(view==null){
			throw new ProcessorException(" View for id '"+uid+"' is null.");			
		}
		
		if(!checkClass(view, exceptedClass)){
			exceptedClassName = exceptedClass==null? null : exceptedClass.getName();
			throw new ProcessorException(" View for id '"+uid+"' is '"+view.getClass().getName()+"', not a '"+exceptedClassName+"'.");
		}

		return view;	
	}
	
	/**
	 * Check if the view is the exceptedClass or the child of exceptedClass.<br>
	 * 
	 * @param view				View, the view to be checked.
	 * @param exceptedClass		Class, The expected class for the View.
	 *                          If this parameter is null, we consider the check pass.
	 */
	private <T extends View> boolean checkClass(View view, Class<T> exceptedClass) throws ProcessorException{
		if(view==null){
			throw new ProcessorException(" View is null!");						
		}
		
		if(exceptedClass!=null){
			String exceptedClassName = exceptedClass.getName();
			boolean matched = false;
			Class<?> clazz = view.getClass();
			String clazzname = null;
			
			while(true && clazz!=null){
				clazzname = clazz.getName();
				if(clazzname.equals(Object.class.getName())){
					break;
				}else if(clazzname.equals(exceptedClassName)){
					matched = true;
					break;
				}else{
					clazz = clazz.getSuperclass();
				}
			}
			
			return  matched;		
		}
		
		return true;
		
	}
	
	/**
	 * Create a Class object from the 'class name'<br>
	 * <b>Note:</b> The class MUST be subclass of {@link android.view.View}<br>
	 * 
	 * @param classname		String, the class name to create Class object
	 * @return Class object (for subclass of {@link android.view.View}).
	 *         {@code null}, if the class can't be found or it is not subclass of {@link View}
	 * 
	 */
	@SuppressWarnings("unchecked")
	private <T extends View> Class<T> ClassViewForName(String classname) {
		String debugPrefix = TAG + ".ClassViewForName() ";
		Class<T> viewClass = null;

		try {
			viewClass = (Class<T>) Class.forName(classname);
		} catch (ClassNotFoundException e) {
			debug(debugPrefix +"Class '" + classname + "' not found: " + e.getMessage());
		} catch (ClassCastException e) {
			debug(debugPrefix +"Class '" + classname + "' is not subclass of "+ View.class.getName());
		}

		return viewClass;
	}
	
	/**
	 * Get image of a View.<br> 
	 * According to the view's id, try to get a View from cache.<br>
	 * If not found, a {@link ProcessorException} will be thrown out.<br>
	 * If a view is found, we will get the bitmap of that view and compress<br>
	 * the bytes to String and return it through KEY_REMOTERESULTINFO<br>
	 * 
	 * @param uid				String, This is the view's uid according to which we will get a View.
	 * 							This uid is the key in local cache {@link #viewCache}
	 * @return 
	 */
	void getGuiImage(Properties props) throws ProcessorException{
		String dbPrefix = TAG +".getGuiImage(): ";
		boolean success = false;
		String message = null;

		try{
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_ID);
			
			View view = null;
			try{ view = (View) getCachedObject(uid, true); }catch(Exception x){}
			
			if(view==null){
				throw new ProcessorException(" View for id '"+uid+"' is null.");			
			}else{
				debug(dbPrefix +" Try to get gui image for view "+view.getClass().getSimpleName());
				Bitmap bitmap = getBitmapOfView(view);
				ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
				if (bitmap != null) {
					success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputstream);
					if (success) {
						message = Base64.encodeToString(outputstream.toByteArray(), Base64.DEFAULT);
					} else {
						message = "did not successfully compress Bitmap to PNG format.";
					}
				}else{
					message = "did not get Bitmap for this View.";
				}
				
			    if(success){
			    	debug(dbPrefix +" Succeed to get the view's image.");
			    	setGeneralSuccessWithSpecialInfo(props, message);
			    }else{
			    	debug(dbPrefix +message);	
			    	setGeneralError(props, message);
			    }

			}
		}catch(Exception e){
			throw new ProcessorException(e.getMessage());
		}
	}
	
	/**
	 * Get bitmap image of a View.<br>
	 * This method call {@link View#getDrawingCache()} to get the bitmap image.<br>
	 * For some special views, we can't generate the bitmap by this way. We can<br>
	 * override this method and provide a special way in the subclass.<br>
	 * 
	 * @param view				View, The View object
	 *
	 * @return	The bitmap object of a view. 
	 */	
	protected Bitmap getBitmapOfView(View view){
		String dbPrefix = TAG +"getBitmapOfView(): ";
		Bitmap bitmap = null;
		
	    Rect v = new Rect();
	    boolean isvisible = view.getGlobalVisibleRect(v);
	    if(isvisible){
			View root = view.getRootView();
			root.setDrawingCacheEnabled(true);
			bitmap = Bitmap.createBitmap(root.getDrawingCache(), v.left, v.top, v.width(), v.height());
			root.setDrawingCacheEnabled(false);
			debug(dbPrefix + " Getting Image for normal View.");
	    }else{
	    	debug(dbPrefix + " The View is not visible.");
	    }
		
	    return bitmap;
	}
	
	/**
	 * Get class name of a View.<br> 
	 * According to the view's id, try to get a View from cache.<br>
	 * If not found, a {@link ProcessorException} will be thrown out.<br>
	 * If a view is found, we will get the name of that view and return<br>
	 * it through KEY_REMOTERESULTINFO<br>
	 * 
	 * @param uid				String, This is the view's uid according to which we will get a View.
	 * 							This uid is the key in local cache {@link #viewCache}
	 * @return	String, the class name of the view. 
	 * @deprecated we can use {@link #getObjectClassName(Properties, boolean)} instead
	 */	
	void getViewClassName(Properties props) throws ProcessorException{

		try{
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_ID);
			
			View view = null;
			try{ view = (View) getCachedObject(uid, true); }catch(Exception x){}
			
			if(view==null){
				throw new ProcessorException(" View for id '"+uid+"' is null.");			
			}else{
				setGeneralSuccessWithSpecialInfo(props, view.getClass().getName());
			}
		}catch(Exception e){
			throw new ProcessorException(e.getMessage());
		}
	}
	
	/**
	 * Get class name of an object.<br> 
	 * According to the object's id, try to get it from cache.<br>
	 * If not found, a {@link ProcessorException} will be thrown out.<br>
	 * If it is found, we will get its class name and return<br>
	 * through KEY_REMOTERESULTINFO<br>
	 * 
	 * @param uid				String, This is the object's uid according to which we will get the object.
	 * 							This uid is the key in local cache {@link #viewCache} or {@link #activityCache}
	 * 							or {@link #webElementCache}
	 * @return	String, the class name of the object. 
	 */	
	void getObjectClassName(Properties props, boolean isViewObject) throws ProcessorException{
		
		try{
			String uid = SoloMessage.getString(props, SoloMessage.PARAM_ID);
			
			Object object = getCachedObject(uid, true);
			//Just try to cast it to View object
			if(isViewObject) object = (View) object;
			
			if(object==null){
				throw new ProcessorException(" Object for id '"+uid+"' is null.");			
			}else{
				setGeneralSuccessWithSpecialInfo(props, object.getClass().getName());
			}
		}catch(Exception e){
			throw new ProcessorException(e.getClass()+":"+e.getMessage());
		}
	}

	/**
	 * Takes a screenshot sequence and stores the images via the Robotium Solo API.<br>
	 * Requires Robotium 4.2
	 * <p>
	 * calling:<br>
	 * {@link Solo#startScreenshotSequence(String,int,int,int)}<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void startScreenshotSequenceMax(Properties props){
		String debugPrefix = TAG+".startScreenshotSequence() ";
		boolean success = false;
		try{ 
			String filename = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
			if(filename == null || filename.length() < 1)
				throw new ProcessorException(debugPrefix +"filename is null or invalid.");	
			
			int quality = SoloMessage.getInteger(props, SoloMessage.PARAM_QUALITY);
			if (quality < 0 || quality > 100)
				throw new ProcessorException(debugPrefix +"quality value "+ quality +" must be 0-100.");			
			
			int frameDelay = SoloMessage.getInteger(props, SoloMessage.PARAM_TIME);
			if (frameDelay < 0)
				throw new ProcessorException(debugPrefix +"frameDelay value "+ frameDelay +" cannot be less than 0.");
			
			int maxFrames = SoloMessage.getInteger(props, SoloMessage.PARAM_STEPCOUNT);
			if (maxFrames < 1)
				throw new ProcessorException(debugPrefix +"maxFrames value "+ maxFrames +" cannot be less than 1.");
			
			String message = Environment.getExternalStorageDirectory() + "/Robotium-Screenshots/"+filename;
			try{ 
				solo.startScreenshotSequence(filename, quality, frameDelay, maxFrames);
				success = true;
			}catch(Exception x){
				message = "Met Exception "+x.getClass().getSimpleName()+": "+x.getMessage();
				debug(debugPrefix +"ignoring "+ x.getClass().getSimpleName()+": "+x.getMessage());
			}
			
		    if(success){
		    	debug(debugPrefix +"success for screenshot sequence "+ message);
		    	setGeneralSuccessWithSpecialInfo(props, message);
		    }else{
		    	debug(debugPrefix + "failure for screenshot sequence "+ filename);	
		    	setGeneralError(props, message);
		    }
		}
		catch(Throwable x){
			String msg = x.getClass().getSimpleName()+": "+ x.getMessage();
			debug(TAG+".startScreenshotSequence(filename, quality, frameDelay, maxFrames) "+ msg);
			setGeneralError(props, msg);
		}
	}

	/**
	 * Retrieve a sequence of screenshots, which are stored in "/sdcard/Robotium-Screenshots/".<br>
	 * Requires Robotium 4.1+
	 * <p>
	 * These screenshots are generated by calling:<br>
	 * {@link Solo#startScreenshotSequence(String)} -- Robotium 4.1+<br>
	 * {@link Solo#startScreenshotSequence(String, int, int, int)} -- Robotium 4.1+<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void getScreenshotSequence(Properties props){
		String debugPrefix = TAG+".getScreenshotSequence() ";
		boolean success = false;
		String message = "";
		
		try{ 
			String filename = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
			if(filename == null || filename.length() < 1)
				throw new ProcessorException(debugPrefix +"filename is null or invalid.");			
			
			debug(debugPrefix +" Try to get completed screenshots...");
			StringBuffer absoluteFilePath = new StringBuffer();
			if(remoteCommand.equals(SoloMessage.cmd_getscreenshotsequence)){
				boolean onlyLasttime = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
				//absoluteFilePath will get back a set of valid filenames
				getScreenshotSequenceSize(filename, onlyLasttime, absoluteFilePath);
				message = absoluteFilePath.toString();
				success = true;
				
			}else if(remoteCommand.equals(SoloMessage.cmd_getscreenshotsequenceszie)){
				boolean onlyLasttime = SoloMessage.getBoolean(props, SoloMessage.PARAM_ONLYVISIBLE);
				//absoluteFilePath will get back a set of valid filenames
				message = String.valueOf(getScreenshotSequenceSize(filename, onlyLasttime, absoluteFilePath));
				props.setProperty(SoloMessage.PARAM_NAME+"FILE", absoluteFilePath.toString());
				success = true;
				
			}else if(remoteCommand.equals(SoloMessage.cmd_getscreenshotsequenceindex)){
				int index = SoloMessage.getInteger(props, SoloMessage.PARAM_INDEX);
				
				message = getImageBase64EncodedString(filename +"_"+index, absoluteFilePath);
				props.setProperty(SoloMessage.PARAM_NAME+"FILE", absoluteFilePath.toString());
				success = true;
				
			}else{
				message = remoteCommand+" can't be handled in method getScreenshotSequence()!";
			}
			
		    if(success){
		    	setGeneralSuccessWithSpecialInfo(props, message);
		    }else{
		    	debug(debugPrefix +message);	
		    	setGeneralError(props, message);
		    }
		    
		}catch(Throwable x){
			String msg = x.getClass().getSimpleName()+": "+ x.getMessage();
			debug("getScreenshotSequence() met "+msg);
			setGeneralError(props, msg);
		}
	}
	
	/**
	 * Takes a Screenshot and retrieve it from /sdcard/Robotium-Screenshots/.<br>
	 * Requires Robotium 3.4.1
	 * <p>
	 * calling:<br>
	 * {@link Solo#takeScreenshot(String)}<br>
	 * {@link Solo#takeScreenshot(String, int)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void takeScreenshot(Properties props){
		String debugPrefix = TAG+".takeScreenshot(filename) ";
		boolean success = false;
		String message = "";
		try{ 
			String filename = SoloMessage.getString(props, SoloMessage.PARAM_NAME);
			if(filename == null || filename.length() < 1)
				throw new ProcessorException(debugPrefix +"filename is null or invalid.");			
			
			try{ 
				if(remoteCommand.equals(SoloMessage.cmd_takescreenshot)){
					solo.takeScreenshot(filename);
				}else if(remoteCommand.equals(SoloMessage.cmd_takescreenshotquality)){
					int quality = SoloMessage.getInteger(props, SoloMessage.PARAM_QUALITY);
					solo.takeScreenshot(filename, quality);
				}
			}catch(Exception x){
				debug("takeScreenshot ignoring "+ x.getClass().getSimpleName()+": "+x.getMessage());
			}

			debug(debugPrefix +" Try to get completed screenshot...");
			StringBuffer absoluteFilePath = new StringBuffer();
			try{
				message = getImageBase64EncodedString(filename, absoluteFilePath);
				success = true;
			}catch(ProcessorException pe){
				message = pe.getMessage();
				success = false;
			}
			
		    if(success){
		    	props.setProperty(SoloMessage.PARAM_NAME+"FILE", absoluteFilePath.toString());
		    	debug(debugPrefix +" Succeeded to get the screenshot "+ absoluteFilePath.toString());
		    	setGeneralSuccessWithSpecialInfo(props, message);
		    }else{
		    	debug(debugPrefix +message);	
		    	setGeneralError(props, message);
		    }
		}
		catch(Throwable x){
			String msg = x.getClass().getSimpleName()+": "+ x.getMessage();
			debug("takeScreenshot(filename) "+msg);
			setGeneralError(props, msg);
		}
	}

	/**
	 * Read a JPG file with name 'filename' from folder '/sdcard/Robotium-Screenshots/', then encode it to
	 * a String and return, this method doesn't care when the image file is created.
	 * 
	 * @param filename, 		String, in, 		the image file to read from mobile device.
	 * @param absoluteFilePath, StringBuffer, out,	the absolute name of the image file. 
	 * @return String, the Base64 encoded string of the image.
	 * @see #getImageBase64EncodedString(String, StringBuffer, long)
	 */
	String getImageBase64EncodedString(String filename/*in*/,
            StringBuffer absoluteFilePath/*out*/) throws ProcessorException{
		//We will read the image, no matter when the image is created.
		return getImageBase64EncodedString(filename, absoluteFilePath, -1);
	}
	/**
	 * Read a JPG file with name 'filename' from folder '/sdcard/Robotium-Screenshots/', then encode it to
	 * a String and return, this method will only read the image file created after the time provide 
	 * by parameter 'newerThan'
	 * 
	 * @param filename, 		String, in, 		the image file to read from mobile device.
	 * @param absoluteFilePath, StringBuffer, out,	the absolute name of the image file.
	 * @param newerThan, 		long, in,			the time to compare with the file's last modified time,
	 *                                              if 'last modified time' is smaller than it, the file will not be read. 
	 * @return String, the Base64 encoded string of the image.
	 * @see #getImageBase64EncodedString(String, StringBuffer)
	 */
	String getImageBase64EncodedString(String filename/*in*/,
			                           StringBuffer absoluteFilePath/*out*/,
			                           long newerThan/*in*/) throws ProcessorException{
		String imgString = null;
		File fileToRead = getRobotiumScreenshotFile(filename);
		
		if(waitForFileExistAndReadable(fileToRead, 5000)){//if the image is very big, is 5 seconds enough?
			if(fileToRead.lastModified()<newerThan){
				throw new ProcessorException(fileToRead.getAbsolutePath()+" is too old, we don't read it!");
			}
			if(absoluteFilePath!=null) absoluteFilePath.append(fileToRead.getAbsolutePath());
			BufferedInputStream inputstream = null;
			ByteArrayOutputStream outputstream = null;
			try {
				inputstream = new BufferedInputStream(new FileInputStream(fileToRead));
				outputstream = new ByteArrayOutputStream();
				int byt = 0;
				while(inputstream.available() > 0 && byt != -1){
					byt = inputstream.read();
					if(byt != -1) outputstream.write(byt);
				}
				outputstream.flush();
				imgString = Base64.encodeToString(outputstream.toByteArray(), Base64.DEFAULT);
				
				if(imgString.length() <= 0){
					throw new ProcessorException(" image data appears to be empty!");
				}
			} catch (Exception e) {
				throw new ProcessorException(e.getClass()+":"+e.getMessage());
			}finally{
				if(inputstream!=null) try{ inputstream.close();}catch (Exception e) {}
				if(outputstream!=null) try{ outputstream.close();}catch (Exception e) {}
			}
		}else{
			throw new ProcessorException(fileToRead.getAbsolutePath()+" does not exist or is not readable.");
		}
		
		return imgString;
	}
	
	/**
	 * @param filename, 		String, in, 		the image file to read from mobile device.
	 * @param onlyLasttime,		boolean, in,		if true, count only the sequence generated last time; if false, count all.
	 * @param absoluteFilePath, StringBuffer, out,	the absolute name of the image file.
	 * @return int, 	the number of valid image file generated by {@link #startScreenshotSequenceMax(Properties)}
	 */
	private int getScreenshotSequenceSize(String filename/*in*/, 
			            		  		  boolean onlyLasttime/*in*/,
			            		  		  StringBuffer absoluteFilePath/*out*/){
		int size = 0;
		try{
			long timeOfFirstImageOfSequence = 0; 
			if (onlyLasttime) getLastModifyTime(getRobotiumScreenshotFile(filename +"_0"));
			File fileToRead = null;
			
			while(true){
				absoluteFilePath.append(";");
				fileToRead = getRobotiumScreenshotFile(filename+"_"+size);
				if(waitForFileExistAndReadable(fileToRead, 5000)){//if the image is very big, is 5 seconds enough?
					if(onlyLasttime && fileToRead.lastModified()<timeOfFirstImageOfSequence){
						throw new ProcessorException(fileToRead.getAbsolutePath()+" is too old, we don't read it!");
					}
				}else{
					throw new ProcessorException(fileToRead.getAbsolutePath()+" does not exist or is not readable.");
				}
				//If we can arrive here, which means the image file is valid, we count it.
				absoluteFilePath.append(fileToRead.getAbsolutePath());
				size++;
			}
						
		}catch(ProcessorException pe){
			//We should ignore this Exception.
			//it is very possible the image file doesn't exist for one of the sequence images
			//if user has called the solo.stopScreenshotSequence().
			//Or there are some older image file with the same prefix
			debug("Ignoring Exception "+pe.getMessage());
		}
		
		return size;
	}
	
	/**
	 * Use the file prefix to create a robotium screen shot File.<br>
	 * The folder is /sdcard/Robotium-Screenshots/<br>
	 * The suffix is .jpg<br>
	 * 
	 * @param filePrefix, String, the filename prefix
	 * @return File, the robotium screen shot File
	 */
	private File getRobotiumScreenshotFile(String filePrefix){
		File directory = new File(Environment.getExternalStorageDirectory() + "/Robotium-Screenshots/");
		//Robotium save file as a jpg image for now, but if one day it changes??
		File fileToRead = new File(directory, filePrefix +".jpg");
		
		return fileToRead;
	}
	/**
	 * @param fileToRead, File, the file to get the last modified time
	 * @return long, the last modified time of a file
	 */
	private long getLastModifyTime(File fileToRead){
		long lastModifyTime = -1;
		
		if(waitForFileExistAndReadable(fileToRead, 5000)){//if the image is very big, is 5 seconds enough?
			lastModifyTime = fileToRead.lastModified();
		}
		
		return lastModifyTime;
	}
	
	/**
	 * @param fileToRead, File
	 * @param timeout, time to wait for existence of the file, in milliseconds
	 * @return boolean, true if the file exists and can be read
	 */
	private boolean waitForFileExistAndReadable(File fileToRead, int timeout/*millisecond*/){
		
		long startTime = (new Date()).getTime();
		while(!(fileToRead.exists()||fileToRead.canRead()) &&
				((new Date()).getTime() < (startTime+timeout))){
			//Wait for for the image to be ready to read
			solo.sleep(100);
		}
		
		return fileToRead.exists() && fileToRead.canRead();
	}
	
	/**
	 * Handle the static methods of class RobotiumUtils.<br>
	 * Requires Robotium 4.1
	 * <p>
	 * calling:<br>
	 * {@link RobotiumUtils#filterViews(Class, Iterable)} -- Robotium 4.1<br>
	 * {@link RobotiumUtils#filterViewsByText(Iterable, java.util.regex.Pattern)} -- Robotium 4.1<br>
	 * {@link RobotiumUtils#filterViewsByText(Iterable, String)} -- Robotium 4.1<br>
	 * {@link RobotiumUtils#filterViewsToSet(Class[], Iterable)} -- Robotium 4.1<br>
	 * {@link RobotiumUtils#sortViewsByLocationOnScreen(List)} -- Robotium 4.1<br>
	 * {@link RobotiumUtils#sortViewsByLocationOnScreen(List, boolean)} -- Robotium 4.1<br>
	 * {@link RobotiumUtils#getNumberOfMatches(String, TextView, java.util.Set)} -- Robotium 4.1<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void handleRobotiumUtilsCommand(Properties props){

		try{
			List resultUIDList = new ArrayList();
			
			if(remoteCommand.equals(SoloMessage.cmd_utilsfilterviews) ||
			   remoteCommand.equals(SoloMessage.cmd_utilsfilterviewsbytext) ||
			   remoteCommand.equals(SoloMessage.cmd_utilsfilterviewstoset) ||
			   remoteCommand.equals(SoloMessage.cmd_utilsremoveinvisibleviews) ||
			   remoteCommand.equals(SoloMessage.cmd_utilssortviewsbylocationonscreen) ||
			   remoteCommand.equals(SoloMessage.cmd_utilssortviewsbylocationonscreenyfirst)){
				
				List<String> uidList = SoloMessage.parseStringArrayList(SoloMessage.getString(props, SoloMessage.PARAM_REFERENCES));
				//objectToUIDHash contains pair <View, UID>
				HashMap objectToUIDHash = new HashMap();
				String UID = null;
				Object object = null;
				for (int i=0;i<uidList.size();i++) {
					UID = uidList.get(i);
					object = getCachedObject(UID, true);
					if(object==null){
						debug("Can't find cached object for UID '"+UID+"'!!!!");
					}else{
						if(remoteCommand.equals(SoloMessage.cmd_utilsfilterviewsbytext)){
							if(object instanceof TextView) objectToUIDHash.put(object, UID);
						}else{
							objectToUIDHash.put(object, UID);
						}
					}
				}
				
				List resultObjectList = null;
				
				if(remoteCommand.equals(SoloMessage.cmd_utilsfilterviews)){
					Class<?> classToFilterBy = Class.forName(SoloMessage.getString(props, SoloMessage.PARAM_CLASS));
					resultObjectList = RobotiumUtils.filterViews(classToFilterBy, objectToUIDHash.keySet());
					
				} else if (remoteCommand.equals(SoloMessage.cmd_utilsfilterviewsbytext)) {
					String regex = SoloMessage.getString(props, SoloMessage.PARAM_REGEX_STRING);
					resultObjectList = RobotiumUtils.filterViewsByText(objectToUIDHash.keySet(), regex);
					
				} else if (remoteCommand.equals(SoloMessage.cmd_utilsfilterviewstoset)) {
					List<String> classNameList = SoloMessage.parseStringArrayList(SoloMessage.getString(props, SoloMessage.PARAM_CLASSES));
					List<Class<View>> clazzList = new ArrayList<Class<View>>();
					for(int i=0;i<classNameList.size();i++){
						clazzList.add((Class<View>) Class.forName(classNameList.get(i)));
					}
					resultObjectList = RobotiumUtils.filterViewsToSet(clazzList.toArray(new Class[0]), objectToUIDHash.keySet());
					
				} else if (remoteCommand.equals(SoloMessage.cmd_utilsremoveinvisibleviews)) {
					resultObjectList = RobotiumUtils.removeInvisibleViews(objectToUIDHash.keySet());
					
				} else if (remoteCommand.equals(SoloMessage.cmd_utilssortviewsbylocationonscreen)) {
					resultObjectList = new ArrayList<Object>();
					resultObjectList.addAll(objectToUIDHash.keySet());
					RobotiumUtils.sortViewsByLocationOnScreen(resultObjectList);
					
				} else if (remoteCommand.equals(SoloMessage.cmd_utilssortviewsbylocationonscreenyfirst)) {
					boolean yAxisFirst = SoloMessage.getBoolean(props, SoloMessage.PARAM_YAXISFIRST);
					resultObjectList = new ArrayList<Object>();
					resultObjectList.addAll(objectToUIDHash.keySet());
					RobotiumUtils.sortViewsByLocationOnScreen(resultObjectList, yAxisFirst);
				}
				
				for(int i=0;i<resultObjectList.size();i++){
					resultUIDList.add(objectToUIDHash.get(resultObjectList.get(i)));
				}
				
			}else if (remoteCommand.equals(SoloMessage.cmd_utilsgetnumberofmatches)) {
				String uid = SoloMessage.getString(props, SoloMessage.PARAM_REFERENCE);
				TextView textView = (TextView) getCachedObject(uid, true);
				String regex = SoloMessage.getString(props, SoloMessage.PARAM_REGEX_STRING);
				
				HashSet<TextView> uniqueTextViews = new HashSet<TextView>();
				int match = RobotiumUtils.getNumberOfMatches(regex, textView, uniqueTextViews);
				//if matched, match time should be 1, uniqueTextViews should contain only textView.
				//TODO Need to know the usage of RobotiumUtils.getNumberOfMatches() for future implementation
				debug("Match time is "+match+"; uniqueTextViews size="+uniqueTextViews.size());
				if(uniqueTextViews.size()>0) resultUIDList.add(uid);
			}
			
			setGeneralSuccessWithSpecialInfo(props,Message.convertToDelimitedString(resultUIDList));
			
		}catch(Throwable x){
			debug("handleRobotiumUtilsCommand() "+SoloMessage.getStackTrace(x));
			setGeneralError(props, x.getClass().getSimpleName()+": "+ x.getMessage());
		}
	}
	
	/**
	 * Handle the static methods of class Timeout.<br>
	 * Requires Robotium 4.1+
	 * <p>
	 * calling:<br>
	 * {@link Timeout#setLargeTimeout(int)}  -- Robotium 4.1+<br>
	 * {@link Timeout#setSmallTimeout(int)}  -- Robotium 4.1+<br>
	 * {@link Timeout#getLargeTimeout}  -- Robotium 4.1+<br>
	 * {@link Timeout#getSmallTimeout}  -- Robotium 4.1+<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void handleRobotiumTimeoutCommand(Properties props){

		try{
			int millisecond = -1;

			if(remoteCommand.equals(SoloMessage.cmd_setlargetimeout)){
				millisecond = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
				Timeout.setLargeTimeout(millisecond);
				
			} else if (remoteCommand.equals(SoloMessage.cmd_setsmalltimeout)) {
				millisecond = SoloMessage.getInteger(props, SoloMessage.PARAM_TIMEOUT);
				Timeout.setSmallTimeout(millisecond);
				
			} else if (remoteCommand.equals(SoloMessage.cmd_getlargetimeout)) {
				millisecond = Timeout.getLargeTimeout();
				
			} else if (remoteCommand.equals(SoloMessage.cmd_getsmalltimeout)) {
				millisecond = Timeout.getSmallTimeout();
				
			}
			
			setGeneralSuccessWithSpecialInfo(props,String.valueOf(millisecond));
			
		}catch(Throwable x){
			debug("handleRobotiumTimeoutCommand() "+SoloMessage.getStackTrace(x));
			setGeneralError(props, x.getClass().getSimpleName()+": "+ x.getMessage());
		}
	}
	
	/**
	 * Handle the zoom, rotate, swipe methods of Robotium Solo.<br>
	 * Requires Robotium 4.1+
	 * <p>
	 * calling:<br>
	 * {@link Solo#pinchToZoom(PointF, PointF, PointF, PointF)}  -- Robotium 4.1+<br>
	 * {@link Solo#rotateLarge(PointF, PointF)  -- Robotium 4.1+<br>
	 * {@link Solo#rotateSmall(PointF, PointF)  -- Robotium 4.1+<br>
	 * {@link Solo#swipe(PointF, PointF, PointF, PointF) -- Robotium 4.1+<br>
	 * 
	 * @param props		The Properties object containing the in and out parameters
	 */
	void handleZoomRotateSwipe(Properties props){

		try{
			String base64String = SoloMessage.getString(props, SoloMessage.PARAM_OBJECT);
			@SuppressWarnings("unchecked")
			ObjectCollection<com.jayway.android.robotium.remotecontrol.PointF> pointCollection = 
				(ObjectCollection<com.jayway.android.robotium.remotecontrol.PointF>) SoloMessage.decodeBase64Object(base64String);
			List<PointF> points = SoloMessage.getAndroidPointFList(pointCollection.getObjectList());
			
			if(remoteCommand.equals(SoloMessage.cmd_pinchtozoom)){
				if(points.size()<4) throw new Exception("For command '"+remoteCommand+"', the parameters are not enough.");
				solo.pinchToZoom(points.get(0), points.get(1), points.get(2), points.get(3));
				
			} else if (remoteCommand.equals(SoloMessage.cmd_rotatelarge)) {
				if(points.size()<2) throw new Exception("For command '"+remoteCommand+"', the parameters are not enough.");
				solo.rotateLarge(points.get(0), points.get(1));
				
			} else if (remoteCommand.equals(SoloMessage.cmd_rotatesmall)) {
				if(points.size()<2) throw new Exception("For command '"+remoteCommand+"', the parameters are not enough.");
				solo.rotateSmall(points.get(0), points.get(1));
				
			} else if (remoteCommand.equals(SoloMessage.cmd_swipe)) {
				if(points.size()<4) throw new Exception("For command '"+remoteCommand+"', the parameters are not enough.");
				solo.swipe(points.get(0), points.get(1), points.get(2), points.get(3));
				
			}
			
			setGeneralSuccess(props);
			
		}catch(Throwable x){
			debug("handleRobotiumTimeoutCommand() "+SoloMessage.getStackTrace(x));
			setGeneralError(props, x.getClass().getSimpleName()+": "+ x.getMessage());
		}
	}	
	
	/** Used for CacheReferenceInterface instance storage. */
	@SuppressWarnings("rawtypes")
	private Vector chainedCache = new Vector();
	
	/** CacheReferenceInterface implementation. */
	public Object getCachedObject(String key, boolean useChain) {
		Object item = getCachedItem(viewCache, key);
		if(item == null) item = getCachedItem(activityCache, key);
		if(item == null) item = getCachedItem(webElementCache, key);
		if(item == null) item = getCachedItem(activityMonitorCache, key);
		if(item == null && useChain){
			for(int i=0;i<chainedCache.size()&&item==null;i++){
				CacheReferenceInterface c = (CacheReferenceInterface) chainedCache.elementAt(i);
				item = c.getCachedObject(key, false);//avoid infinite circular references				
			}
		}
		return item;
	}

	/** CacheReferenceInterface implementation. 
	 * @see CacheReferenceInterface#addCacheReferenceInterface(CacheReferenceInterface) */
	@SuppressWarnings("unchecked")
	public void addCacheReferenceInterface(CacheReferenceInterface cache) {
		if(! chainedCache.contains(cache)) chainedCache.add(cache);
	}

	/** CacheReferenceInterface implementation. 
	 * @see CacheReferenceInterface#removeCacheReferenceInterface(CacheReferenceInterface) */
	public void removeCacheReferenceInterface(CacheReferenceInterface cache) {
		if(chainedCache.contains(cache)) chainedCache.remove(cache);
	}

	/** CacheReferenceInterface implementation. 
	 * @see CacheReferenceInterface#clearCache(boolean) */
	public void clearCache(boolean useChain) {
		resetExternalModeCache(viewCache);
		resetExternalModeCache(webElementCache);
		resetExternalModeCache(activityCache);
		//resetExternalModeCache(activityMonitorCache); //must not be cleared until testing is done
		if(useChain){
			for(int i=0;i<chainedCache.size();i++){
				CacheReferenceInterface c = (CacheReferenceInterface) chainedCache.elementAt(i);
				c.clearCache(false);//avoid infinite circular references				
			}
		}
	}

}
