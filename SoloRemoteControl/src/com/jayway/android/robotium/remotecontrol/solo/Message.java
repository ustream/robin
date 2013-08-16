/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.solo;

import java.util.ArrayList;
import java.util.List;

import org.w3c.tools.codec.Base64Decoder;
import org.w3c.tools.codec.Base64Encoder;

/**
 * The constants used by both Robotium Remote Control and the Robotium Test Runner to exchange commands and data.
 * 
 * @author Carl Nagle, SAS Institute, Inc.
 * @since
 * <br>May 17, 2013		(SBJLWA)	Update to support Robotium 4.1
 * <br>Jun 21, 2013		(SBJLWA)	Update to support Robotium 4.1+
 * <br>Jun 25, 2013		(CANAGL)	Update to support Robotium 4.2
 */
public class Message extends org.safs.sockets.Message{

	/** The Property key for the command/method to be executed on the remote device. */
	public static final String KEY_COMMAND = "command";	
	/** The Property key for the name of the target to execute the command. */
	public static final String KEY_TARGET = "target";	
	
	/**
	 * Property key from the device-side that the controller should change its timeout.
	 * Used in {@link SoloRemoteControl#waitForRemoteResult(int)} to extend or shorten the wait 
	 * for the currently running command.  If the device-side sends a remoteResult with this key 
	 * present then the waitForRemoteResults should wait again using the new timeout value 
	 * specified in the property value. 
	 * The new timeout there should be specified in seconds.
	 */
	public static final String KEY_CHANGETIMEOUT = "changetimeout";
	
	/** The charset used to translate the base64 encoded bytes to string,
	 *  and translate that string back to base64 encoded bytes.*/
	public static final String KEY_UTF8_CHARSET = "UTF-8";
	
	/** The name for the default Solo target. */
	public static final String target_solo = "solo";	
	/** The name for the default InstrumentationTestRunner target. */
	public static final String target_instrument = "instrument";
	
	/** The Property key for a "errormsg" parameter used by Solo for jUnit assertion failure messages. */
	public static final String PARAM_ERRORMSG = "errormsg";
	/** The Property key for a "class" parameter used by Solo. */
	public static final String PARAM_CLASS = "class";
	/** The Property key for a "classes" parameter used by Solo.
	* this string's content format is: ";classname;classname;classname" */
	public static final String PARAM_CLASSES = "classes";
	/** The Property key for a "isnewinstance" parameter used by Solo. */
	public static final String PARAM_ISNEWINSTANCE = "isnewinstance";
	/** The Property key for a "timeout" parameter used by Solo. */
	public static final String PARAM_TIMEOUT = "timeout";
	/** The Property key for a "timeouttype" parameter used by Solo. */
	public static final String PARAM_TIMEOUT_TYPE = "timeouttype";
	/** The Property key for an String UID "reference" parameter used by Solo. */
	public static final String PARAM_REFERENCE = "reference";
	/** The Property key for an String of UIDs "reference" parameter used by Solo.
	 * this string's content format is: ";UID;UID;UID" */
	public static final String PARAM_REFERENCES = "referencelist";
	/** The Property key for a "index" parameter used by Solo. */
	public static final String PARAM_INDEX = "index";
	/** The Property key for a "line" parameter used by Solo. */
	public static final String PARAM_LINE = "line";
	/** The Property key for a "time" parameter used by Solo. */
	public static final String PARAM_TIME = "time";
	/** The Property key for a "floatx" parameter used by Solo. */
	public static final String PARAM_FLOATX = "floatx";
	/** The Property key for a "floaty" parameter used by Solo. */
	public static final String PARAM_FLOATY = "floaty";
	/** The Property key for a "clicknumber" parameter used by Solo. */
	public static final String PARAM_CLICKNUMBER = "clicknumber";
	/** The Property key for a "text" parameter used by Solo. */
	public static final String PARAM_TEXT = "text";
	/** The Property key for a "match" parameter used by Solo. */
	public static final String PARAM_MATCH = "match";
	/** The Property key for a "minimummatches" parameter used by Solo. */
	public static final String PARAM_MINIMUMMATCHES = "minimummatches";
	/** The Property key for a "scroll" parameter used by Solo. */
	public static final String PARAM_SCROLL = "scroll";
	/** The Property key for a "name" parameter used by Solo. */
	public static final String PARAM_NAME = "name";
	/** The Property key for a "submenu" parameter used by Solo. */
	public static final String PARAM_SUBMENU = "submenu";
	/** The Property key for a "fromx" parameter used by Solo. */
	public static final String PARAM_FROMX = "fromx";
	/** The Property key for a "fromy" parameter used by Solo. */
	public static final String PARAM_FROMY = "fromy";
	/** The Property key for a "tox" parameter used by Solo. */
	public static final String PARAM_TOX = "tox";
	/** The Property key for a "tox" parameter used by Solo. */
	public static final String PARAM_TOY = "toy";
	/** The Property key for a "stepcount" parameter used by Solo. */
	public static final String PARAM_STEPCOUNT = "stepcount";
	/** The Property key for a "onlyvisible" parameter used by Solo. */
	public static final String PARAM_ONLYVISIBLE = "onlyvisible";
	/** The Property key for a "resid" parameter used by Solo. */
	public static final String PARAM_RESID = "resid";
	/** The Property key for a "id" parameter used by Solo. */
	public static final String PARAM_ID = "id";
	/** The Property key for a "itemsperrow" parameter used by Solo. */
	public static final String PARAM_ITEMSPERROW = "itemsperrow";
	/** The Property key for a "itemindex" parameter used by Solo. */
	public static final String PARAM_ITEMINDEX = "itemindex";
	/** The Property key for a "side" parameter used by Solo. */
	public static final String PARAM_SIDE = "side";
	/** The Property key for a "key" parameter used by Solo. */
	public static final String PARAM_KEY = "key";
	/** The Property key for a "orientation" parameter used by Solo. */
	public static final String PARAM_ORIENTATION = "orientation";
	/** The Property key for a "year" parameter used by Solo. */
	public static final String PARAM_YEAR = "year";
	/** The Property key for a "yearmonth" parameter used by Solo. */
	public static final String PARAM_YEARMONTH = "yearmonth";
	/** The Property key for a "monthday" parameter used by Solo. */
	public static final String PARAM_MONTHDAY = "monthday";
	/** The Property key for a "hour" parameter used by Solo. */
	public static final String PARAM_HOUR = "hour";
	/** The Property key for a "minute" parameter used by Solo. */
	public static final String PARAM_MINUTE = "minute";
	/** The Property key for a "progress" parameter used by Solo. */
	public static final String PARAM_PROGRESS = "progress";
	/** The Property key for a "status" parameter used by Solo. */
	public static final String PARAM_STATUS = "status";
	/** The Property key for an "object" parameter (an instance of a class) used by Solo. */
	public static final String PARAM_OBJECT = "object_instance";
	/** The Property key for a "conditiondefinition" parameter used by Solo. */
	public static final String PARAM_CONDITION_DEFINITION = "conditiondefinition";
	/** The Property key for a "regexstring" parameter used by Solo. */
	public static final String PARAM_REGEX_STRING = "regexstring";

	/** The Property key for a "immediately" parameter used by Solo. */
	public static final String PARAM_IMMEDIATELY = "immediately";
	/** The Property key for a "quality" parameter used by Solo. */
	public static final String PARAM_QUALITY = "quality";
	/** The Property key for a "yaxisfirst" parameter used by Solo. */
	public static final String PARAM_YAXISFIRST = "yaxisfirst";
	
	public static final String NULL_VALUE = "NULL";
	
	/** "assertcurrentactivityname" */
	public static final String cmd_assertcurrentactivityname = "assertcurrentactivityname";
	
	/** "assertcurrentactivityclass" */
	public static final String cmd_assertcurrentactivityclass = "assertcurrentactivityclass";
	
	/** "assertmemorynotlow" */
	public static final String cmd_assertmemorynotlow = "assertmemorynotlow";
	
	/** "assertnewcurrentactivityname" */
	public static final String cmd_assertnewcurrentactivityname = "assertnewcurrentactivityname";

	/** "assertnewcurrentactivityclass" */
	public static final String cmd_assertnewcurrentactivityclass = "assertnewcurrentactivityclass";

	/** "clickonactionbarhomebutton" */
	public static final String cmd_clickonactionbarhomebutton = "clickonactionbarhomebutton";

	/** "clickonactionbaritem" */
	public static final String cmd_clickonactionbaritem = "clickonactionbaritem";

	/** "clickonscreen" */
	public static final String cmd_clickonscreen = "clickonscreen";

	/** "clicklongonscreen" */
	public static final String cmd_clicklongonscreen = "clicklongonscreen";

	/** "clicklongtimeonscreen" */
	public static final String cmd_clicklongtimeonscreen = "clicklongtimeonscreen";

	/** "clickonbutton" */
	public static final String cmd_clickonbutton = "clickonbutton";

	/** "clickonbuttonindex" */
	public static final String cmd_clickonbuttonindex = "clickonbuttonindex";

	/** "clickonradiobuttonindex" */
	public static final String cmd_clickonradiobuttonindex = "clickonradiobuttonindex";

	/** "clickonimagebutton" */
	public static final String cmd_clickonimagebutton = "clickonimagebutton";

	/** "clickontogglebutton" */
	public static final String cmd_clickontogglebutton = "clickontogglebutton";

	/** "clickoncheckboxindex" */
	public static final String cmd_clickoncheckboxindex = "clickoncheckboxindex";

	/** "clickonedittextindex" */
	public static final String cmd_clickonedittextindex = "clickonedittextindex";

	/** "clickinlist" */
	public static final String cmd_clickinlist = "clickinlist";

	/** "clickinlistindex" */
	public static final String cmd_clickinlistindex = "clickinlistindex";

	/** "clicklonginlist" */
	public static final String cmd_clicklonginlist = "clicklonginlist";

	/** "clicklonginlistindex" */
	public static final String cmd_clicklonginlistindex = "clicklonginlistindex";

	/** "clicklongtimeinlistindex" */
	public static final String cmd_clicklongtimeinlistindex = "clicklongtimeinlistindex";

	/** "clickonmenuitem" */
	public static final String cmd_clickonmenuitem = "clickonmenuitem";

	/** "clickonsubmenuitem" */
	public static final String cmd_clickonsubmenuitem = "clickonsubmenuitem";

	/** "clickonview" */
	public static final String cmd_clickonview = "clickonview";

	/** "clicklongonview" */
	public static final String cmd_clicklongonview = "clicklongonview";

	/** "clicklongtimeonview" */
	public static final String cmd_clicklongtimeonview = "clicklongtimeonview";

	/** "clickontext" */
	public static final String cmd_clickontext = "clickontext";

	/** "clickontextmatch" */
	public static final String cmd_clickontextmatch = "clickontextmatch";

	/** "clickontextmatchscroll" */
	public static final String cmd_clickontextmatchscroll = "clickontextmatchscroll";

	/** "clicklongontext" */
	public static final String cmd_clicklongontext = "clicklongontext";

	/** "clicklongontextmatch" */
	public static final String cmd_clicklongontextmatch = "clicklongontextmatch";

	/** "clicklongontextmatchscroll" */
	public static final String cmd_clicklongontextmatchscroll = "clicklongontextmatchscroll";

	/** "clicklongtimeontextmatch" */
	public static final String cmd_clicklongtimeontextmatch = "clicklongtimeontextmatch";

	/** "clicklongpressontext" */
	public static final String cmd_clicklongpressontext = "clicklongpressontext";

	/** "clearedittextindex" */
	public static final String cmd_clearedittextindex = "clearedittextindex";
	
	/** "clearedittextreference" */
	public static final String cmd_clearedittextreference = "clearedittextreference";
	
	/** "clickonimage" */
	public static final String cmd_clickonimage = "clickonimage";
	
	/** "drag" */
	public static final String cmd_drag = "drag";

	/** "entertextindex" */
	public static final String cmd_entertextindex = "entertextindex";
	
	/** "entertextreference" */
	public static final String cmd_entertextreference = "entertextreference";
	
	/** "finishopenedactivities" */
	public static final String cmd_finishopenedactivities = "finishopenedactivities";
	
	/** "finalizeremotesolo" */
	public static final String cmd_finalizeremotesolo = "finalizeremotesolo";
	
	/** "goback" */
	public static final String cmd_goback = "goback";
	
	/** "getstring" */
	public static final String cmd_getstring = "getstring";

	/** "getactivitymonitor" */
	public static final String cmd_getactivitymonitor = "getactivitymonitor";

	/** "getcurrentactivity" */
	public static final String cmd_getcurrentactivity = "getcurrentactivity";

	/** "getbacktoactivity" */
	public static final String cmd_gobacktoactivity = "gobacktoactivity";

	/** "getallopenactivities" */
	public static final String cmd_getallopenactivities = "getallopenactivities";

	/** "getedittext" */
	public static final String cmd_getedittext = "getedittext";
	
	/** "getbutton" */
	public static final String cmd_getbutton = "getbutton";
	
	/** "gettext" */
	public static final String cmd_gettext = "gettext";
	
	/** "getimage" */
	public static final String cmd_getimage = "getimage";
	
	/** "getimagebutton" */
	public static final String cmd_getimagebutton = "getimagebutton";
	
	/** "gettexttext" */
	public static final String cmd_gettexttext = "gettexttext";
	
	/** "gettextvisible" */
	public static final String cmd_gettextvisible = "gettextvisible";
	
	/** "getbuttontext" */
	public static final String cmd_getbuttontext = "getbuttontext";
	
	/** "getbuttonvisible" */
	public static final String cmd_getbuttonvisible = "getbuttonvisible";
	
	/** "getedittexttext" */
	public static final String cmd_getedittexttext = "getedittexttext";
	
	/** "getedittextvisible" */
	public static final String cmd_getedittextvisible = "getedittextvisible";
	
	/** "getviewid" */
	public static final String cmd_getviewid = "getviewid";
	
	/** "getviewclass" */
	public static final String cmd_getviewclass = "getviewclass";
	
	/** "getcurrentviews" */
	public static final String cmd_getcurrentviews = "getcurrentviews";
	
	/** "getcurrentimageviews" */
	public static final String cmd_getcurrentimageviews = "getcurrentimageviews";
	
	/** "getcurrentedittexts" */
	public static final String cmd_getcurrentedittexts = "getcurrentedittexts";
	
	/** "getcurrentlistviews" */
	public static final String cmd_getcurrentlistviews = "getcurrentlistviews";
	
	/** "getcurrentscrollviews" */
	public static final String cmd_getcurrentscrollviews = "getcurrentscrollviews";
	
	/** "getcurrentspinners" */
	public static final String cmd_getcurrentspinners = "getcurrentspinners";
	
	/** "getcurrenttextviews" */
	public static final String cmd_getcurrenttextviews = "getcurrenttextviews";
	
	/** "getcurrentgridviews" */
	public static final String cmd_getcurrentgridviews = "getcurrentgridviews";
	
	/** "getcurrentbuttons" */
	public static final String cmd_getcurrentbuttons = "getcurrentbuttons";
	
	/** "getcurrenttogglebuttons" */
	public static final String cmd_getcurrenttogglebuttons = "getcurrenttogglebuttons";
	
	/** "getcurrentradiobuttons" */
	public static final String cmd_getcurrentradiobuttons = "getcurrentradiobuttons";
	
	/** "getcurrentcheckboxes" */
	public static final String cmd_getcurrentcheckboxes = "getcurrentcheckboxes";
	
	/** "getcurrentimagebuttons" */
	public static final String cmd_getcurrentimagebuttons = "getcurrentimagebuttons";
	
	/** "getcurrentdatepickers" */
	public static final String cmd_getcurrentdatepickers = "getcurrentdatepickers";
	
	/** "getcurrenttimepickers" */
	public static final String cmd_getcurrenttimepickers = "getcurrenttimepickers";
	
//	/** "getcurrentnumberpickers" */
//	public static final String cmd_getcurrentnumberpickers = "getcurrentnumberpickers";
	
	/** "getcurrentslidingdrawers" */
	public static final String cmd_getcurrentslidingdrawers = "getcurrentslidingdrawers";
	
	/** "getcurrentprogressbars" */
	public static final String cmd_getcurrentprogressbars = "getcurrentprogressbars";

	/** "getguiimage" */
	public static final String cmd_getguiimage = "getguiimage";	
	
	/** "getviewclassname" */
	public static final String cmd_getviewclassname = "getviewclassname";
	
	/** "getobjectclassname" */
	public static final String cmd_getobjectclassname = "getobjectclassname";	

	/** "gettopparent" */
	public static final String cmd_gettopparent = "gettopparent";
	
	/** "getparentviews" */
	public static final String cmd_getparentviews = "getparentviews";
	
	/** "getviews" */
	public static final String cmd_getviews = "getviews";
	
	/** "isradiobuttonchecked" */
	public static final String cmd_isradiobuttonchecked = "isradiobuttonchecked";
	
	/** "isradiobuttoncheckedtext" */
	public static final String cmd_isradiobuttoncheckedtext = "isradiobuttoncheckedtext";
	
	/** "ischeckboxchecked" */
	public static final String cmd_ischeckboxchecked = "ischeckboxchecked";
	
	/** "ischeckboxcheckedtext" */
	public static final String cmd_ischeckboxcheckedtext = "ischeckboxcheckedtext";
	
	/** "istogglebuttonchecked" */
	public static final String cmd_istogglebuttonchecked = "istogglebuttonchecked";
	
	/** "istogglebuttoncheckedtext" */
	public static final String cmd_istogglebuttoncheckedtext = "istogglebuttoncheckedtext";
	
	/** "istextchecked" */
	public static final String cmd_istextchecked = "istextchecked";
	
	/** "isspinnertextselected" */
	public static final String cmd_isspinnertextselected = "isspinnertextselected";
	
	/** "isspinnertextselectedindex" */
	public static final String cmd_isspinnertextselectedindex = "isspinnertextselectedindex";
	
	/** "pressmenuitem" */
	public static final String cmd_pressmenuitem = "pressmenuitem";

	/** "presssubmenuitem" */
	public static final String cmd_presssubmenuitem = "presssubmenuitem";

	/** "pressspinneritem" */
	public static final String cmd_pressspinneritem = "pressspinneritem";

	/** "scrolldown" */
	public static final String cmd_scrolldown = "scrolldown";
	
	/** "scrollup" */
	public static final String cmd_scrollup = "scrollup";
	
	/** "scrolldownlist" */
	public static final String cmd_scrolldownlist = "scrolldownlist";

	/** "scrolldownlistuid" */
	public static final String cmd_scrolldownlistuid = "scrolldownlistuid";
	
	/** "scrolluplist" */
	public static final String cmd_scrolluplist = "scrolluplist";
	
	/** "scrolluplistuid" */
	public static final String cmd_scrolluplistuid = "scrolluplistuid";
	
	/** "scrolllisttotop" */
	public static final String cmd_scrolllisttotop = "scrolllisttotop";
	
	/** "scrolllisttotopuid" */
	public static final String cmd_scrolllisttotopuid = "scrolllisttotopuid";
	
	/** "scrolllisttobottom" */
	public static final String cmd_scrolllisttobottom = "scrolllisttobottom";
	
	/** "scrolllisttobottomuid" */
	public static final String cmd_scrolllisttobottomuid = "scrolllisttobottomuid";
	
	/** "scrolllisttoline" */
	public static final String cmd_scrolllisttoline = "scrolllisttoline";
	
	/** "scrolllisttolineuid" */
	public static final String cmd_scrolllisttolineuid = "scrolllisttolineuid";
	
	/** "scrolltoside" */
	public static final String cmd_scrolltoside = "scrolltoside";
	
	/** "scrolltotop" */
	public static final String cmd_scrolltotop = "scrolltotop";
	
	/** "scrolltobottom" */
	public static final String cmd_scrolltobottom = "scrolltobottom";
	
	/** "scrolltobottomuid" */
	public static final String cmd_scrolltobottomuid = "scrolltobottomuid";
	
	/** "scrollviewtoside" */
	public static final String cmd_scrollviewtoside = "scrollviewtoside";
	
	/** "searchbutton" */
	public static final String cmd_searchbutton = "searchbutton";
	
	/** "searchbuttonvisible" */
	public static final String cmd_searchbuttonvisible = "searchbuttonvisible";
	
	/** "searchbuttonmatch" */
	public static final String cmd_searchbuttonmatch = "searchbuttonmatch";
	
	/** "searchbuttonmatchvisible" */
	public static final String cmd_searchbuttonmatchvisible = "searchbuttonmatchvisible";
	
	/** "searchedittext" */
	public static final String cmd_searchedittext = "searchedittext";
	
	/** "searchtext" */
	public static final String cmd_searchtext = "searchtext";
	
	/** "searchtextvisible" */
	public static final String cmd_searchtextvisible = "searchtextvisible";
	
	/** "searchtextmatch" */
	public static final String cmd_searchtextmatch = "searchtextmatch";
	
	/** "searchtextmatchscroll" */
	public static final String cmd_searchtextmatchscroll = "searchtextmatchscroll";
	
	/** "searchtextmatchscrollvisible" */
	public static final String cmd_searchtextmatchscrollvisible = "searchtextmatchscrollvisible";
	
	/** "searchtogglebutton" */
	public static final String cmd_searchtogglebutton = "searchtogglebutton";
	
	/** "searchtogglebuttonmatch" */
	public static final String cmd_searchtogglebuttonmatch = "searchtogglebuttonmatch";
	
	/** "setactivityorientation" */
	public static final String cmd_setactivityorientation = "setactivityorientation";
	
	/** "setdatepickerreference" */
	public static final String cmd_setdatepickerreference = "setdatepickerreference";
	
	/** "setdatepickerindex" */
	public static final String cmd_setdatepickerindex = "setdatepickerindex";
	
	/** "settimepickerreference" */
	public static final String cmd_settimepickerreference = "settimepickerreference";
	
	/** "settimepickerindex" */
	public static final String cmd_settimepickerindex = "settimepickerindex";
	
	/** "setprogressbarreference" */
	public static final String cmd_setprogressbarreference = "setprogressbarreference";
	
	/** "setprogressbarindex" */
	public static final String cmd_setprogressbarindex = "setprogressbarindex";
	
	/** "setslidingdrawerreference" */
	public static final String cmd_setslidingdrawerreference = "setslidingdrawerreference";
	
	/** "setslidingdrawerindex" */
	public static final String cmd_setslidingdrawerindex = "setslidingdrawerindex";
	
	/** "sendkey" */
	public static final String cmd_sendkey = "sendkey";
	
	/** "sleep" */
	public static final String cmd_sleep = "sleep";
	
	/** "startmainlauncher" */
	public static final String cmd_startmainlauncher = "startmainlauncher";
	
	/** "startscreenshotsequence" */
	public static final String cmd_startscreenshotsequence = "startscreenshotsequence";
	
	/** "startscreenshotsequencemax" */
	public static final String cmd_startscreenshotsequencemax = "startscreenshotsequencemax";
	
	/** "stopscreenshotsequence" */
	public static final String cmd_stopscreenshotsequence = "stopscreenshotsequence";

	/** "getscreenshotsequence" */
	public static final String cmd_getscreenshotsequence = "getscreenshotsequence";
	
	/** "getscreenshotsequencesize" */
	public static final String cmd_getscreenshotsequenceszie = "getscreenshotsequencesize";
	
	/** "getscreenshotsequenceindex" */
	public static final String cmd_getscreenshotsequenceindex = "getscreenshotsequenceindex";
	
	/** "takescreenshot" */
	public static final String cmd_takescreenshot = "takescreenshot";
	
	/** "typetext" */
	public static final String cmd_typetext = "typetext";
	
	/** "typetextuid" */
	public static final String cmd_typetextuid = "typetextuid";
	
	/** "waitforactivity" */
	public static final String cmd_waitforactivity = "waitforactivity";

	/** "waitforactivitytimeout" */
	public static final String cmd_waitforactivitytimeout = "waitforactivitytimeout";

	/** "waitfordialogtoclose" */
	public static final String cmd_waitfordialogtoclose = "waitfordialogtoclose";

	/** "waitforfragmentbyid" */
	public static final String cmd_waitforfragmentbyid = "waitforfragmentbyid";

	/** "waitforfragmentbytag" */
	public static final String cmd_waitforfragmentbytag = "waitforfragmentbytag";

	/** "waitforlogmessage" */
	public static final String cmd_waitforlogmessage = "waitforlogmessage";

	/** "waitfortext" */
	public static final String cmd_waitfortext = "waitfortext";

	/** "waitfortextmatchtimeout" */
	public static final String cmd_waitfortextmatchtimeout = "waitfortextmatchtimeout";

	/** "waitfortextmatchtimeoutscroll" */
	public static final String cmd_waitfortextmatchtimeoutscroll = "waitfortextmatchtimeoutscroll";

	/** "waitfortextmatchtimeoutscrollvisible" */
	public static final String cmd_waitfortextmatchtimeoutscrollvisible = "waitfortextmatchtimeoutscrollvisible";

	/** "waitforviewclass" */
	public static final String cmd_waitforviewclass = "waitforviewclass";

	/** "waitforviewclassmatchtimeout" */
	public static final String cmd_waitforviewclassmatchtimeout = "waitforviewclassmatchtimeout";

	/** "waitforviewclassmatchtimeoutscroll" */
	public static final String cmd_waitforviewclassmatchtimeoutscroll = "waitforviewclassmatchtimeoutscroll";

	/** "waitforviewreference" */
	public static final String cmd_waitforviewreference = "waitforviewreference";

	/** "waitforviewreferencetimeoutscroll" */
	public static final String cmd_waitforviewreferencetimeoutscroll = "waitforviewreferencetimeoutscroll";
	
	/** "getscreensize" */
	public static final String cmd_getscreensize = "getscreensize";
	
	/** "getviewlocation" */
	public static final String cmd_getviewlocation = "getviewlocation";
	
	/** "gettextviewvalue" */
	public static final String cmd_gettextviewvalue = "gettextviewvalue";
	
	/**============================= Support for Robotium 4.x Begin ==============================================*/
	
	/** "cleartextinwebelement" */
	public static final String cmd_cleartextinwebelement = "cleartextinwebelement";

	/** "clickonviewimmediately" clickOnView(View, boolean) */
	public static final String cmd_clickonviewimmediately = "clickonviewimmediately";
	
	/** "clickonwebelement" clickOnWebElement(By)*/
	public static final String cmd_clickonwebelement = "clickonwebelement";
	
	/** "clickonwebelementindex"  clickOnWebElement(By, int)
	 * if multiple objects match, index determines which one to click */
	public static final String cmd_clickonwebelementindex = "clickonwebelementindex";
	
	/** "clickonwebelementindexscroll" clickOnWebElement(By, int, boolean)
	* if multiple objects match, index determines which one to click
	* if scrolling should be performed */
	public static final String cmd_clickonwebelementindexscroll = "clickonwebelementindexscroll";
	
	/** "clickonwebelementuid" clickOnWebElement(WebElement)*/
	public static final String cmd_clickonwebelementuid = "clickonwebelementuid";
	
	/** "entertextinwebelement" enterTextInWebElement(By, String)*/
	public static final String cmd_entertextinwebelement = "entertextinwebelement";
	
	/** "getcurrentviewsbyclass" getCurrentViews(Class<T>)*/
	public static final String cmd_getcurrentviewsbyclass = "getcurrentviewsbyclass";
	
	/** "getcurrentviewsbyclassandparent" getCurrentViews(Class<T>, View)*/
	public static final String cmd_getcurrentviewsbyclassandparent = "getcurrentviewsbyclassandparent";
	
	/** "getcurrentwebelements" getCurrentWebElements()*/
	public static final String cmd_getcurrentwebelements = "getcurrentwebelements";
	
	/** "getcurrentwebelementsby" getCurrentWebElements(By)*/
	public static final String cmd_getcurrentwebelementsby = "getcurrentwebelementsby";
	
	/** "getwebelement" getWebElement(By, int)*/
	public static final String cmd_getwebelement = "getwebelement";
	
	/** "getweburl" getWebUrl()*/
	public static final String cmd_getweburl = "getweburl";

	/** "hidesoftkeyboard" hideSoftKeyboard()*/
	public static final String cmd_hidesoftkeyboard = "hidesoftkeyboard";

	/** "takescreenshotquality" takeScreenshot(String, int)*/
	public static final String cmd_takescreenshotquality = "takescreenshotquality";
	
	/** "typetextinwebelement" typeTextInWebElement(By, String)*/
	public static final String cmd_typetextinwebelement = "typetextinwebelement";
	
	/** "typetextinwebelementindex" typeTextInWebElement(By, String, int)*/
	public static final String cmd_typetextinwebelementindex = "typetextinwebelementindex";
	
	/** "typetextinwebelementuid" typeTextInWebElement(WebElement, String)*/
	public static final String cmd_typetextinwebelementuid = "typetextinwebelementuid";

	/** "waitforactivitybyclass" waitForActivity(Class<? extends Activity>)*/
	public static final String cmd_waitforactivitybyclass = "waitforactivitybyclass";

	/** "waitforactivitybyclasstimeout" waitForActivity(Class<? extends Activity>, int)*/
	public static final String cmd_waitforactivitybyclasstimeout = "waitforactivitybyclasstimeout";

	/** "waitforcondition" waitForCondition(Condition, int)*/
	public static final String cmd_waitforcondition = "waitforcondition";
	
	/** "waitfordialogtoopen" waitForDialogToOpen(long)*/
	public static final String cmd_waitfordialogtoopen = "waitfordialogtoopen";

	/** "waitforwebelement" waitForWebElement(By)*/
	public static final String cmd_waitforwebelement = "waitforwebelement";

	/** "waitforwebelementtimeout" waitForWebElement(By, int, boolean)*/
	public static final String cmd_waitforwebelementtimeout = "waitforwebelementtimeout";

	/** "waitforwebelementminmatchtimeout" waitForWebElement(By, int, int, boolean)*/
	public static final String cmd_waitforwebelementminmatchtimeout = "waitforwebelementminmatchtimeout";

	/** "pinchtozoom" pinchToZoom(PointF, PointF, PointF, PointF)*/
	public static final String cmd_pinchtozoom = "pinchtozoom";

	/** "rotatelarge" rotateLarge(PointF, PointF)*/
	public static final String cmd_rotatelarge = "rotatelarge";

	/** "rotatesmall" rotateSmall(PointF, PointF)*/
	public static final String cmd_rotatesmall = "rotatesmall";
	
	/** "swipe" swipe(PointF, PointF, PointF, PointF)*/
	public static final String cmd_swipe = "swipe";
	
	/** "clickonscreenntimes" clickOnScreen(float , float , int )*/
	public static final String cmd_clickonscreenntimes = "clickonscreenntimes";
	
	/** "waitforviewid" waitForView(int )*/
	public static final String cmd_waitforviewid = "waitforviewid";
	
	/** "waitforviewidtimeout" waitForView(int , int, int)*/
	public static final String cmd_waitforviewidtimeout = "waitforviewidtimeout";
	
	/** "waitforviewidtimeoutscroll" waitForView(int , int, int , boolean )*/
	public static final String cmd_waitforviewidtimeoutscroll = "waitforviewidtimeoutscroll";
	
	/** "getviewbyname" getView(String )*/
	public static final String cmd_getviewbyname = "getviewbyname";
	
	/** "getviewbynamematch" getView(String , int )*/
	public static final String cmd_getviewbynamematch = "getviewbynamematch";
	
	/** "clearlog" clearLog()*/
	public static final String cmd_clearlog = "clearlog";
	
	/** "utilsfilterviews" filterViews(Class<T>, Iterable<?>)*/
	public static final String cmd_utilsfilterviews = "utilsfilterviews";
	
	/** "utilsfilterviewsbytext" filterViewsByText(Iterable<T>, Pattern)*/
	public static final String cmd_utilsfilterviewsbytext = "utilsfilterviewsbytext";
	
	/** "utilsfilterviewstoset" filterViewsToSet(Class<View>[], Iterable<View>)*/
	public static final String cmd_utilsfilterviewstoset = "utilsfilterviewstoset";
	
	/** "utilsgetnumberofmatches" getNumberOfMatches(String, TextView, Set<TextView>)*/
	public static final String cmd_utilsgetnumberofmatches = "utilsgetnumberofmatches";
	
	/** "utilsremoveinvisibleviews" removeInvisibleViews(Iterable<T>)*/
	public static final String cmd_utilsremoveinvisibleviews = "utilsremoveinvisibleviews";
	
	/** "utilssortviewsbylocationonscreen" filterViews(Class<T>, Iterable<?>)*/
	public static final String cmd_utilssortviewsbylocationonscreen = "utilssortviewsbylocationonscreen";
	
	/** "utilssortviewsbylocationonscreenyfirst" sortViewsByLocationOnScreen(List<? extends View>, boolean)*/
	public static final String cmd_utilssortviewsbylocationonscreenyfirst = "utilssortviewsbylocationonscreenyfirst";
	
	/** "getlargetimeout" getLargeTimeout()*/
	public static final String cmd_getlargetimeout = "getlargetimeout";
	
	/** "setlargetimeout" setLargeTimeout(int)*/
	public static final String cmd_setlargetimeout = "setlargetimeout";

	/** "getsmalltimeout" getSmallTimeout()*/
	public static final String cmd_getsmalltimeout = "getsmalltimeout";
	
	/** "setsmalltimeout" setSmallTimeout(int)*/
	public static final String cmd_setsmalltimeout = "setsmalltimeout";
	
	/**============================= Support for Robotium 4.x End ==============================================*/
	
	/** method_by_xxx represents the static method name of com.jayway.android.robotium.remotecontrol.By class */
	public static final String method_by_id 			= "id";
	public static final String method_by_xpath 			= "xpath";
	public static final String method_by_cssSelector 	= "cssSelector";
	public static final String method_by_name 			= "name";
	public static final String method_by_className 		= "className";
	public static final String method_by_textContent 	= "textContent";
	public static final String method_by_tagName 		= "tagName";

	/**
	 * Encode the object to a string with charset {@value #KEY_UTF8_CHARSET}.<br>
	 * 
	 * @param object, Object an object must implements interface Serializable.
	 * @return	String, the base64 encoded string with charset {@value #KEY_UTF8_CHARSET}
	 * @throws IllegalThreadStateException
	 * @see {@link #decodeBase64Object(String)}
	 */
	public static String encodeBase64Object(Object object) throws IllegalThreadStateException{
		return Base64Encoder.encodeBase64Object(object, KEY_UTF8_CHARSET);
	}
	
	/**
	 * Decode a string to object with charset {@value #KEY_UTF8_CHARSET}.<br>
	 * 
	 * @param base64String, String, the base64 encoded string with charset {@value #KEY_UTF8_CHARSET}
	 * @return Object, an object who implements interface Serializable.
	 * @throws IllegalThreadStateException
	 * @see {@link #encodeBase64Object(Object)}
	 */
	public static Object decodeBase64Object(String base64String) throws IllegalThreadStateException{
		return Base64Decoder.decodeBase64Object(base64String, KEY_UTF8_CHARSET);
	}
	
	/** Array of possible separators: ";",":","|",":","_","#","!","$","^","&","*","~" */
	static final String[] SEPS = {";",":","|",":","_","#","!","$","^","&","*","~"};
	
	/**
	 * Return a usable single character separator string that does NOT exist 
	 * in the provided field.
	 * Tries each character in SEPS array.
	 * @param afield String field to keep intact.
	 * @return unique String separator that does NOT exist in afield.
	 */
	public static String getUniqueSeparator(String afield){
		String debugPrefix = "Message.getUniqueSeparator() ";

		for(int d=0;d<SEPS.length;d++){
			if(afield.indexOf(SEPS[d])< 0) return SEPS[d];
		}
		
		System.err.println(debugPrefix+"Separator options exhausted for:"+ afield);

		return null;		
	}

	/**
	 * Convert the array to a single string of separated values. The first
	 * character in the returned string defines the delimiter used to separate
	 * the items. If there are no items in the provided array (length==0) then
	 * we return a 0-length (empty) String.
	 * 
	 * @param items
	 * @return character delimited String of fields
	 * @see #getUniqueSeparator(String)
	 */
	public static String convertToDelimitedString(String[] items) throws IllegalThreadStateException{
		String debugPrefix = "Message.convertToDelimitedString() ";
		StringBuffer result = new StringBuffer();
		
		if(items==null || items.length==0){
			System.err.println(debugPrefix+" The array items is null or 0-length!");
			return ""; 
		}
		
		for (int i = 0; i < items.length; i++) {
			result.append(items[i]);
		}
		String separator = getUniqueSeparator(result.toString());

		if(separator==null){
			String message = "Can't deduce a delimiter!!! Add more delimiters to array: "+SEPS;
			System.err.println(debugPrefix+message);
			throw new IllegalThreadStateException(message); 
		}else{
			result.delete(0, result.length());
			for (int i = 0; i < items.length; i++) {
				result.append(separator + items[i]);
			}
		}
		return result.toString();
	}
	
	/**
	 * @param UIDList ArrayList, a list of UID<br>
	 *  
	 * @return  String, a string of format: ";UID;UID;UID", The first character is the delimiter 
	 *          used to delimit each item followed by each item separated by the delimiter.
	 */
	public static final String convertToDelimitedString(List<String> UIDList) throws IllegalThreadStateException{
		if(UIDList != null && UIDList.size() > 0){
			return convertToDelimitedString(UIDList.toArray(new String[0]));				
		}else{
			return "";
		}
	}
	
	/**
	 * @param text expected format: ";UID;UID;UID"<br>
	 * The first character is the delimiter used to delimit each item followed by each item separated by the delimiter. 
	 * @return  ArrayList with 0 or more String elements.  It is possible that 0-length String elements might exist 
	 * in the returned ArrayList.
	 */
	public static final ArrayList<String> parseStringArrayList(String text){
		ArrayList<String> list = new ArrayList<String>(0);
		if(text != null && text.length() > 1){
			String sep = String.valueOf(text.charAt(0));
			String uids = text.substring(1);
			String[] ids = uids.split(sep);
			for(int i = 0; i<ids.length;i++) {
				//test for null might be over protective :)
				list.add(ids[i] != null ? ids[i]: "");
			}				
		}
		return list;
	}
}