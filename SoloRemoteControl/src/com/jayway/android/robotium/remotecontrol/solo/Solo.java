/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.solo;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import org.safs.android.auto.lib.AndroidTools;
import org.safs.android.auto.lib.DUtilities;
import org.safs.android.auto.lib.Process2;
import org.safs.sockets.RemoteException;
import org.safs.sockets.ShutdownInvocationException;
import org.w3c.tools.codec.Base64Decoder;
import org.w3c.tools.codec.Base64FormatException;

import com.jayway.android.robotium.remotecontrol.By;
import com.jayway.android.robotium.remotecontrol.Condition;
import com.jayway.android.robotium.remotecontrol.ObjectCollection;
import com.jayway.android.robotium.remotecontrol.PointF;

/**
 * Provides a remote control API to the embedded Robotium Solo class on the device.
 * <p>
 * It is important to note that the Remote Control Solo class is always running in a process that is outside 
 * of the device or emulator.  Thus, the API (and your program) do not have direct access to the actual Solo 
 * or AUT objects like when running the traditional Robotium Solo on the device.
 * <p>
 * For this reason, we generally receive and use String unique ID (UID) references to these objects when the original 
 * Solo API calls for a reference to an actual Android object like a View, Button, EditText, etc...
 * <p>
 * Additionally, the embedded Robotium Solo class is always running in the context of an Android jUnit test.  
 * That means a traditional Robotium test will generally issue (and stop) on Error any time a jUnit Assert fails.
 * <p>  
 * That is not the case with Robotium Remote Control.
 * <p>
 * We cannot have the embedded test engine using Robotium Solo abort on Error because no information would get 
 * back to Robotium Remote Control.  Instead, the embedded remote control client must capture and report all 
 * such Errors to Robotium Remote Control and remain running until commanded to shutdown.
 * <p>
 * By default, Robotium Remote Control is NOT implemented as jUnit tests.  It does not abort or stop upon a test 
 * failure.  This allows Robotium Remote Control to be used in many different types of testing frameworks including, 
 * but not exclusive to, jUnit.  A future subclass of this Solo class and API can provide that same 
 * jUnit support, if needed.
 * <p>
 * Default usage:
 * <p><pre>
 * Solo solo = new Solo();
 * try{
 *     solo.setLogsInterface(alog);
 *     solo.initialize();
 *     results = solo.startMainLauncher(errormsg);
 *     (use the API)
 *     solo.finishOpenActivities();
 *     solo.shutdown();
 * }
 * catch(IllegalThreadStateException x){ 
 *    //TODO: handle it
 * }
 * catch(RemoteException x){ 
 *    //TODO: handle it
 * }
 * catch(TimeoutException x){ 
 *    //TODO: handle it
 * }
 * catch(ShutdownInvocationException x){ 
 *    //TODO: handle it
 * }
 * </pre>
 * @see Message
 * @author Carl Nagle, SAS Institute, Inc.
 * @since
 * <br>May 17, 2013		(SBJLWA)	Update to support Robotium 4.1
 * <br>Jun 21, 2013		(SBJLWA)	Update to support Robotium 4.1+
 * <br>Jun 25, 2013		(CANAGL)	Update to support Robotium 4.2
 */
public class Solo extends SoloWorker{

	/** 22 */
	public static final int RIGHT = 22;
	/** 21 */
	public static final int LEFT = 21;	
	/** 19 */
	public static final int UP = 19;
	/** 20 */
	public static final int DOWN = 20;
	/** 66 */
	public static final int ENTER = 66;
	/** 82 */
	public static final int MENU = 82;
	/** 67 */
	public static final int DELETE = 67;
	/** 1 */
	public static final int OPENED = 1;
	/** 0 */
	public static final int CLOSED = 0;
	/** 1 */
	public static final int PORTRAIT = 1;
	/** 0 */
	public static final int LANDSCAPE = 0;
	
	/** Holds the Properties object returned from the last remote control call returning Properties.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 */
	public Properties _last_remote_result;
	
	public Solo() {	super(); }

	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout<br>
	 * This method expects the objects are stored in {@link Message#KEY_REMOTERESULTINFO} in format of ";UID;UID;UID".<br>
	 * @see #getCurrentObjects(Properties)
	 */
	private ArrayList<String> getCurrentObjects(String typecommand) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		return getCurrentObjects(props);
	}

	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout<br>
	 * This method expects the objects are stored in {@link Message#KEY_REMOTERESULTINFO} in format of ";UID;UID;UID".<br>
	 * @see #getCurrentObjects(String)
	 */
	private ArrayList<String> getCurrentObjects(Properties props) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		ArrayList<String> list = new ArrayList<String>();
		_last_remote_result = control.performRemotePropsCommand(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		if(rc==Message.STATUS_REMOTERESULT_OK){
			String info = _last_remote_result.getProperty(Message.KEY_REMOTERESULTINFO);
			list = Message.parseStringArrayList(info);
		}
		return list;
	}
	
	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * @see #getSingleObject(Properties)
	 */
	private String getSingleObject(String typecommand, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		return getSingleObject(props);
	}
	
	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * @see #getSingleObject(Properties)
	 */
	private String getSingleObject(String typecommand, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		props.setProperty(Message.PARAM_TEXT, text);
		return getSingleObject(props);
	}

	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * 
	 * @see #getSingleObject(Properties, int, int, int)
	 */
	private String getSingleObject(Properties props) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
	}	
	
	/**
	 * Runs the specified command with user given timeouts
	 * 
	 * @see #getSingleObject(Properties)
	 */
	private String getSingleObject(Properties props, int ready_stimeout, int running_stimeout, int result_stimeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		String result = null;
		_last_remote_result = control.performRemotePropsCommand(props, ready_stimeout, running_stimeout, result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		if(rc==Message.STATUS_REMOTERESULT_OK){
			result = _last_remote_result.getProperty(Message.KEY_REMOTERESULTINFO);
		}else{
			String errorMsg = _last_remote_result.getProperty(Message.KEY_REMOTERESULTINFO);
			String detailErrorMsg = _last_remote_result.getProperty(Message.PARAM_ERRORMSG);
			if(detailErrorMsg!=null) errorMsg+=" : "+detailErrorMsg;
			debug(" Fail to execute '"+props.getProperty(Message.KEY_COMMAND)+"' due to '"+errorMsg+"'");
		}
		return result;
	}	
	
	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * @see #runBooleanResult(Properties)
	 */
	private boolean runBooleanResultWithIndex(String typecommand, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		return runBooleanResult(props);
	}
	
	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * @see #runBooleanResult(Properties)
	 */
	private boolean runBooleanResultWithText(String typecommand, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		props.setProperty(Message.PARAM_TEXT, text);
		return runBooleanResult(props);
	}

	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * @see #runBooleanResult(Properties)
	 */
	private boolean runBooleanResultWithName(String typecommand, String name) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		props.setProperty(Message.PARAM_NAME, name);
		return runBooleanResult(props);
	}

	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * 
	 * @see #runBooleanResult(Properties)
	 */
	private boolean runBooleanResultWithUID(String typecommand, String uidReference) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		props.setProperty(Message.PARAM_REFERENCE, uidReference);
		return runBooleanResult(props);
	}
	
	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * 
	 * @see #runBooleanResult(Properties)
	 */
	private boolean runBooleanResult(String typecommand) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		return runBooleanResult(props);
	}
	
	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * 
	 * @see #runBooleanResultWithIndex(String, int)
	 * @see #runBooleanResultWithName(String, String)
	 * @see #runBooleanResultWithText(String, String)
	 * @see #runBooleanResultWithUID(String, String)
	 * @see #runBooleanResult(String)
	 */
	private boolean runBooleanResult(Properties props) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResult(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
	}
	
	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 * 
	 * @see #runBooleanResultWithIndex(String, int)
	 * @see #runBooleanResultWithName(String, String)
	 * @see #runBooleanResultWithText(String, String)
	 * @see #runBooleanResultWithUID(String, String)
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 */
	private boolean runBooleanResult(Properties props,
			                         int ready_stimeout,
			                         int running_stimeout,
			                         int result_stimeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		if(props==null) return false;
		_last_remote_result = control.performRemotePropsCommand(props, ready_stimeout,running_stimeout, result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		return rc==Message.STATUS_REMOTERESULT_OK;
	}
	
	/**
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 */
	private boolean runNoArgCommand(String typecommand) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(typecommand);
		_last_remote_result = control.performRemotePropsCommand(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		return rc==Message.STATUS_REMOTERESULT_OK ? true: false;
	}
	
	/**
	 * Sometimes, the Robotium Solo's method will return a boolean value, such as isCheckBoxChecked(int index).
	 * We should not mix this return-value with the suceess of command-execution.
	 * We should return this value through a separate parameter.
	 * If the command-execution fails, RemoteSoloException will be thrown out.
	 * 
	 * @param success		boolean, if the command has been executed successfully on remote-side.
	 * @param command		String, the command that has been executed.
	 * @param boolParam		String, the param sent from the device-side, which contains a boolean value.
	 *                              This param is defined in class {@link Message}
	 * @return
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side.
	 */
	private boolean getRemoteBooleanResult(boolean success, String command, String boolParam) throws RemoteSoloException{
		if(success){
			return Boolean.parseBoolean(_last_remote_result.getProperty(boolParam));
		}else{
			throw new RemoteSoloException("Fail to execute '"+command+"' in remote side.");
		}	
	}
	
	/**
	 * Clears the value of an EditText. 
	 * @param String UID reference to the EditText to clear.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clearedittextreference
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_REFERENCE=String UID reference 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clearEditText(String uidEditText) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithUID(Message.cmd_clearedittextreference, uidEditText);
	}
	
	/**
	 * Clears the value of an EditText. 
	 * @param index of the EditText to be cleared. 0 if only one is available.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clearedittextindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clearEditText(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithIndex(Message.cmd_clearedittextindex, index);
	}
	
	/**
	 * Clears the logcat log. -- <b>Robotium 4.1+ required.</b>.<br>
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clearlog
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String, error message if the command fails.  
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(String)
	 * @see Message
	 */
	public boolean clearLog() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResult(Message.cmd_clearlog);
	}
	
	/**
	 * Clicks on a given list line and returns an ArrayList of String UID references for each TextView object that 
	 * the list line is showing.  Will use the first list it finds.  
	 * @param line that should be clicked.
	 * @return ArrayList of 0 or more String UIDs for all the TextView objects located in the list line.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickinlist
	 * (in ):PARAM_LINE=int
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual TextView objects 
	 * stored in a remote cache.
	 * (out):PARAM_TEXT=String containing the text-value of actual TextView objects stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * <p>
	 * PARAM_TEXT content format: ";text;text;text"
	 * The first character is the delimiter used to delimit each text followed by each text separated by the delimiter. 
	 * 
	 * <p>
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> clickInList(int line) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickinlist);
		props.setProperty(Message.PARAM_LINE, String.valueOf(line));
		
		return getCurrentObjects(props);
	}
	
	/**
	 * Clicks on a given list line on a specified list and returns an ArrayList of String UID references for each TextView object that 
	 * the list line is showing.  
	 * @param line that should be clicked.
	 * @param index of the list. 1 if two lists are available.
	 * @return ArrayList of 0 or more String UIDs for all the TextView objects located in the list line.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickinlistindex
	 * (in ):PARAM_LINE=int
	 * (in ):PARAM_INDEX=int
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual TextView objects 
	 * stored in a remote cache.
	 * (out):PARAM_TEXT=String containing the text-value of actual TextView objects stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * <p>
	 * PARAM_TEXT content format: ";text;text;text"
	 * The first character is the delimiter used to delimit each text followed by each text separated by the delimiter. 
	 * 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> clickInList(int line, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickinlistindex);
		props.setProperty(Message.PARAM_LINE, String.valueOf(line));
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		
		return getCurrentObjects(props);
	}
	
	/**
	 * Long Click on a given list line and returns an ArrayList of String UID references for each TextView object that 
	 * the list line is showing.  Will use the first list it finds.  
	 * @param line that should be clicked.
	 * @return ArrayList of 0 or more String UIDs for all the TextView objects located in the list line.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklonginlist
	 * (in ):PARAM_LINE=int
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual TextView objects 
	 * stored in a remote cache.
	 * (out):PARAM_TEXT=String containing the text-value of actual TextView objects stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * <p>
	 * PARAM_TEXT content format: ";text;text;text"
	 * The first character is the delimiter used to delimit each text followed by each text separated by the delimiter. 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> clickLongInList(int line) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklonginlist);
		props.setProperty(Message.PARAM_LINE, String.valueOf(line));

		return getCurrentObjects(props);
	}
	
	/**
	 * Long click on a given list line on a specified list and returns an ArrayList of String UID references for each TextView object that 
	 * the list line is showing.  
	 * @param line that should be clicked.
	 * @param index of the list. 1 if two lists are available.
	 * @return ArrayList of 0 or more String UIDs for all the TextView objects located in the list line.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklonginlistindex
	 * (in ):PARAM_LINE=int
	 * (in ):PARAM_INDEX=int
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual TextView objects 
	 * stored in a remote cache.
	 * (out):PARAM_TEXT=String containing the text-value of actual TextView objects stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * <p>
	 * PARAM_TEXT content format: ";text;text;text"
	 * The first character is the delimiter used to delimit each text followed by each text separated by the delimiter. 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> clickLongInList(int line, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklonginlistindex);
		props.setProperty(Message.PARAM_LINE, String.valueOf(line));
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));

		return getCurrentObjects(props);
	}
	
	/**
	 * Long click on a given list line on a specified list and returns an ArrayList of String UID references for each TextView object that 
	 * the list line is showing.  
	 * @param line that should be clicked.
	 * @param index of the list. 1 if two lists are available.
	 * @param time in milliseconds to hold the long click.
	 * @return ArrayList of 0 or more String UIDs for all the TextView objects located in the list line.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongtimeinlistindex
	 * (in ):PARAM_LINE=int
	 * (in ):PARAM_INDEX=int
	 * (in ):PARAM_TIME=int
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual TextView objects 
	 * stored in a remote cache.
	 * (out):PARAM_TEXT=String containing the text-value of actual TextView objects stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * <p>
	 * PARAM_TEXT content format: ";text;text;text"
	 * The first character is the delimiter used to delimit each text followed by each text separated by the delimiter. 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> clickLongInList(int line, int index, int time) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklongtimeinlistindex);
		props.setProperty(Message.PARAM_LINE, String.valueOf(line));
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_TIME, String.valueOf(time));

		return getCurrentObjects(props);
	}
	
	/**
	 * Long clicks on a given coordinate on the screen. 
	 * @param float x coordinate
	 * @param float y coordinate
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongonscreen
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_FLOATX=float 
	 * (in ):PARAM_FLOATy=float 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickLongOnScreen(float x, float y) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklongonscreen);
		props.setProperty(Message.PARAM_FLOATX, String.valueOf(x));
		props.setProperty(Message.PARAM_FLOATY, String.valueOf(y));
		return runBooleanResult(props);
	}
	
	/**
	 * Long clicks on a given coordinate on the screen for a specified number of milliseconds. 
	 * @param float x coordinate
	 * @param float y coordinate
	 * @param time in milliseconds to hold the click.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongtimeonscreen
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_FLOATX=float 
	 * (in ):PARAM_FLOATY=float 
	 * (in ):PARAM_TIME=int milliseconds 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickLongOnScreen(float x, float y, int time) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklongtimeonscreen);
		props.setProperty(Message.PARAM_FLOATX, String.valueOf(x));
		props.setProperty(Message.PARAM_FLOATY, String.valueOf(y));
		props.setProperty(Message.PARAM_TIME, String.valueOf(time));
		return runBooleanResult(props);
	}
	
	/**
	 * Long clicks on a given View. Will automatically scroll when needed. {@link #clickOnText(String)} can then 
	 * be used to click on the context menu items that appear after the long click. 
	 * @param String text that should be clicked.  The parameter is interpretted as a regular expression.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongontext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String text 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickLongOnText(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithText(Message.cmd_clicklongontext, text);
	}
	
	/**
	 * Long clicks on a given View. Will automatically scroll when needed. {@link #clickOnText(String)} can then 
	 * be used to click on the context menu items that appear after the long click. 
	 * @param String text that should be clicked.  The parameter is interpretted as a regular expression.
	 * @param match the match of the text that should be clicked.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongontextmatch
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String text 
	 * (in ):PARAM_MATCH=String text 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickLongOnText(String text, int match) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklongontextmatch);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MATCH, String.valueOf(match));
		return runBooleanResult(props);
	}
	
	/**
	 * Long clicks on a given View. {@link #clickOnText(String)} can then 
	 * be used to click on the context menu items that appear after the long click. 
	 * @param String text that should be clicked.  The parameter is interpretted as a regular expression.
	 * @param match the match of the text that should be clicked.
	 * @param scroll true if scrolling should be performed.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongontextmatchscroll
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String text 
	 * (in ):PARAM_MATCH=String text 
	 * (in ):PARAM_SCROLL=boolean 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickLongOnText(String text, int match, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklongontextmatchscroll);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MATCH, String.valueOf(match));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		return runBooleanResult(props);
	}
	
	/**
	 * Long clicks on a given View. {@link #clickOnText(String)} can then 
	 * be used to click on the context menu items that appear after the long click. 
	 * @param String text that should be clicked.  The parameter is interpretted as a regular expression.
	 * @param match the match of the text that should be clicked.
	 * @param time in milliseconds to hold the click.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongtimeontextmatch
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String text 
	 * (in ):PARAM_MATCH=String text 
	 * (in ):PARAM_TIME=int millis 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickLongOnText(String text, int match, int time) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklongtimeontextmatch);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MATCH, String.valueOf(match));
		props.setProperty(Message.PARAM_TIME, String.valueOf(time));
		return runBooleanResult(props);
	}
	
	/**
	 * Long clicks on a given View and then selects an item from the context menu that appears.  
	 * Will automatically scroll when needed. 
	 * @param String text that should be clicked.  The parameter is interpretted as a regular expression.
	 * @param index of the menu item to be pressed. 0 if only one is available.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongpressontext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String text 
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickLongOnTextAndPress(String text, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklongpressontext);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		return runBooleanResult(props);
	}
	
	/**
	 * Long clicks on a given View. 
	 * @param String UID reference to the View that should be long clicked.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongonview
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_REFERENCE=String UID reference 
	 * (out):PARAM_CLASS=String The view's full qualified class name 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickLongOnView(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithUID(Message.cmd_clicklongonview, uidView);
	}
	
	/**
	 * Long clicks on a given View for a specified amount of time. 
	 * @param String UID reference to the View that should be long clicked.
	 * @param time in milliseconds to hold the click.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clicklongtimeonview
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_REFERENCE=String UID reference 
	 * (in ):PARAM_TIME=int 
	 * (out):PARAM_CLASS=String The view's full qualified class name 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickLongOnView(String uidView, int time) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clicklongtimeonview);
		props.setProperty(Message.PARAM_REFERENCE, uidView);
		props.setProperty(Message.PARAM_TIME, String.valueOf(time));
		return runBooleanResult(props);
	}
	
	/**
	 * Clicks on a Button at a given index. 
	 * @param index of the Button to be clicked. 0 if only one is available.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonbuttonindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnButton(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithIndex(Message.cmd_clickonbuttonindex, index);
	}
	
	/**
	 * Clicks on the Action Bar Home button.
	 * Requires Robotium 3.4.1. 
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonactionbarhomebutton
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnActionBarHomeButton() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runNoArgCommand(Message.cmd_clickonactionbarhomebutton);
	}
	
	/**
	 * Clicks on an ActionBar item with a given resource id.
	 * Requires Robotium 3.6.
	 * @param resourceID, the R.id of the ActionBar item.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonactionbaritem
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in ):PARAM_RESID=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickOnActionBarItem(int resourceID) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickonactionbaritem);
		props.setProperty(Message.PARAM_RESID, String.valueOf(resourceID));
		return runBooleanResult(props);
	}	
	
	/**
	 * Clicks on a Button with the given text. Will automatically scroll when needed. 
	 * @param text name of the button presented to the user.  The parameter will be interpretted as a regular expression.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonbutton
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_NAME=String
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnButton(String name) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithName(Message.cmd_clickonbutton, name);
	}
	
	/**
	 * Clicks on a CheckBox at a given index. 
	 * @param index of the CheckBox to be clicked. 0 if only one is available.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickoncheckboxindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnCheckBox(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithIndex(Message.cmd_clickoncheckboxindex, index);
	}
	
	/**
	 * Clicks on an EditText at a given index. 
	 * @param index of the EditText to be clicked. 0 if only one is available.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonedittextindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnEditText(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithIndex(Message.cmd_clickonedittextindex, index);
	}
	
	/**
	 * Clicks on Image at a given index. 
	 * @param index of the Image to be clicked. 0 if only one is available.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonimage
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnImage(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithIndex(Message.cmd_clickonimage, index);
	}
	
	/**
	 * Clicks on a ImageButton at a given index. 
	 * @param index of the ImageButton to be clicked. 0 if only one is available.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonimagebutton
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnImageButton(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithIndex(Message.cmd_clickonimagebutton, index);
	}
	
	/**
	 * Clicks on a menuitem with a given text. 
	 * @param text of the menuitem to be clicked. The parameter will be interpretted as a regular expression.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonmenuitem
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnMenuItem(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithText(Message.cmd_clickonmenuitem, text);
	}
	
	/**
	 * Clicks on a MenuItem with a given text. 
	 * @param text of the menuitem to be clicked. The parameter will be interpretted as a regular expression.
	 * @param submenu true if the menu item could be located in a sub menu.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonsubmenuitem
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String
	 * (in ):PARAM_SUBMENU=boolean 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickOnMenuItem(String text, boolean subMenu) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickonsubmenuitem);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_SUBMENU, String.valueOf(subMenu));
		return runBooleanResult(props);
	}
	
	/**
	 * Clicks on a RadioButton at a given index. 
	 * @param index of the RadioButton to be clicked. 0 if only one is available.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonradiobuttonindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnRadioButton(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithIndex(Message.cmd_clickonradiobuttonindex, index);
	}
	
	/**
	 * Clicks on a given coordinate on the screen. 
	 * @param float x coordinate
	 * @param float y coordinate
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonscreen
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_FLOATX=float 
	 * (in ):PARAM_FLOATY=float 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickOnScreen(float x, float y) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickonscreen);
		props.setProperty(Message.PARAM_FLOATX, String.valueOf(x));
		props.setProperty(Message.PARAM_FLOATY, String.valueOf(y));
		return runBooleanResult(props);
	}
	
	/**
	 * Clicks the specified coordinates rapidly a specified number of times. -- <b>Requires API level >= 14, Robotium 4.1+ required</b>.<br>
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param numberOfClicks the number of clicks to perform
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonscreenntimes
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_FLOATX=float 
	 * (in ):PARAM_FLOATY=float 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 */
	public boolean clickOnScreen(float x, float y, int numberOfClicks) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickonscreenntimes);
		props.setProperty(Message.PARAM_FLOATX, String.valueOf(x));
		props.setProperty(Message.PARAM_FLOATY, String.valueOf(y));
		props.setProperty(Message.PARAM_CLICKNUMBER, String.valueOf(numberOfClicks));
		
		return runBooleanResult(props);
	}
	
	/**
	 * Clicks on a View displaying the given text.  Will automatically scroll when needed. 
	 * @param text that should be clicked. The parameter will be treated like a regular expression.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickontext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnText(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithText(Message.cmd_clickontext, text);
	}
	
	/**
	 * Clicks on a View displaying the given text.  Will automatically scroll when needed. 
	 * @param text that should be clicked.  The parameter will be treated as a regular expression.
	 * @param int match of the text that should be clicked.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickontextmatch
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_MATCH=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickOnText(String text, int match) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickontextmatch);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MATCH, String.valueOf(match));
		return runBooleanResult(props);
	}
	
	/**
	 * Clicks on a View displaying the given text. 
	 * @param text that should be clicked.  The parameter will be treated as a regular expression.
	 * @param int match of the text that should be clicked.
	 * @param scroll true if scrolling should be performed.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickontextmatchscroll
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_MATCH=int 
	 * (in ):PARAM_SCROLL=true/false 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean clickOnText(String text, int match, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickontextmatchscroll);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MATCH, String.valueOf(match));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		return runBooleanResult(props);
	}
	
	/**
	 * Clicks on a ToggleButton displaying the given text. 
	 * @param name of the ToggleButton presented to the user.  The parameter will be treated as a regular expression.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickontogglebutton
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_NAME=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnToggleButton(String name) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithName(Message.cmd_clickontogglebutton, name);
	}
	
	/**
	 * Clicks on the specified View. 
	 * @param String UID reference of the View that should be clicked.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonview
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_REFERENCE=String UID 
	 * (out):PARAM_CLASS=String The view's full qualified class name 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnView(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithUID(Message.cmd_clickonview, uidView);
	}
	
	/**
	 * Clicks on the specified View -- <b>Robotium 4.1 required</b>.<br> 
	 * @param String UID reference of the View that should be clicked.
	 * @param boolean, immediately, if View should be clicked without any wait 
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonviewimmediately
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_REFERENCE=String, UID reference of the View that should be clicked.
	 * (in ):PARAM_IMMEDIATELY=boolean, if View should be clicked without any wait 
	 * (out):PARAM_CLASS=String The view's full qualified class name 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnView(String uidView, boolean immediately) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_clickonviewimmediately);
		props.setProperty(Message.PARAM_REFERENCE, uidView);
		props.setProperty(Message.PARAM_IMMEDIATELY, Boolean.toString(immediately));
		return runBooleanResult(props);
	}
	
	/**
	 * Simulate touching a given location and dragging it to a new location. 
	 * @param fromX coordinate of the initial touch, in screen coordinates.
	 * @param toX coordinate of the drag destination, in screen coordinates.
	 * @param fromY coordinate of the initial touch, in screen coordinates.
	 * @param toY coordinate of the drag destination, in screen coordinates.
	 * @param stepCount How many move steps to include in the drag.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_drag
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_FROMX=float 
	 * (in ):PARAM_TOX=float 
	 * (in ):PARAM_FROMY=float 
	 * (in ):PARAM_TOY=float 
	 * (in ):PARAM_STEPCOUNT=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean drag(float fromX, float toX, float fromY, float toY, int stepCount) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_drag);
		props.setProperty(Message.PARAM_FROMX, String.valueOf(fromX));
		props.setProperty(Message.PARAM_TOX, String.valueOf(toX));
		props.setProperty(Message.PARAM_FROMY, String.valueOf(fromY));
		props.setProperty(Message.PARAM_TOY, String.valueOf(toY));
		props.setProperty(Message.PARAM_STEPCOUNT, String.valueOf(stepCount));
		return runBooleanResult(props);
	}
	
	/**
	 * Enter text into a given EditText.
	 * @param String UID reference for the EditText to enter text into. 
	 * @param text String to enter into the EditText field.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_entertextreference
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_REFERENCE=String UID 
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean enterText(String uidEditText, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_entertextreference);
		props.setProperty(Message.PARAM_REFERENCE, uidEditText);
		props.setProperty(Message.PARAM_TEXT, text);
		return runBooleanResult(props);
	}
	
	/**
	 * Enter text into an EditText with the given index.
	 * @param index of the EditText. 0 if only one is available. 
	 * @param text String to enter into the EditText field.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_entertextindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int 
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean enterText(int index, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_entertextindex);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_TEXT, text);
		return runBooleanResult(props);
	}

	/**
	 * Returns a String UID for the Robotium Solo Activity Monitor.  
	 * Not yet sure if we are going to do anything with it.
	 * @return String UID for ActivityMonitor.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getactivitymonitor
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the stored object 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getActivityMonitor() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getactivitymonitor);
		
		return getSingleObject(props);
	}
	
	/**
	 * Returns a String UID for the Button at the given index. 
	 * @param index of the Button to get.
	 * @return String UID for Button at the given index, or null if index is invalid.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getbutton
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public String getButton(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(Message.cmd_getbutton, index);
	}
	
	/**
	 * Returns a String UID for the Button showing the given text.
	 * @param text that is shown. 
	 * @return String UID for Button showing the given text, or null if not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getbuttontext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * (in ):PARAM_TEXT=String
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public String getButton(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(Message.cmd_getbuttontext, text);
	}
	
	/**
	 * Returns a String UID for the Button showing the given text.
	 * @param text that is shown. 
	 * @param onlyVisible true if only visible buttons on the screen should be returned.
	 * @return String UID for visible Button with the given text, or null if not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getbuttonvisible
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * (in ):PARAM_TEXT=String
	 * (in ):PARAM_ONLYVISIBLE=true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getButton(String text, boolean onlyVisible) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getbuttonvisible);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_ONLYVISIBLE, String.valueOf(onlyVisible));
		
		return getSingleObject(props);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current Buttons in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current Buttons shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentbuttons
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentButtons() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentbuttons);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current CheckBoxes in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current CheckBoxes shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentcheckboxes
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentCheckBoxes() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentcheckboxes);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current DatePickers in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current DatePickers shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentdatepickers
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentDatePickers() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentdatepickers);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current EditTexts in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current EditTexts shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentedittexts
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentEditTexts() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentedittexts);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current GridViews in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current GridViews shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentgridviews
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentGridViews() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentgridviews);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current ImageButtons in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current ImageButtons shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentimagebuttons
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentImageButtons() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentimagebuttons);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current ImageViews in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current ImageViews shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentimageviews
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentImageViews() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentimageviews);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current ListViews in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current ListViews shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentlistviews
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentListViews() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentlistviews);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current ProgressBars in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current ProgressBars shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentprogressbars
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentProgressBars() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentprogressbars);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current RadioButtons in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current RadioButtons shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentradiobuttons
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentRadioButtons() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentradiobuttons);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current ScrollViews in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current ScrollViews shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentscrollviews
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentScrollViews() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentscrollviews);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current SlidingDrawers in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current SlidingDrawers shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentslidingdrawers
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentSlidingDrawers() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentslidingdrawers);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current Spinners in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current Spinners shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentspinners
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentSpinners() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentspinners);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current TextViews in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current TextViews shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrenttextviews
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentTextViews() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrenttextviews);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current TimePickers in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current TimePickers shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrenttimepickers
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentTimePickers() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrenttimepickers);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current NumberPickers in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current NumberPickers shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentnumberpickers
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
//	public ArrayList<String> getCurrentNumberPickers() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
//		return getCurrentObjects(Message.cmd_getcurrentnumberpickers);
//	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current ToggleButtons in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current ToggleButtons shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrenttogglebuttons
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentToggleButtons() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrenttogglebuttons);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current Views in the focused Activity or Dialog. 
	 * @return ArrayList of 0 or more String UIDs for all the current Views shown in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentviews
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getCurrentViews() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentviews);
	}
	
	/**
	 * Returns a String UID for the EditText at the given index.
	 * @param index of the EditText. 0 if only one is available. 
	 * @return String UID for EditText at the given index, or null if index is invalid.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getedittext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public String getEditText(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(Message.cmd_getedittext, index);
	}
	
	/**
	 * Returns a String UID for the EditText item with the given text. 
	 * @param text that is shown
	 * @return String UID for EditText, or null if given text was not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getedittexttext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache. 
	 * (in ):PARAM_TEXT=String
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public String getEditText(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(Message.cmd_getedittexttext, text);
	}
	
	/**
	 * Returns a String UID for the EditText item with the given text. 
	 * @param text that is shown
	 * @param onlyVisible true if only visible EditTexts on the screen should be returned.
	 * @return String UID for EditText, or null if given text was not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getedittextvisible
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache. 
	 * (in ):PARAM_TEXT=String
	 * (in ):PARAM_ONLYVISIBLE=true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getEditText(String text, boolean onlyVisible) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getedittextvisible);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_ONLYVISIBLE, String.valueOf(onlyVisible));
		
		return getSingleObject(props);
	}
	
	/**
	 * Returns a String UID for the Image at the given index.
	 * @param index of the Image.  0 if only one is available. 
	 * @return String UID for Image at the given index, or null if index is invalid.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getimage
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public String getImage(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(Message.cmd_getimage, index);
	}
	
	/**
	 * Returns a String UID for the ImageButton at the given index. 
	 * @param index of the ImageButton.  0 if only one is available. 
	 * @return String UID for ImageButton at the given index, or null if index is invalid.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getimagebutton
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public String getImageButton(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(Message.cmd_getimagebutton, index);
	}
	
	/**
	 * Returns a String UID for the Text at the given index.
	 * @param index of the Text View. 0 if only one is available.
	 * @return String UID for Text at the given index, or null if index is invalid.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_gettext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public String getText(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(Message.cmd_gettext, index);
	}
	
	/**
	 * Returns a String UID for the Text item with the given text. 
	 * @param text that is shown
	 * @return String UID for Text, or null if given text was not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getetexttext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache. 
	 * (in ):PARAM_TEXT=String
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public String getText(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getSingleObject(Message.cmd_gettexttext, text);
	}
	
	/**
	 * Returns a String UID for the Text item with the given text. 
	 * @param text that is shown
	 * @param onlyVisible tru if only visible Texts on the screen should be shown.
	 * @return String UID for Text, or null if given text was not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_gettextvisible
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache. 
	 * (in ):PARAM_TEXT=String
	 * (in ):PARAM_ONLYVISIBLE=true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getText(String text, boolean onlyVisible) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_gettextvisible);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_ONLYVISIBLE, String.valueOf(onlyVisible));

		return getSingleObject(props);
	}
	
	/**
	 * Returns the absolute top parent View for a given View. 
	 * @param uidView the String UID Reference whose top parent is requested.
	 * @return String UID for the top parent View, or null if not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_gettopparent
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache. 
	 * (in ):PARAM_REFERENCE=String
	 * (out):PARAM_CLASS=String The top parent View's full qualified class name
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getTopParent(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_gettopparent);
		props.setProperty(Message.PARAM_REFERENCE, uidView);

		return getSingleObject(props);
	}
	
	/**
	 * Returns a View of a given class and index. 
	 * @param classname of the requested View.
	 * @param index of the View. 0 if only one is available.
	 * @return String UID for the View, or null if not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getviewclass
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache. 
	 * (in ):PARAM_CLASS=String classname
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getView(String classname, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getviewclass);
		props.setProperty(Message.PARAM_CLASS, classname);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		
		return getSingleObject(props);
	}
	
	/**
	 * Returns a View with the given id. 
	 * @param id the R.id of the View to be returned.
	 * @return String UID for the View, or null if not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getviewid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache.
	 * (out):PARAM_CLASS=String full Classname of the actual View. 
	 * (in ):PARAM_ID=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getView(int id) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getviewid);
		props.setProperty(Message.PARAM_ID, String.valueOf(id));
		
		return getSingleObject(props);
	}
	
	/**
	 * Returns a View matching the specified "resource name" and index. -- <b>Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param idname The resource name of the {@link View} to return.
	 *  			 The resource name contains 3 parts: "package", "type" and "entry", it has format as "package:type/entry".
	 * 				 If the entry belongs to the package of "Application Under Test"(AUT) and it type is "id", user can
	 * 				 provide only the entry part for simplicity. Otherwise, user SHOULD provide the full resource name.<br>
	 *               For example:<br>
	 *               In a resource xml a view is given id as "@+id/flipper", "flipper" is the entry name.
	 *               In the AndroidManifest.xml file the attribute "package" is "com.example.android.apis", the full
	 *               resource name will be "com.example.android.apis:id/flipper"<br>
	 *               If "com.example.android.apis" is the package of AUT, user can provide "flipper" as idname.
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @return String UID for the View, or null if not found.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getviewbynamematch
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache. 
	 * (in ):PARAM_NAME=String, The name of the resource id of the View
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 */
	public String getViewByName(String idname, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getviewbynamematch);
		props.setProperty(Message.PARAM_NAME, idname);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		
		return getSingleObject(props);
	}
	
	/**
	 * Returns a View matching the specified "resource name". -- <b>Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param idname The resource name of the {@link View} to return.
	 *  			 The resource name contains 3 parts: "package", "type" and "entry", it has format as "package:type/entry".
	 * 				 If the entry belongs to the package of "Application Under Test"(AUT) and it type is "id", user can
	 * 				 provide only the entry part for simplicity. Otherwise, user SHOULD provide the full resource name.<br>
	 *               For example:<br>
	 *               In a resource xml a view is given id as "@+id/flipper", "flipper" is the entry name.
	 *               In the AndroidManifest.xml file the attribute "package" is "com.example.android.apis", the full
	 *               resource name will be "com.example.android.apis:id/flipper"<br>
	 *               If "com.example.android.apis" is the package of AUT, user can provide "flipper" as idname.
	 * @return String UID for the View, or null if not found.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getviewbyname
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference for the actual object 
	 * stored in remote cache. 
	 * (in ):PARAM_NAME=String, The name of the resource id of the View
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 */
	public String getViewByName(String idname) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getviewbyname);
		props.setProperty(Message.PARAM_NAME, idname);
		
		return getSingleObject(props);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the View objects located in the focused Activity or Dialog. 
	 * This is the same as {@link #getCurrentViews()}. 
	 * @return ArrayList of 0 or more String UIDs for all the opened/active Views.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getviews
	 * (in ):PARAM_REFERENCE=String
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getViews() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getviews);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the View objects contained in the parent View.
	 * @param parent String UID of the parent View from which to return the Views' String UIDs. 
	 * @return ArrayList of 0 or more String UIDs for all the View objects contained in the given View.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getparentviews
	 * (in ):PARAM_REFERENCE=String
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getViews(String uidParent) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getparentviews);
		props.setProperty(Message.PARAM_REFERENCE, uidParent);
		
		return getCurrentObjects(props);
	}
	
	/**
	 * Checks if the CheckBox with a given index is checked. 
	 * @param index of the CheckBox to check. 0 if only one is available.
	 * @return true if the object is checked. false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_ischeckboxchecked
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked  
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean isCheckBoxChecked(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithIndex(Message.cmd_ischeckboxchecked, index); 
		return getRemoteBooleanResult(success,Message.cmd_ischeckboxchecked,Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Checks if the CheckBox with the given text is checked. 
	 * @param text shown on the CheckBox.
	 * @return true if the object is checked. false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_ischeckboxcheckedtext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked
	 * (in ):PARAM_TEXT=String
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean isCheckBoxChecked(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_ischeckboxcheckedtext, text); 
		return getRemoteBooleanResult(success,Message.cmd_ischeckboxcheckedtext,Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Checks if the RadioButton with a given index is checked. 
	 * @param index of the RadioButton to check. 0 if only one is available.
	 * @return true if the object is checked. false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_isradiobuttonchecked
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean isRadioButtonChecked(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithIndex(Message.cmd_isradiobuttonchecked, index); 
		return getRemoteBooleanResult(success,Message.cmd_isradiobuttonchecked,Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Checks if the RadioButton with the given text is checked. 
	 * @param text shown on the RadioButton.
	 * @return true if the object is checked. false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_isradiobuttoncheckedtext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean isRadioButtonChecked(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_isradiobuttoncheckedtext, text); 
		return getRemoteBooleanResult(success,Message.cmd_isradiobuttoncheckedtext, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Checks if the given text is selected in the given Spinner. 
	 * @param text that is expected to be selected.
	 * @param index of the Spinner to check. 0 if only one is available.
	 * @return true if the given text is selected, false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_isspinnertextselectedindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean isSpinnerTextSelected(int index, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_isspinnertextselectedindex);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));

		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success,Message.cmd_isspinnertextselectedindex, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Checks if the given text is selected in any Spinner located in the current screen. 
	 * @param text that is expected to be selected.
	 * @return true if the given text is selected in any Spinner, false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_isspinnertextselected
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean isSpinnerTextSelected(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_isspinnertextselected, text); 
		return getRemoteBooleanResult(success,Message.cmd_isspinnertextselected, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Checks if the given Text is checked. 
	 * @param text shown on a CheckedTextView or CompoundButton.
	 * @return true if the given text is checked. false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_istextchecked
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean isTextChecked(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_istextchecked, text); 
		return getRemoteBooleanResult(success,Message.cmd_istextchecked, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Checks if a ToggleButton with the give text is checked. 
	 * @param text shown on a ToggleButton.
	 * @return true if a ToggleButton with the given text is checked. false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_istogglebuttoncheckedtext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean isToggleButtonChecked(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_istogglebuttoncheckedtext, text); 
		return getRemoteBooleanResult(success,Message.cmd_istogglebuttoncheckedtext, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Checks if the ToggleButton at the give index is checked. 
	 * @param index of the ToggleButton.  0 if only one is available.
	 * @return true if the ToggleButton is checked. false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_istogglebuttonchecked
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether checked
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean isToggleButtonChecked(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithIndex(Message.cmd_istogglebuttonchecked, index); 
		return getRemoteBooleanResult(success,Message.cmd_istogglebuttonchecked, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Presses a MenuItem with a given index. Index 0 is the first item in the first row.  Index 3 is the first item 
	 * in the second row, and Index 6 is the first item in the third row.
	 * @param index of the MenuItem to be pressed. 
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_pressmenuitem
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean pressMenuItem(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResultWithIndex(Message.cmd_pressmenuitem, index); 
	}
	
	/**
	 * Presses a MenuItem with a given index. Supports three rows with a given amount of items.  If itemsPerRow 
	 * equals 5 then Index 0 is the first item in the first row, Index 5 is the first item in the second row, and 
	 * Index 10 is the first item in the third row.
	 * @param index of the MenuItem to be pressed. 
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_presssubmenuitem
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_INDEX=int 
	 * (in ):PARAM_ITEMSPERROW=int 
	 * 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean pressMenuItem(int index, int itemsPerRow) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_presssubmenuitem);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_ITEMSPERROW, String.valueOf(itemsPerRow));
		return runBooleanResult(props);
	}
	
	/**
	 * Presses on a Spinner (drop-down menu) item.
	 * @param index of the Spinner menu to be used.
	 * @param itemindex of the Spinner item to be pressed relative to the currently selected item. 
	 * A negative number moves up the Spinner.  A positive number moves down. 
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_pressspinneritem
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_INDEX=int 
	 * (in ):PARAM_ITEMINDEX=int 
	 * 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean pressSpinnerItem(int spinnerIndex, int itemIndex) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_pressspinneritem);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(spinnerIndex));
		props.setProperty(Message.PARAM_ITEMINDEX, String.valueOf(itemIndex));
		return runBooleanResult(props);
	}
	
	/**
	 * Scrolls down the screen. 
	 * @return true if more scrolling can be done. false if it is at the end of the screen.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolldown
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled down
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean scrollDown() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runNoArgCommand(Message.cmd_scrolldown);
		return getRemoteBooleanResult(success,Message.cmd_scrolldown, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls to the top of the screen. 
	 * @return true if successful. false otherwise.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolltotop
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled. Always false. 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean scrollToTop() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		return runNoArgCommand(Message.cmd_scrolltotop);
	}
	
	/**
	 * Scrolls to the bottom of the screen. 
	 * @return true if successful. false otherwise.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolltobottom
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled. Always false. 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean scrollToBottom() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		return runNoArgCommand(Message.cmd_scrolltobottom);
	}
	
	/**
	 * Scrolls down a list with the given index.
	 * @param index of the ListView to be scrolled. 0 if only one is available. 
	 * @return true if more scrolling can be done.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolldownlist
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled down
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean scrollDownList(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithIndex(Message.cmd_scrolldownlist,index);
		return getRemoteBooleanResult(success,Message.cmd_scrolldownlist, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls down a list with the given Reference UID. 
	 * @param uidView - the reference id of ListView to scroll down.
	 * @return true if more scrolling can be done. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolldownlistuid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled down
	 * (in ):PARAM_REFERENCE=String UID reference for the View.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean scrollDownListUID(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_scrolldownlistuid);
		props.setProperty(Message.PARAM_REFERENCE, uidView);

		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_scrolldownlistuid, Message.KEY_REMOTERESULTINFO);
	}	
	
	/**
	 * Scrolls down a list with the given index all the way to the bottom.
	 * @param index of the ListView to be scrolled. 0 if only one is available. 
	 * @return true if more scrolling can be done.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolllisttobottom
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled down
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see #runBooleanResultWithIndex(String, int)
	 * @see Message
	 */
	public boolean scrollListToBottom(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithIndex(Message.cmd_scrolllisttobottom,index);
		return getRemoteBooleanResult(success,Message.cmd_scrolllisttobottom, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls down a list with the given UID all the way to the bottom. 
	 * @param uidView - the reference id of ListView to scroll down.
	 * @return true if more scrolling can be done. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolllisttobottomuid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled down
	 * (in ):PARAM_REFERENCE=String UID reference for the View.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean scrollListToBottomUID(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_scrolllisttobottomuid);
		props.setProperty(Message.PARAM_REFERENCE, uidView);

		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_scrolllisttobottomuid, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls a list with the given index to the specified line. 
	 * @param index - the index of the AbsListView to scroll.
	 * @param line	- the line to scroll to
	 * @return true if the command executed successfully, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolllisttoline
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled down
	 * (in ):PARAM_INDEX=String the index of the AbsListView to scroll.
	 * (in ):PARAM_LINE=Int the line to scroll to
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean scrollListToLine(int index, int line) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_scrolllisttoline);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_LINE, String.valueOf(line));
		return runBooleanResult(props);
	}

	/**
	 * Scrolls a list with the given UID to the specified line. 
	 * @param uidView - the reference id of ListView to scroll.
	 * @param line	- the line to scroll to
	 * @return true if the command executed successfully, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolllisttolineuid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled down
	 * (in ):PARAM_REFERENCE=String UID reference for the View.
	 * (in ):PARAM_LINE=Int the line to scroll to
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean scrollListToLineUID(String uidView, int line) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_scrolllisttolineuid);
		props.setProperty(Message.PARAM_REFERENCE, uidView);
		props.setProperty(Message.PARAM_LINE, String.valueOf(line));
		return runBooleanResult(props);
	}	
	
	/**
	 * Scrolls up a list with the given index all the way to the top.
	 * @param index of the ListView to be scrolled. 0 if only one is available. 
	 * @return true if more scrolling can be done.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolllisttotop
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled up
	 * (in ):PARAM_INDEX=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean scrollListToTop(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithIndex(Message.cmd_scrolllisttotop,index);
		return getRemoteBooleanResult(success,Message.cmd_scrolllisttotop, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls up a list with the given UID all the way to the top. 
	 * @param uidView - the reference id of ListView to scroll up.
	 * @return true if more scrolling can be done. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolllisttotopuid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled up
	 * (in ):PARAM_REFERENCE=String UID reference for the View.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean scrollListToTopUID(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_scrolllisttotopuid);
		props.setProperty(Message.PARAM_REFERENCE, uidView);

		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_scrolllisttotopuid, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls horizontally.
	 * @param side to which to scroll; {@link #RIGHT} or {@link #LEFT}
	 * @return true if the command executed successfully, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolltoside
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_SIDE=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean scrollToSide(int side) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_scrolltoside);
		props.setProperty(Message.PARAM_SIDE, String.valueOf(side));
		return runBooleanResult(props);
	}
	
	/**
	 * Scrolls up the screen. 
	 * @return true if more scrolling can be done. false if it is at the top of the screen.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrollup
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled up
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean scrollUp() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runNoArgCommand(Message.cmd_scrollup);
		return getRemoteBooleanResult(success,Message.cmd_scrollup, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls up a list with the given index.
	 * @param index of the ListView to be scrolled. 0 if only one is available. 
	 * @return true if more scrolling can be done.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolluplist
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled up
	 * (in ):PARAM_INDEX=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean scrollUpList(int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithIndex(Message.cmd_scrolluplist,index);
		return getRemoteBooleanResult(success,Message.cmd_scrolluplist, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls up a list with the given reference id. 
	 * @param uidView - the reference id of ListView to scroll up.
	 * @return true if more scrolling can be done. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrolluplistuid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled up
	 * (in ):PARAM_REFERENCE=String UID reference for the View.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean scrollUpListUID(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_scrolluplistuid);
		props.setProperty(Message.PARAM_REFERENCE, uidView);
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_scrolllisttobottomuid, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Scrolls horizontally a view with the given UID. 
	 * @param uidView - the reference id of View to scroll.
	 * @param side	- the side to which to scroll; {@link #RIGHT} or {@link #LEFT}
	 * @return true if the command executed successfully, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_scrollviewtoside
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:whether can still be scrolled down
	 * (in ):PARAM_REFERENCE=String UID reference for the View.
	 * (in ):PARAM_SIDE=Int the side to scroll to
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean scrollViewToSide(String uidView, int side) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_scrollviewtoside);
		props.setProperty(Message.PARAM_REFERENCE, uidView);
		props.setProperty(Message.PARAM_SIDE, String.valueOf(side));
		return runBooleanResult(props);
	}
	
	/**
	 * Searches for a Button with the given text and returns true if at least one is found. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression. 
	 * @return true if at least one such Button is found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchbutton
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean searchButton(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_searchbutton,text);
		return getRemoteBooleanResult(success, Message.cmd_searchbutton, Message.KEY_REMOTERESULTINFO);
	}

	/*
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 */
	private boolean searchVisibleObjectText(String cmd, String text, boolean onlyVisible)throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(cmd);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_ONLYVISIBLE, String.valueOf(onlyVisible));
		return runBooleanResult(props);
	}

	/*
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 */
	private boolean searchMinimumObjectText(String cmd, String text, int minimumNumberOfMatches)throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(cmd);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		return runBooleanResult(props);
	}
	
	/*
	 * Runs the specified command with default_ready_stimeout, default_running_stimeout, default_result_stimeout
	 */
	private boolean searchMinimumVisibleObjectText(String cmd, String text, int minimumNumberOfMatches, boolean onlyVisible)throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(cmd);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_ONLYVISIBLE, String.valueOf(onlyVisible));
		return runBooleanResult(props);
	}
	
	/**
	 * Searches for a Button with the given text and returns true if at least one is found. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression.
	 * @param onlyVisible true if only visible Buttons should be searched. 
	 * @return true if at least one such Button is found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchbuttonvisible
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_ONLYVISIBLE=true/false 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchButton(String text, boolean onlyVisible) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = searchVisibleObjectText(Message.cmd_searchbuttonvisible, text, onlyVisible);
		return getRemoteBooleanResult(success, Message.cmd_searchbuttonvisible, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a Button with the given text and returns true if found at least a given number of times. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression.
	 * @param minimumNumberOfMatches expected to be found. 0 matches means that one or more matches are expected. 
	 * @return true if the Button is found the given number of times, false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchbuttonmatch
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_MINIMUMMATCHES=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchButton(String text, int minimumNumberOfMatches) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = searchMinimumObjectText(Message.cmd_searchbuttonmatch, text, minimumNumberOfMatches);
		return getRemoteBooleanResult(success, Message.cmd_searchbuttonmatch, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a Button with the given text and returns true if found at least a given number of times. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression.
	 * @param minimumNumberOfMatches expected to be found. 0 matches means that one or more matches are expected. 
	 * @param onlyVisible true if only visible Buttons are to be sought. 
	 * @return true if the Button is found the given number of times, false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchbuttonmatchvisible
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_MINIMUMMATCHES=int 
	 * (in ):PARAM_ONLYVISIBLE=true/false 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchButton(String text, int minimumNumberOfMatches, boolean onlyVisible) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = searchMinimumVisibleObjectText(Message.cmd_searchbuttonmatchvisible, text, minimumNumberOfMatches, onlyVisible);
		return getRemoteBooleanResult(success, Message.cmd_searchbuttonmatchvisible, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for an EditText with the given text and returns true if at least one is found. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression. 
	 * @return true if at least one such EditText is found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchedittext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchEditText(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_searchedittext, text);
		return getRemoteBooleanResult(success, Message.cmd_searchedittext, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a TextView with the given text and returns true if at least one is found. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression. 
	 * @return true if at least one such TextView is found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchtext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchText(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_searchtext, text);
		return getRemoteBooleanResult(success, Message.cmd_searchtext, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a TextView with the given text and returns true if at least one is found. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression.
	 * @param onlyVisible true if only visible TextViews should be searched. 
	 * @return true if at least one such TextView is found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchtextvisible
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_ONLYVISIBLE=true/false 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchText(String text, boolean onlyVisible) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = searchVisibleObjectText(Message.cmd_searchtextvisible, text, onlyVisible);
		return getRemoteBooleanResult(success, Message.cmd_searchtextvisible, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a TextView with the given text and returns true if found at least a given number of times. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression.
	 * @param minimumNumberOfMatches expected to be found. 0 matches means that one or more matches are expected. 
	 * @return true if the TextView is found the given number of times, false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchtextmatch
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_MINIMUMMATCHES=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchText(String text, int minimumNumberOfMatches) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = searchMinimumObjectText(Message.cmd_searchtextmatch, text, minimumNumberOfMatches);
		return getRemoteBooleanResult(success, Message.cmd_searchtextmatch, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a TextView with the given text and returns true if found at least a given number of times. 
	 * @param text to search for. The parameter will be interpretted as a regular expression.
	 * @param minimumNumberOfMatches expected to be found. 0 matches means that one or more matches are expected.
	 * @param scroll true if scrolling should be performed.
	 * @return true if the TextView is found the given number of times, false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchtextmatchscroll
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_MINIMUMMATCHES=int 
	 * (in ):PARAM_SCROLL=true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean searchText(String text, int minimumNumberOfMatches, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_searchtextmatchscroll);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_searchtextmatchscroll, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a TextView with the given text and returns true if found at least a given number of times. 
	 * @param text to search for. The parameter will be interpretted as a regular expression.
	 * @param minimumNumberOfMatches expected to be found. 0 matches means that one or more matches are expected.
	 * @param scroll true if scrolling should be performed.
	 * @param onlyVisible true if only visible TextView objects should be sought.
	 * @return true if the TextView is found the given number of times, false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchtextmatchscrollvisible
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_MINIMUMMATCHES=int 
	 * (in ):PARAM_SCROLL=true/false
	 * (in ):PARAM_ONLYVISIBLE=true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean searchText(String text, int minimumNumberOfMatches, boolean scroll, boolean onlyVisible) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_searchtextmatchscrollvisible);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		props.setProperty(Message.PARAM_ONLYVISIBLE, String.valueOf(onlyVisible));

		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_searchtextmatchscrollvisible, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a ToggleButton with the given text and returns true if at least one is found. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression. 
	 * @return true if at least one such ToggleButton is found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchtogglebutton
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchToggleButton(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResultWithText(Message.cmd_searchtogglebutton, text);
		return getRemoteBooleanResult(success, Message.cmd_searchtogglebutton, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Searches for a ToggleButton with the given text and returns true if found at least a given number of times. 
	 * Will automatically scroll when needed.
	 * @param text to search for. The parameter will be interpretted as a regular expression.
	 * @param minimumNumberOfMatches expected to be found. 0 matches means that one or more matches are expected. 
	 * @return true if the ToggleButton is found the given number of times, false if it is not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_searchtogglebuttonmatch
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean: whether found or not
	 * (in ):PARAM_TEXT=String 
	 * (in ):PARAM_MINIMUMMATCHES=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean searchToggleButton(String text, int minimumNumberOfMatches) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = searchMinimumObjectText(Message.cmd_searchtogglebuttonmatch, text, minimumNumberOfMatches);
		return getRemoteBooleanResult(success, Message.cmd_searchtogglebuttonmatch, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Send a key: {@link #RIGHT}, {@link #LEFT}, {@link #UP}, {@link #DOWN}, {@link #ENTER}, {@link #MENU}, {@link #DELETE}
	 * @param key to be sent.
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_sendkey
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_KEY=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean sendKey(int key) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_sendkey);
		props.setProperty(Message.PARAM_KEY, String.valueOf(key));
		return runBooleanResult(props);
	}
	
	/**
	 * Set the Orientation ({@link #LANDSCAPE}/{@link #PORTRAIT}) for the current Activity.
	 * @param orientation to be set.
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setactivityorientation
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_ORIENTATION=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setActivityOrientation(int orientation) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setactivityorientation);
		props.setProperty(Message.PARAM_ORIENTATION, String.valueOf(orientation));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the date in a given DatePicker.
	 * @param uidDatePicker String UID reference to a DatePicker object.
	 * @param years to set e.g. 2011
	 * @param monthOfYear the month e.g. 03
	 * @param dayOfMonth the day e.g. 10
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setdatepickerreference
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_REFERENCE=String UID 
	 * (in ):PARAM_YEAR=int 
	 * (in ):PARAM_YEARMONTH=int 
	 * (in ):PARAM_MONTHDAY=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setDatePicker(String uidDatePicker, int year, int monthOfYear, int dayOfMonth) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setdatepickerreference);
		props.setProperty(Message.PARAM_REFERENCE, uidDatePicker);
		props.setProperty(Message.PARAM_YEAR, String.valueOf(year));
		props.setProperty(Message.PARAM_YEARMONTH, String.valueOf(monthOfYear));
		props.setProperty(Message.PARAM_MONTHDAY, String.valueOf(dayOfMonth));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the date in a given DatePicker.
	 * @param index of the DatePicker to set. 0 if only one is available.
	 * @param years to set e.g. 2011
	 * @param monthOfYear the month e.g. 03
	 * @param dayOfMonth the day e.g. 10
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setdatepickerindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_INDEX=int 
	 * (in ):PARAM_YEAR=int 
	 * (in ):PARAM_YEARMONTH=int 
	 * (in ):PARAM_MONTHDAY=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setDatePicker(int index, int year, int monthOfYear, int dayOfMonth) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setdatepickerindex);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_YEAR, String.valueOf(year));
		props.setProperty(Message.PARAM_YEARMONTH, String.valueOf(monthOfYear));
		props.setProperty(Message.PARAM_MONTHDAY, String.valueOf(dayOfMonth));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the status in a given ProgressBar. Examples are SeekBar and RatingBar.
	 * @param index of the ProgressBar to set. 0 if only one is available.
	 * @param progress that the ProgressBar should be set to.
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setprogressbarindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_INDEX=int 
	 * (in ):PARAM_PROGRESS=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setProgressBar(int index, int progress) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setprogressbarindex);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_PROGRESS, String.valueOf(progress));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the status in a given ProgressBar. Examples are SeekBar and RatingBar.
	 * @param uidProgressBar String UID reference to the desired ProgressBar.
	 * @param progress that the ProgressBar should be set to.
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setprogressbarreference
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_REFERENCE=String UID 
	 * (in ):PARAM_PROGRESS=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setProgressBar(String uidProgressBar, int progress) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setprogressbarreference);
		props.setProperty(Message.PARAM_REFERENCE, uidProgressBar);
		props.setProperty(Message.PARAM_PROGRESS, String.valueOf(progress));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the status in a given SlidingDrawer. Settings are {@link #CLOSED} and {@link #OPENED}
	 * @param index of the SlidingDrawer to be set. 0 if only 1 is available.
	 * @param status of {@link #CLOSED} or {@link #OPENED} to be set.
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setslidingdrawerindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_INDEX=int 
	 * (in ):PARAM_STATUS=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setSlidingDrawer(int index, int status) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setslidingdrawerindex);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_STATUS, String.valueOf(status));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the status in a given SlidingDrawer. Settings are {@link #CLOSED} and {@link #OPENED}
	 * @param uidSlidingDrawer String UID reference to the desired SlidingDrawer.
	 * @param status of {@link #CLOSED} or {@link #OPENED} to be set.
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setslidingdrawerreference
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_REFERENCE=String UID 
	 * (in ):PARAM_STATUS=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setSlidingDrawer(String uidSlidingDrawer, int status) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setslidingdrawerreference);
		props.setProperty(Message.PARAM_REFERENCE, uidSlidingDrawer);
		props.setProperty(Message.PARAM_STATUS, String.valueOf(status));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the time in a given TimePicker.
	 * @param index of the TimePicker to set. 0 if only one is available.
	 * @param hour to be set e.g. 15
	 * @param minute to be set e.g. 30
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_settimepickerindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_INDEX=int 
	 * (in ):PARAM_HOUR=int 
	 * (in ):PARAM_MINUTE=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setTimePicker(int index, int hour, int minute) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_settimepickerindex);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_HOUR, String.valueOf(hour));
		props.setProperty(Message.PARAM_MINUTE, String.valueOf(minute));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the time in a given TimePicker.
	 * @param uidTimePicker String UID reference to the desired TimePicker.
	 * @param hour to be set e.g. 15
	 * @param minute to be set e.g. 30
	 * @return true if the command was successfully executed, false if not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_settimepickerreference
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String 
	 * (in ):PARAM_REFERENCE=String UID 
	 * (in ):PARAM_HOUR=int 
	 * (in ):PARAM_MINUTE=int 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean setTimePicker(String uidTimePicker, int hour, int minute) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_settimepickerreference);
		props.setProperty(Message.PARAM_REFERENCE, uidTimePicker);
		props.setProperty(Message.PARAM_HOUR, String.valueOf(hour));
		props.setProperty(Message.PARAM_MINUTE, String.valueOf(minute));
		return runBooleanResult(props);
	}
	
	/**
	 * Start the AUT app by launching its main launcher Activity. 
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_startmainlauncher
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in/out):PARAM_ERRORMSG=String
	 * (out):PARAM_NAME=String Name of the Activity that was launched.
	 * (out):PARAM_CLASS=String full Classname of the Activity that was launched.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int) 
	 * @see #_last_remote_result 
	 * @see Message
	 */
	public boolean startMainLauncher() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runNoArgCommand(Message.cmd_startmainlauncher);
	}
	
	/**
	 * All Activities that have been active are finished. 
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_finishopenedactivities
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean finishOpenedActivities() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_finishopenedactivities);
		return runBooleanResult(props);
	}
	
	/**
	 * Finalizes the remote Solo object and removes the ActivityMonitor. 
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_finalizeremotesolo
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean finalizeRemoteSolo() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_finalizeremotesolo);
		return runBooleanResult(props);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the opened/active Activities. 
	 * @return ArrayList of 0 or more String UIDs for all the opened/active Activities.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getallopenactivities
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual Activity objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public ArrayList<String> getAllOpenActivities() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getallopenactivities);

		return getCurrentObjects(props);
	}
	
	/**
	 * Returns a String UID reference to the current Activity. 
	 * @return string UID reference to the current Activity, or null.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentactivity
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference key for current Activity object 
	 * stored in a remote cache.
	 * (out):PARM_CLASS=ClassName of retrieved Activity.
	 * (out):PARM_NAME=Name of retrieved Activity.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getCurrentActivity() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getcurrentactivity);
		return getSingleObject(props);
	}
	
	/**
	 * Returns a localized String from localized String resources.
	 * @param resourceId of the localized String resource. 
	 * @return String or null if not found.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getstring
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=localized String or null if it does not exist.
	 * (in ):PARAM_RESID=int
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getString(String resourceId) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getstring);
		
		return getSingleObject(props);
	}
	
	/**
	 * Make Robotium sleep for a specified number of milliseconds.
	 * @param time in milliseconds for Robotium to sleep.
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_sleep
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in ):PARAM_TIME=millis
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean sleep(int millis) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_sleep);
		props.setProperty(Message.PARAM_TIME, String.valueOf(millis));
		return runBooleanResult(props);
	}
	
	/**
	 * Asserts that the expected Activity is the current active one. 
	 * @param errormsg - the message to display/log if the assertion fails.
	 * @param activityname - the name of the Activity that is expected to be active e.g. "MyActivity"
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_assertcurrentactivityname
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in/out):PARAM_ERRORMSG=String
	 * (in/out):PARAM_NAME=String (in: the expected Activity name, out: the actual Activity name)
	 * (out):PARAM_CLASS=String full Classname of the actual current Activity.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean assertCurrentActivityName(String errormsg, String activityname) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_assertcurrentactivityname);
		props.setProperty(Message.PARAM_ERRORMSG, errormsg);
		props.setProperty(Message.PARAM_NAME, activityname);
		_last_remote_result = control.performRemotePropsCommand(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		if(rc == Message.STATUS_REMOTERESULT_OK) {
			processSuccess(_last_remote_result.getProperty(Message.KEY_COMMAND), _last_remote_result.getProperty(Message.PARAM_NAME));
		}else{
			processFailure(_last_remote_result.getProperty(Message.KEY_COMMAND), _last_remote_result.getProperty(Message.PARAM_ERRORMSG));
			return false;
		}
		return true;
	}

	/**
	 * Asserts that the expected Activity is the current active one, with the possibility to verify that 
	 * the expected Activity is a new instance of the Activity.  
	 * @param errormsg - the message to display/log if the assertion fails.
	 * @param activityname - the name of the Activity that is expected to be active e.g. "MyActivity"
	 * @param isnewinstance - true if the expected Activity is a new instance of the Activity.
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_assertnewcurrentactivityname
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in/out):PARAM_ERRORMSG=String
	 * (in/out):PARAM_ISNEWINSTANCE=true/false 
	 * (in/out):PARAM_NAME=String (in: the expected Activity name, out: the actual Activity name)
	 * (out):PARAM_CLASS=String full Classname of the actual current Activity.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean assertCurrentActivityName(String errormsg, String activityname, boolean isNewInstance) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_assertnewcurrentactivityname);
		props.setProperty(Message.PARAM_ERRORMSG, errormsg);
		props.setProperty(Message.PARAM_NAME, activityname);
		props.setProperty(Message.PARAM_ISNEWINSTANCE, String.valueOf(isNewInstance));
		_last_remote_result = control.performRemotePropsCommand(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		if(rc == Message.STATUS_REMOTERESULT_OK) {
			processSuccess(_last_remote_result.getProperty(Message.KEY_COMMAND), _last_remote_result.getProperty(Message.PARAM_NAME));
		}else{
			processFailure(_last_remote_result.getProperty(Message.KEY_COMMAND), _last_remote_result.getProperty(Message.PARAM_ERRORMSG));
			return false;
		}
		return true;
	}
	
	/**
	 * Asserts that the expected Activity is the current active one. 
	 * @param errormsg - the message to display/log if the assertion fails.
	 * @param activityclass - the full classname of the Activity that is expected to be active e.g. "com.company.activities.MainActivityClass"
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_assertcurrentactivityclass
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in/out):PARAM_ERRORMSG=String
	 * (out):PARAM_NAME=String name of the actual current Activity.
	 * (in/out):PARAM_CLASS=String (in: the expected Activity class, out: the actual current Activity class)
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see#_last_remote_result
	 * @see Message
	 */
	public boolean assertCurrentActivityClass(String errormsg, String activityclass) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_assertcurrentactivityclass);
		props.setProperty(Message.PARAM_ERRORMSG, errormsg);
		props.setProperty(Message.PARAM_CLASS, activityclass);
		_last_remote_result = control.performRemotePropsCommand(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		if(rc == Message.STATUS_REMOTERESULT_OK) {
			processSuccess(_last_remote_result.getProperty(Message.KEY_COMMAND), _last_remote_result.getProperty(Message.PARAM_CLASS));
		}else{
			processFailure(_last_remote_result.getProperty(Message.KEY_COMMAND), _last_remote_result.getProperty(Message.PARAM_ERRORMSG));
			return false;
		}
		return true;
	}

	/**
	 * Asserts that the expected Activity is the current active one, with the possibility to verify that the expected 
	 * Activity is a new instance of the Activity.
	 * @param errormsg - the message to display/log if the assertion fails.
	 * @param activityclass - the full classname of the Activity that is expected to be active e.g. "com.company.activities.MainActivityClass"
	 * @param isnewinstance - true if the expected Activity is a new instance of the Activity
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object. 
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_assertnewcurrentactivityclass
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in/out):PARAM_ISNEWINSTANCE=true/false
	 * (in/out):PARAM_ERRORMSG=String
	 * (out):PARAM_NAME=String name of the actual current Activity.
	 * (in/out):PARAM_CLASS=String (in: the expected Activity class, out: the actual current Activity class)
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean assertCurrentActivityClass(String errormsg, String activityclass, boolean isNewInstance) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_assertnewcurrentactivityclass);
		props.setProperty(Message.PARAM_ERRORMSG, errormsg);
		props.setProperty(Message.PARAM_CLASS, activityclass);
		props.setProperty(Message.PARAM_ISNEWINSTANCE, String.valueOf(isNewInstance));
		_last_remote_result = control.performRemotePropsCommand(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		if(rc == Message.STATUS_REMOTERESULT_OK) {
			processSuccess(_last_remote_result.getProperty(Message.KEY_COMMAND), _last_remote_result.getProperty(Message.PARAM_CLASS));
		}else{
			processFailure(_last_remote_result.getProperty(Message.KEY_COMMAND), _last_remote_result.getProperty(Message.PARAM_ERRORMSG));
			return false;
		}
		return true;
	}

	/**
	 * Asserts that the available memory on the device or system is not low. 
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_assertmemorynotlow
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:true, if the system's memory is enough.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(String)
	 * @see Message
	 */
	public boolean assertMemoryNotLow() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = runBooleanResult(Message.cmd_assertmemorynotlow);
		return getRemoteBooleanResult(success, Message.cmd_assertmemorynotlow, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Simulates pressing the hardware back key.  
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_goback
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(String)
	 * @see Message
	 */
	public boolean goBack() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return runBooleanResult(Message.cmd_goback);
	}
	
	/**
	 * Returns to the given Activity.
	 * @param activityname - the name of the Activity to wait for e.g. "MyActivity" 
	 * @return true if successful.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_gobacktoactivity
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in/out):PARAM_NAME=String (in: the name of the Activity to return to e.g. "MyActivity", out: the actual Activity name)
	 * (out):PARAM_CLASS=String the actual Activity's full qualified class name
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean goBackToActivity(String activityname) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_gobacktoactivity);
		props.setProperty(Message.PARAM_NAME, activityname);
		return runBooleanResult(props);
	}
	
	/**
	 * Waits for the given Activity. Default timeout is 20 seconds.
	 * @param activityname - the name of the Activity to wait for e.g. "MyActivity" 
	 * @return true if Activity appears before the timeout and false if it does not. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforactivity
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the activity appear
	 * (in/out):PARAM_NAME=String (in: the name of the Activity to wait for e.g. "MyActivity", out: the actual Activity name)
	 * (out):PARAM_CLASS=String the actual Activity's full qualified class name
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForActivity(String activityname) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforactivity);
		props.setProperty(Message.PARAM_NAME, activityname);
		boolean success = runBooleanResult(props);
		
		return getRemoteBooleanResult(success, Message.cmd_waitforactivity, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for the given Activity for up to the specified timeout milliseconds.
	 * @param activityname - the name of the Activity to wait for e.g. "MyActivity".
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @return true if Activity appears before the timeout and false if it does not. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforactivitytimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the activity appear
	 * (in/out):PARAM_NAME=String (in: the name of the Activity to wait for e.g. "MyActivity", out: the actual Activity name)
	 * (in ):PARAM_TIMEOUT=milliseconds to wait.
	 * (out):PARAM_CLASS=String the actual Activity's full qualified class name
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForActivity(String activityname, int timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforactivitytimeout);
		props.setProperty(Message.PARAM_NAME, activityname);
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;

		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout+stime);
		return getRemoteBooleanResult(success, Message.cmd_waitforactivitytimeout, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for the given V4 Fragment for up to the specified timeout milliseconds.
	 * @param tag - the tag of the Fragment to wait for.
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @return true if Fragment appears before the timeout and false if it does not. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforfragmentbytag
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the fragment appear
	 * (in/out):PARAM_NAME=String (in: the tag of the Fragment to wait for, out: the current Activity name)
	 * (in ):PARAM_TIMEOUT=milliseconds to wait.
	 * (out):PARAM_CLASS=String the current Activity's full qualified class name
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForFragmentByTag(String tag, int timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforfragmentbytag);
		props.setProperty(Message.PARAM_NAME, tag);
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitforfragmentbytag, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for the given V4 Fragment for up to the specified timeout milliseconds.
	 * @param id - the id of the Fragment to wait for.
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @return true if Fragment appears before the timeout and false if it does not. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforfragmentbyid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the fragment appear
	 * (in/out):PARAM_ID=String (in: the int id of the Fragment to wait for, out: the current Activity String name)
	 * (in ):PARAM_TIMEOUT=milliseconds to wait.
	 * (out):PARAM_CLASS=String the current Activity's full qualified class name
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForFragmentById(int id, int timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforfragmentbyid);
		props.setProperty(Message.PARAM_ID, String.valueOf(id));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitforfragmentbyid, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a Dialog to close.
	 * @param timeout -- the amount of time in milliseconds to wait 
	 * @return true if the Dialog is closed before the timeout and false if it is not closed. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitfordialogtoclose
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the dialog has been closed
	 * (in ):PARAM_TIMEOUT=milliseconds
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForDialogToClose(int millis) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitfordialogtoclose);
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(millis));
		int stime = millis > 0 ? (int)Math.ceil(millis/1000) : 0;
		
		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitfordialogtoclose, Message.KEY_REMOTERESULTINFO);
	}	
	
	/**
	 * Waits for a Dialog to open.
	 * @param timeout -- the amount of time in milliseconds to wait 
	 * @return true if the Dialog is opened before the timeout and false if it is not opened. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitfordialogtoopen
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the dialog has been opened
	 * (in ):PARAM_TIMEOUT=milliseconds
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException -- if the command was not executed successfully in remote side.
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #finishOpenedActivities()
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForDialogToOpen(int millis) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitfordialogtoopen);
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(millis));
		int stime = millis > 0 ? (int)Math.ceil(millis/1000) : 0;

		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitfordialogtoopen, Message.KEY_REMOTERESULTINFO);
	}	

	/**
	 * Waits for specific text in the android log within a timeout period. 
	 * The app must have the android.permission.READ_LOGS permission.
	 * @param text - the text to wait for.
	 * @param timeout - timeout in milliseconds to wait.
	 * @return true if log message is found, and false if not before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforlogmessage
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in ):PARAM_TEXT=String text to wait for.
	 * (in ):PARAM_TIMEOUT=milliseconds timeout to wait.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForLogMessage(String text, int timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforlogmessage);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_TIMEOUT, text);
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;

		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitforlogmessage, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a text to be shown. Default timeout is 20 seconds. 
	 * @param text - the text to wait for.
	 * @return true if text is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitfortext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String
	 * (in ):PARAM_TEXT=String text to wait for.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForText(String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitfortext);
		props.setProperty(Message.PARAM_TEXT, text);
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_waitfortext, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a text to be shown.  
	 * @param text - the text to wait for.
	 * @param minimumNumberOfMatches -- the minimum number of matches that are expected to be shown.  
	 * 0 means any number of matches. 
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @return true if text is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitfortextmatchtimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the text appear
	 * (in ):PARAM_TEXT=String text to wait for.
	 * (in ):PARAM_MINIMUMMATCHES=int
	 * (in ):PARAM_TIMEOUT=milliseconds.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForText(String text, int minimumNumberOfMatches, long timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitfortextmatchtimeout);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		boolean success = this.runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitfortextmatchtimeout, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a text to be shown.  
	 * @param text - the text to wait for.
	 * @param minimumNumberOfMatches -- the minimum number of matches that are expected to be shown.  
	 * 0 means any number of matches. 
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @param scroll -- true if scrolling should be performed. 
	 * @return true if text is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitfortextmatchtimeoutscroll
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the text appear
	 * (in ):PARAM_TEXT=String text to wait for.
	 * (in ):PARAM_MINIMUMMATCHES=int
	 * (in ):PARAM_TIMEOUT=milliseconds.
	 * (in ):PARAM_SCROLL=true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForText(String text, int minimumNumberOfMatches, long timeout, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitfortextmatchtimeoutscroll);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		
		boolean success = this.runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitfortextmatchtimeoutscroll, Message.KEY_REMOTERESULTINFO);
	}

	/**
	 * Waits for a text to be shown.  
	 * @param text - the text to wait for.
	 * @param minimumNumberOfMatches -- the minimum number of matches that are expected to be shown.  
	 * 0 means any number of matches. 
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @param scroll -- true if scrolling should be performed. 
	 * @param onlyVisible -- true if only visible text views should be waited for 
	 * @return true if text is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitfortextmatchtimeoutscrollvisible
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the text appear
	 * (in ):PARAM_TEXT=String text to wait for.
	 * (in ):PARAM_MINIMUMMATCHES=int
	 * (in ):PARAM_TIMEOUT=milliseconds.
	 * (in ):PARAM_SCROLL=true/false
	 * (in ):PARAM_ONLYVISIBLE=true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForText(String text, int minimumNumberOfMatches, long timeout, boolean scroll, boolean onlyVisible) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitfortextmatchtimeoutscrollvisible);
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		props.setProperty(Message.PARAM_ONLYVISIBLE, String.valueOf(onlyVisible));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		
		boolean success = this.runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitfortextmatchtimeoutscrollvisible, Message.KEY_REMOTERESULTINFO);
	}

	/**
	 * Waits for a View of a certain classname (or extended subclass) to be shown. Default timeout is 20 seconds.  
	 * @param classname - the View classname to wait for.
	 * @return true if View is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforviewclass
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the view appear
	 * (in ):PARAM_CLASS=String classname to wait for.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForView(String classname) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforviewclass);
		props.setProperty(Message.PARAM_CLASS, classname);
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_waitforviewclass, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a View of a certain classname (or extended subclass) to be shown.  
	 * @param classname - the classname to wait for.
	 * @param minimumNumberOfMatches -- the minimum number of matches that are expected to be shown.  
	 * 0 means any number of matches. 
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @return true if View is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforviewclassmatchtimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the view appear
	 * (in ):PARAM_CLASS=String classname to wait for.
	 * (in ):PARAM_MINIMUMMATCHES=int
	 * (in ):PARAM_TIMEOUT=milliseconds.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForView(String classname, int minimumNumberOfMatches, long timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforviewclassmatchtimeout);
		props.setProperty(Message.PARAM_CLASS, classname);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		
		boolean success = this.runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitforviewclassmatchtimeout, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a View of a certain classname (or extended subclass) to be shown.  
	 * @param classname - the classname to wait for.
	 * @param minimumNumberOfMatches -- the minimum number of matches that are expected to be shown.  
	 * 0 means any number of matches. 
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @param scroll -- true if scrolling should be performed 
	 * @return true if View is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforviewclassmatchtimeoutscroll
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the view appear
	 * (in ):PARAM_CLASS=String classname to wait for.
	 * (in ):PARAM_MINIMUMMATCHES=int
	 * (in ):PARAM_TIMEOUT=milliseconds.
	 * (in ):PARAM_SCROLL=true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see Message
	 */
	public boolean waitForView(String classname, int minimumNumberOfMatches, long timeout, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforviewclassmatchtimeoutscroll);
		props.setProperty(Message.PARAM_CLASS, classname);
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;

		boolean success = this.runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitforviewclassmatchtimeoutscroll, Message.KEY_REMOTERESULTINFO);
	}

	/**
	 * Waits for a View matching the specified resource id. Default timeout is 20 seconds.  -- <b>Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param id the R.id of the {@link View} to wait for
	 * @return true if View is shown and false if it is not shown before the timeout.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforviewid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the view appear
	 * (in ):PARAM_ID=int, the id of the view to wait for.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForView(int id) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforviewid);
		props.setProperty(Message.PARAM_ID, String.valueOf(id));
		
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success,Message.cmd_waitforviewid,Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a View matching the specified resource id. -- <b>Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param id the R.id of the {@link View} to wait for
	 * @param minimumNumberOfMatches the minimum number of matches that are expected to be found. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @return true if View is shown and false if it is not shown before the timeout.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforviewidtimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the view appear
	 * (in ):PARAM_ID=int, the id of the view to wait for.
	 * (in ):PARAM_MINIMUMMATCHES=int
	 * (in ):PARAM_TIMEOUT=int, milliseconds.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForView(int id, int minimumNumberOfMatches, int timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforviewidtimeout);
		props.setProperty(Message.PARAM_ID, String.valueOf(id));
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success,Message.cmd_waitforviewidtimeout,Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a View matching the specified resource id. -- <b>Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param id the R.id of the {@link View} to wait for
	 * @param minimumNumberOfMatches the minimum number of matches that are expected to be found. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @return true if View is shown and false if it is not shown before the timeout.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforviewidtimeoutscroll
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the view appear
	 * (in ):PARAM_ID=int, the id of the view to wait for.
	 * (in ):PARAM_MINIMUMMATCHES=int
	 * (in ):PARAM_TIMEOUT=int, milliseconds.
	 * (in ):PARAM_SCROLL=boolean, true/false
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForView(int id, int minimumNumberOfMatches, int timeout, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforviewidtimeoutscroll);
		props.setProperty(Message.PARAM_ID, String.valueOf(id));
		props.setProperty(Message.PARAM_MINIMUMMATCHES, String.valueOf(minimumNumberOfMatches));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success,Message.cmd_waitforviewidtimeoutscroll,Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for the View with the previously captured UID reference. Default timeout is 20 seconds. 
	 * @param uidView - the View to wait for.
	 * @return true if View is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforviewreference
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the view appear
	 * (in ):PARAM_REFERENCE=String UID reference for the View.
	 * (out):PARAM_CLASS=String The full qualified class name of the View.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean waitForViewUID(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforviewreference);
		props.setProperty(Message.PARAM_REFERENCE, uidView);
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_waitforviewreference, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for the View with the previously captured UID reference. 
	 * @param uidView - the View to wait for.
	 * @param timeout -- milliseconds to wait before timeout. 
	 * @param scroll -- true if scrolling should be performed 
	 * @return true if View is shown and false if it is not shown before the timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforviewreferencetimeoutscroll
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the view appear
	 * (in ):PARAM_REFERENCE=String UID reference for the View.
	 * (in ):PARAM_TIMEOUT=milliseconds
	 * (in ):PARAM_SCROLL=true/false
	 * (out):PARAM_CLASS=String The full qualified class name of the View.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties, int, int, int)
	 * @see Message
	 */
	public boolean waitForViewUID(String uidView, int timeout, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforviewreferencetimeoutscroll);
		props.setProperty(Message.PARAM_REFERENCE, uidView);
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		
		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, stime + tcp_delay);
		return getRemoteBooleanResult(success, Message.cmd_waitforviewreferencetimeoutscroll, Message.KEY_REMOTERESULTINFO);
	}	
	
	/**
	 * Returns a Dimension representing the emulator/device's screen size.
	 * If unexpected something happened, a null will be returned.
	 * @return Dimension representing the emulator/device's screen size.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getscreensize
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the width and height of emulator/device
	 * <p>
	 * REMOTERESULTINFO content format: ";width;height"
	 * <p>
	 * The first character is the delimiter used to delimit width and height.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public Dimension getScreenSize() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Dimension dim = null;
		try{
			ArrayList<String> wh = getCurrentObjects(Message.cmd_getscreensize);
			dim = new Dimension(Integer.parseInt(wh.get(0)), Integer.parseInt(wh.get(1)));
		}catch(Exception e){}
		return dim;
	}
	
	/**
	 * Return the location of View on screen.
	 * @param uidView - the View to get location.
	 * @return a Rectangle representing the location of View on screen.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getviewlocation
	 * (in ):PARAM_REFERENCE=String: the view's ID
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the location of view on screen
	 * <p>
	 * REMOTERESULTINFO content format: ";x;y;width;height"
	 * <p>
	 * The first character is the delimiter used to delimit x, y, width and height.
	 * </pre>
	 * (x, y) is the upper-left point of the View.
	 * If unexpected something happened, a null will be returned.
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see Message
	 */
	public Rectangle getViewLocation(String uidView) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Rectangle loc = null;
		try{
			Properties props = prepSoloDispatch(Message.cmd_getviewlocation);
			props.setProperty(Message.PARAM_REFERENCE, String.valueOf(uidView));

			ArrayList<String> xywh = getCurrentObjects(props);
			
			loc = new Rectangle(Integer.parseInt(xywh.get(0)),
					            Integer.parseInt(xywh.get(1)),
					            Integer.parseInt(xywh.get(2)),
					            Integer.parseInt(xywh.get(3)));
		}catch(Exception e){}
		return loc;
	}
	
	/**
	 * Return the TextView's text value.
	 * @param uid - the TextView to get its text value.
	 * @return String, the TextView's text value.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_gettextviewvalue
	 * (in ):PARAM_REFERENCE=String: the view's ID
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String the TextView's text value
	 * 
	 * </pre>
	 * If unexpected something happened, a null will be returned.
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */
	public String getTextViewValue(String uid) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		String info = null;
		try{
			Properties props = prepSoloDispatch(Message.cmd_gettextviewvalue);
			props.setProperty(Message.PARAM_REFERENCE, String.valueOf(uid));

			info = getSingleObject(props);
		}catch(Exception e){}
		return info;
	}
	
	/**
	 * Get the image of View according to String UID reference.
	 *   
	 * @param viewID 			The string uid of the View.
	 * @return BufferedImage 	The image of the View.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getguiimage
	 * (in ):PARAM_ID=String
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the encoded png image bytes of the view./Or error message.
	 * <p>
	 * 
	 * <p>
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public BufferedImage getGUIImage(String viewID) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		String debugmsg = getClass().getName()+".getGUIImage() ";
		BufferedImage image = null;

		Properties props = prepSoloDispatch(Message.cmd_getguiimage);
		props.setProperty(Message.PARAM_ID, viewID);
		_last_remote_result = control.performRemotePropsCommand(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
		int rc = Message.STATUS_REMOTERESULT_UNKNOWN;
		try{rc = Integer.parseInt(_last_remote_result.getProperty(Message.KEY_REMOTERESULTCODE));}
		catch(NumberFormatException x){}
		
		if(rc==Message.STATUS_REMOTERESULT_OK){
			try {
				//Get the encoded png image string from the remote result
				String encodedPNGImageString = _last_remote_result.getProperty(Message.KEY_REMOTERESULTINFO);
				ByteArrayInputStream instream = new ByteArrayInputStream(encodedPNGImageString.getBytes());
				//Create the file to contain the png image
				File fn = File.createTempFile("remoteAndroidView", ".PNG");
				FileOutputStream outstream = new FileOutputStream(fn);
				//Decode the "encoded png image string" to a normal png image.
				Base64Decoder decoder = new Base64Decoder(instream, outstream);
				decoder.process();
				instream.close();
				outstream.close();
				//Read the png image file
				image = ImageIO.read(fn);
			} catch (IOException e) {
				debug(debugmsg+" Met Exception "+e.getMessage());
			} catch (Base64FormatException e) {
				debug(debugmsg+" Met Exception "+e.getMessage());
			}
		}else{
			String errorMsg = _last_remote_result.getProperty(Message.KEY_REMOTERESULTINFO);
			debug(debugmsg+" Fail to get image: "+errorMsg);
		}
		
		return image;
	}
	
	/**
	 * Get the class name of View according to String UID reference.
	 *   
	 * @param viewID 			The string uid of the View.
	 * @return String		 	The class name of the View./ or null if meet error.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getviewclassname
	 * (in ):PARAM_ID=String
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the class name of the view.
	 * <p>
	 * 
	 * <p>
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */	
	public String getViewClassName(String viewID) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getviewclassname);
		props.setProperty(Message.PARAM_ID, viewID);
		
		return getSingleObject(props);
	}
	
	/**
	 * Get the class name of an object according to String UID reference.
	 *   
	 * @param viewID 			The string uid of an object.
	 * @return String		 	The class name of the object./ or null if meet error.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getobjectclassname
	 * (in ):PARAM_ID=String
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the class name of the object.
	 * <p>
	 * 
	 * <p>
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 */	
	public String getObjectClassName(String viewID) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getobjectclassname);
		props.setProperty(Message.PARAM_ID, viewID);
		
		return getSingleObject(props);
	}

	/**
	 * Stop the current screenshot sequence started with startScreenshotSequence.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_stopscreenshotsequence
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String, error message if the command fails.  
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(String)
	 * @see Message
	 */
	public boolean stopScreenshotSequence() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		boolean result = runBooleanResult(Message.cmd_stopscreenshotsequence);
		setScreenshotSequenceRunning(false);
		return result;
	}

	/**
	 * Takes a Robotium screenshot sequence and saves the images with the specified name prefix in 
	 * "/sdcard/Robotium-Screenshots/".  
	 * The name prefix is appended with "_" + sequence_number for each image in the sequence, where 
	 * numbering starts at 0.  Requires write permission (android.permission.WRITE_EXTERNAL_STORAGE) 
	 * in AndroidManifest.xml of the application under test.  At present, multiple simultaneous 
	 * screenshot sequences are not supported by Robotium.  This method will throw an Exception if 
	 * {@link #stopScreenshotSequence()} has not been called to finish any prior sequences.  
	 * Calling this method is equivalent to calling {@link #startScreenshotSequence(String, int, int, int)} 
	 * with (filename, 80, 400, 100).
	 * @param filename 	The root filename prefix for the screenshot sequence.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object. 
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_startscreenshotsequencemax
	 * (in ):PARAM_NAME=String
	 * (in ):PARAM_QUALITY= 80
	 * (in ):PARAM_TIME= 400
	 * (in ):PARAM_STEPCOUNT= 100
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=/sdcard/Robotium-Screenshots/filename prefix, or error message if the command fails.  
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #startScreenshotSequence(String, int, int, int)
	 * @see #stopScreenshotSequence()
	 * @see #getScreenshotSequence(int)
	 * @see #getScreenshotSequence(int, int)
	 * @see Message
	 */
	public boolean startScreenshotSequence(String filename) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return startScreenshotSequence(filename,80,400,100);
	}

	/**
	 * Takes a Robotium screenshot sequence and saves the images with the specified name prefix in 
	 * "/sdcard/Robotium-Screenshots/".  
	 * The name prefix is appended with "_" + sequence_number for each image in the sequence, where 
	 * numbering starts at 0.  Requires write permission (android.permission.WRITE_EXTERNAL_STORAGE) 
	 * in AndroidManifest.xml of the application under test.  At present, multiple simultaneous 
	 * screenshot sequences are not supported by Robotium.
	 * Note: taking a single screenshot takes 40-100 milliseconds each on the main remote thread.  
	 * This method will throw an Exception if {@link #stopScreenshotSequence()} has not been called to 
	 * finish any prior sequences.   
	 * @param filename 	The root filename prefix for the screenshots.
	 * @param quality the compression rate. 0 - 100. 0 = compress for lowest quality, 100 = compress for max quality.
	 * @param frameDelay the time in milliseconds to wait between each frame.
	 * @param maxFrames the maximum number of frames that will comprise the sequence.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object. 
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_startscreenshotsequencemax
	 * (in ):PARAM_NAME=String
	 * (in ):PARAM_QUALITY= 0 - 100
	 * (in ):PARAM_TIME= time in milliseconds between each frame
	 * (in ):PARAM_STEPCOUNT= max number of frames for the sequence
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=/sdcard/Robotium-Screenshots/filename prefix, or error message if the command fails.  
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #stopScreenshotSequence()
	 * @see #getScreenshotSequence(int)
	 * @see #getScreenshotSequence(int, int)
	 * @see Message
	 */
	public boolean startScreenshotSequence(String filename, int quality, int frameDelay, int maxFrames) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_startscreenshotsequencemax);
		props.setProperty(Message.PARAM_NAME, filename);
		props.setProperty(Message.PARAM_QUALITY, String.valueOf(quality));
		props.setProperty(Message.PARAM_TIME, String.valueOf(frameDelay));
		props.setProperty(Message.PARAM_STEPCOUNT, String.valueOf(maxFrames));
		
		setScreenshotSequenceRunning(true);
		screenshotSequenceName = filename;
		
		// calculate results_timeout seconds using 100_ms per frame + frameDelay_ms * #frames + 2 arbitrary seconds
		int max_timeout = (((frameDelay * maxFrames) + (100 * maxFrames))/1000) + 2;
		return runBooleanResult(props, default_ready_stimeout, default_running_stimeout, max_timeout);		
	}
	
	/**
	 * This method is used to retrieve number of the screenshot sequence generated at the device.<br>
	 * It is MUST be called in following order:<br>
	 * 1. Call {@link #startScreenshotSequence(String)}/{@link #startScreenshotSequence(String, int, int, int)}<br>
	 * 2. Call {@link #stopScreenshotSequence()}<br>
	 * 3. Call {@link #getScreenshotSequenceSize(String, boolean, int)}<br>
	 * 4. Call {@link #getScreenshotSequence(String, int)} to get each image.<br>
	 * @param onlyLasttime,	boolean, if true, count only the sequence generated last time; if false, count all.
	 * @param filename, String, The root filename prefix for the screenshots.
	 * @param timeoutSecond, int, the extra timeout (in seconds) to wait for the result.
	 * @return int, the total number of screenshot generated by call startScreenshotSequence()
	 * Field {@link #_last_remote_result} contains the returned Properties object. 
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getscreenshotsequenceszie
	 * (in ):PARAM_NAME=String
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String, containing a encoded JPG image./Or an error message. 
	 * (out):PARAM_NAME+"FILE"=A set of absolute-path to temp screenshot image, delimited by ;
	 * </pre>
	 * @throws IllegalThreadStateException
	 * @throws RemoteException
	 * @throws TimeoutException
	 * @throws ShutdownInvocationException
	 * @throws RemoteSoloException
	 * @see #startScreenshotSequence(String)
	 * @see #startScreenshotSequence(String, int, int, int)
	 * @see #stopScreenshotSequence()
	 * @see #getScreenshotSequence(String, int)
	 */
	public int getScreenshotSequenceSize(String filename, boolean onlyLasttime, int timeoutSecond) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		if(filename==null || isScreenshotSequenceRunning()){
			throw new IllegalThreadStateException("Screenshot sequence name is null or the collecting-thread is still running.!");
		}

		Properties props = prepSoloDispatch(Message.cmd_getscreenshotsequenceszie);
		props.setProperty(Message.PARAM_NAME, filename);
		props.setProperty(Message.PARAM_ONLYVISIBLE, Boolean.toString(onlyLasttime));
		
		int size = 0;
		try{
			size = Integer.parseInt(getSingleObject(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout+timeoutSecond));
		}catch(NumberFormatException ignore){}
		
		return size;
	}
	
	/**
	 * This method is used to retrieve one of the screenshot sequence from the device.<br>
	 * It is MUST be called in following order:<br>
	 * 1. Call {@link #startScreenshotSequence(String)}/{@link #startScreenshotSequence(String, int, int, int)}<br>
	 * 2. Call {@link #stopScreenshotSequence()}<br>
	 * 3. Optionally Call {@link #getScreenshotSequenceSize(String, boolean, int)}<br>
	 * 4. Call {@link #getScreenshotSequence(String, int)} to get each image.<br>
	 * @param filename, String, The root filename prefix for the screenshots.
	 * @param index, int, the index of the screenshot of the whole sequence, start from 0.
	 * @return BufferedImage, a screenshot generated by call startScreenshotSequence()
	 * Field {@link #_last_remote_result} contains the returned Properties object. 
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getscreenshotsequenceindex
	 * (in ):PARAM_NAME=String
	 * (in ):PARAM_INDEX= the index of frame to get, start from 0.
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String, containing a encoded JPG image./Or an error message. 
	 * (out):PARAM_NAME+"FILE"=Absolute Path to temp screenshot image. 
	 * </pre>
	 * @throws IllegalThreadStateException
	 * @throws RemoteException
	 * @throws TimeoutException
	 * @throws ShutdownInvocationException
	 * @throws RemoteSoloException
	 * @see #startScreenshotSequence(String)
	 * @see #startScreenshotSequence(String, int, int, int)
	 * @see #stopScreenshotSequence()
	 * @see #getScreenshotSequenceSize(String, boolean, int)
	 */
	public BufferedImage getScreenshotSequence(String filename, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		if(filename==null || isScreenshotSequenceRunning()){
			throw new IllegalThreadStateException("Screenshot sequence name is null or the collecting-thread is still running.!");
		}
		if(index<0){
			throw new IllegalThreadStateException("The index should be equal to or greater than 0!");
		}

		Properties props = prepSoloDispatch(Message.cmd_getscreenshotsequenceindex);
		BufferedImage image = null;
		
		props.setProperty(Message.PARAM_NAME, filename);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		
		String encodedImageString = getSingleObject(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout);
		
		if(encodedImageString!=null){
			image = getBase64EncodedImage(encodedImageString, filename+"_"+index);
		}
		
		return image;
	}
	
    /** static android sdk tool to the one appropriate for the OS (Windows or Unix). */
	protected static AndroidTools androidsdk = null;
	
	/**
	 * This method is used to retrieve all screenshots of sequence from the device.<br>
	 * <b>REQUIRES: </b><br>
	 * As android command 'adb pull' is used to get the sequence, so this method needs to know where to find<br>
	 * 'adb' command, so user MUST set 'android sdk path' to Environment ANDROID_HOME or ANDROID_SDK<br>
	 * It is MUST be called in following order:<br>
	 * 1. Call {@link #startScreenshotSequence(String)}/{@link #startScreenshotSequence(String, int, int, int)}<br>
	 * 2. Call {@link #stopScreenshotSequence()}<br>
	 * 3. Call {@link #getScreenshotSequence(String, boolean, String, String)} to get all images.<br>
	 * @param filename, String, The root filename prefix for the screenshots.
	 * @param onlyLasttime,	boolean, if true, get only the sequence generated last time; if false, get all.
	 * @param destinationDir, String, the local directory to store the sequence image.
	 * @param serialNumber, String, the serial number of the device; can be null if only one device is attached.
	 * @return boolean, true if at least one of the sequence is got successfully.
	 * Field {@link #_last_remote_result} contains the returned Properties object. 
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getscreenshotsequence
	 * (in ):PARAM_NAME=String
	 * (in ):PARAM_ONLYVISIBLE=boolean, if get only the sequence generated last time or get all.
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String, a set of sequence name delimited by ';'. Such as ;FILE_0;FILE_1;FILE_2;FILE_3
	 * </pre>
	 * @throws IllegalThreadStateException
	 * @throws RemoteException
	 * @throws TimeoutException
	 * @throws ShutdownInvocationException
	 * @throws RemoteSoloException
	 * @see #startScreenshotSequence(String)
	 * @see #startScreenshotSequence(String, int, int, int)
	 * @see #stopScreenshotSequence()
	 */	
	public boolean getScreenshotSequence(String filename, boolean onlyLasttime, String destinationDir, String serialNumber) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		boolean success = false;
		
		if(filename==null || 
		   destinationDir==null || 
		   isScreenshotSequenceRunning()){
			throw new IllegalThreadStateException("Screenshot sequence name is null or the directory is null or the collecting-thread is still running.!");
		}

		File dir = new File(destinationDir);
		if(!dir.exists()){
			throw new IllegalThreadStateException(destinationDir+" does NOT exit.");
		}
		if(!dir.isDirectory()){
			throw new IllegalThreadStateException(destinationDir+" is NOT directory.");
		}
		//Remove the last file separator '\', adb pull will complain about it.
		if(destinationDir.endsWith(File.separator)) destinationDir = destinationDir.substring(0, destinationDir.length()-File.separator.length());
		
		Properties props = prepSoloDispatch(Message.cmd_getscreenshotsequence);
		props.setProperty(Message.PARAM_NAME, filename);
		props.setProperty(Message.PARAM_ONLYVISIBLE, Boolean.toString(onlyLasttime));
		List<String> absolutePaths = getCurrentObjects(props);
		
		try{
			//Use "adb pull" to retrieve the sequence image from device/emulator and save on local machine
			String[] pull = {"pull", "", destinationDir};
			if(serialNumber!=null && !serialNumber.isEmpty()){
				DUtilities.USE_DEVICE_SERIAL = "-s "+serialNumber;
			}
			
			Process2 process = null;
			if(androidsdk==null) androidsdk = DUtilities.getAndroidTools(null);
			
			for(int i=0;i<absolutePaths.size();i++){
				pull[1] = absolutePaths.get(i);
				try {
					debug("Saving '"+absolutePaths.get(i)+"' to '"+destinationDir+"'");
					process = androidsdk.adb(DUtilities.addDeviceSerialParam(pull));
					process.discardStdout().discardStderr().waitForSuccess();
					success = true;
				} catch (Exception ignore) {
					//If fail to get one file, maybe we should continue to try other files.
					debug("During get sequence '"+absolutePaths.get(i)+"', Met Exception="+ignore.getMessage());
				} finally{
					try{ process.destroy(); process = null;}catch(Exception x){}
				}
			}
			
		}catch(Throwable e){
			debug("Met "+e.getClass().getSimpleName()+":"+e.getMessage());
		}

		return success;
	}
	
	/**  The last screenshotSequenceName we are processing or processed. */
	private String screenshotSequenceName = "";
	/**
	 * If this field is true, {@link #startScreenshotSequence(String)}/{@link #startScreenshotSequence(String, int, int, int)}
	 * will not be permitted to call.<br>
	 * This field will be set to: <br>
	 * True,  if we have called {@link #startScreenshotSequence(String)}/{@link #startScreenshotSequence(String, int, int, int)}<br>
	 * False, if we have called {@link #stopScreenshotSequence()}<br>
	 */
	private boolean isScreenshotSequenceRunning = false;

	synchronized boolean isScreenshotSequenceRunning() {
		return isScreenshotSequenceRunning;
	}

	synchronized void setScreenshotSequenceRunning(boolean isScreenshotSequenceRunning) {
		if(isScreenshotSequenceRunning){//If we want to set the running state to true
			if(this.isScreenshotSequenceRunning)//If there is already a thread running, throw Exception
				throw new IllegalThreadStateException("An other thread is working to collect screenshot sequence for '"+this.screenshotSequenceName+"'!");
		}
		this.isScreenshotSequenceRunning = isScreenshotSequenceRunning;
	}
	
	/**
	 * Take a screenshot and retrieve the stored JPG image.
	 * The app must have the android.permission.WRITE_EXTERNAL_STORAGE permission.
	 * @return BufferedImage 	The JPG image of the screenshot. null on error.  
	 * Field {@link #_last_remote_result} contains the returned Properties object. 
	 * Upon success, 
	 * there will also be a PARAM_NAME+"FILE" property containing an absolute file path to 
	 * the temporary binary file containing the JPG image. The temporary file will be  
	 * timestamped with a "RobotiumRCScreenshot" prefix.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_takescreenshot
	 * (in ):PARAM_NAME= "RobotiumRCScreenShot-"+ datetimestamp
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the encoded JPG image./Or an error message.
	 * (out):PARAM_NAME+"FILE"=Absolute Path to temp screenshot image.
	 * <p>
	 * 
	 * <p>
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 * @see #getBase64EncodedImage(String, String)
	 */
	public BufferedImage takeScreenshot() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		DateFormat f = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		String filename = "RobotiumRCScreenShot-"+ f.format(new Date());
		return takeScreenshot(filename);
	}

	/**
	 * Take a screenshot and retrieve the stored JPG image.
	 * The app must have the android.permission.WRITE_EXTERNAL_STORAGE permission.
	 * @param filename 			The root filename for the returned JPG image.
	 * @return BufferedImage 	The JPG image of the screenshot. null on error.  
	 * Field {@link #_last_remote_result} contains the returned Properties object. Upon success, 
	 * there will also be a PARAM_NAME+"FILE" property containing an absolute file path to 
	 * the temporary binary file containing the JPG image. 
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_takescreenshot
	 * (in ):PARAM_NAME=String
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the encoded JPG image./Or an error message.
	 * (out):PARAM_NAME+"FILE"=Absolute Path to temp screenshot image.
	 * <p>
	 * 
	 * <p>
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getSingleObject(Properties)
	 * @see Message
	 * @see #getBase64EncodedImage(String, String)
	 */
	public BufferedImage takeScreenshot(String filename) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		BufferedImage image = null;

		Properties props = prepSoloDispatch(Message.cmd_takescreenshot);
		props.setProperty(Message.PARAM_NAME, filename);
		
		//Get the encoded png image string from the remote result
		String encodedImageString = getSingleObject(props);
		if(encodedImageString!=null){
			image = getBase64EncodedImage(encodedImageString, filename);
		}
		return image;
	}

	/**
	 * Take a screenshot and retrieve the stored JPG image.
	 * The app must have the android.permission.WRITE_EXTERNAL_STORAGE permission.
	 * @param filename 	String,	The root filename for the returned JPG image.
	 * @param quality 	int,	The compression rate. From 0 (compress for lowest size) to 100 (compress for maximum quality)
	 * @return BufferedImage 	The JPG image of the screenshot. null on error.  
	 * Field {@link #_last_remote_result} contains the returned Properties object. Upon success, 
	 * there will also be a PARAM_NAME+"FILE" property containing an absolute file path to 
	 * the temporary binary file containing the JPG image. 
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_takescreenshotquality
	 * (in ):PARAM_NAME=String
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the encoded JPG image./Or an error message.
	 * (out):PARAM_NAME+"FILE"=Absolute Path to temp screenshot image.
	 * (in ):PARAM_QUALITY=int, the compression rate. From 0 (compress for lowest size) to 100 (compress for maximum quality)
	 * <p>
	 * 
	 * <p>
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see SoloRemoteControl#performRemotePropsCommand(Properties, int, int, int)
	 * @see #_last_remote_result
	 * @see Message
	 * @see #getBase64EncodedImage(String, String)
	 */	
	public BufferedImage takeScreenshot(String filename, int quality) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		BufferedImage image = null;

		Properties props = prepSoloDispatch(Message.cmd_takescreenshotquality);
		props.setProperty(Message.PARAM_NAME, filename);
		props.setProperty(Message.PARAM_QUALITY, String.valueOf(quality));
		
		//Get the encoded png image string from the remote result
		String encodedImageString = getSingleObject(props);
		if(encodedImageString!=null){
			image = getBase64EncodedImage(encodedImageString, filename);
		}
		return image;
	}
	
	/**
	 * 
	 * @param encodedImageString, String, Base64 encoded string of an image.
	 * @param outputFileName, String, the file to store the image.
	 * @return	BufferedImage
	 * @see #takeScreenshot(String)	
	 */
	private BufferedImage getBase64EncodedImage(String encodedImageString, String outputFileName){
		String debugmsg = getClass().getName()+".getBase64EncodedImage() ";
		BufferedImage image = null;
		
		try {
			ByteArrayInputStream instream = new ByteArrayInputStream(encodedImageString.getBytes());
			// Create the file to contain the jpg image
			File fn = File.createTempFile(outputFileName, ".jpg");
			FileOutputStream outstream = new FileOutputStream(fn);
			// Decode the "encoded jpg image string" to a normal jpg image.
			Base64Decoder decoder = new Base64Decoder(instream, outstream);
			decoder.process();
			instream.close();
			outstream.close();
			// Read the jpg image file
			image = ImageIO.read(fn);
			_last_remote_result.setProperty(Message.PARAM_NAME + "FILE", fn.getAbsolutePath());
		} catch (IOException e) {
			debug(debugmsg + " Met Exception " + e.getMessage());
		} catch (Base64FormatException e) {
			debug(debugmsg + " Met Exception " + e.getMessage());
		}
		
		return image;
	}
	
	/**
	 * Enter text into a given EditText.
	 * @param String UID reference for the EditText to enter text into. 
	 * @param text String to enter into the EditText field.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_typetextuid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_REFERENCE=String UID 
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean typeText(String uidEditText, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_typetextuid);
		props.setProperty(Message.PARAM_REFERENCE, uidEditText);
		props.setProperty(Message.PARAM_TEXT, text);
		return runBooleanResult(props);
	}
	
	/**
	 * Enter text in an EditText with a given index.
	 * @param index of the EditText. 0 if only one is available. 
	 * @param text String to enter into the EditText field.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_typetext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String  
	 * (in ):PARAM_INDEX=int 
	 * (in ):PARAM_TEXT=String 
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #runBooleanResult(Properties)
	 * @see Message
	 */
	public boolean typeText(int index, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_typetext);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		props.setProperty(Message.PARAM_TEXT, text);
		return runBooleanResult(props);
	}
	
	/**
	 * Waits for the condition to be satisfied. -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param condition, Condition - the instance of Condition
	 * @param timeout, int - the amount of time in milliseconds to wait
	 * @return true if the condition is satified within timeout. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforcondition
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:true if the condition is satisfied withing timeout
	 * (in):PARAM_OBJECT=Condition: the encoded string of object Condition
	 * (in):PARAM_TIMEOUT=int: the amount of time in milliseconds to wait.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */	
	public boolean waitForCondition(Condition condition, final int timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforcondition);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(condition));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));

		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout+stime);
		return getRemoteBooleanResult(success, Message.cmd_waitforcondition, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Clears text in a WebElement matching the specified By object.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param by, By - the instance of By. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_cleartextinwebelement
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */		
	public boolean clearTextInWebElement(By by) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_cleartextinwebelement);
		
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		return runBooleanResult(props);
	}

	/**
	 * Clicks a WebElement matching the specified By object.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param by, By - the instance of By. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonwebelement
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnWebElement(By by) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_clickonwebelement);
		
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		return runBooleanResult(props);
	}
	
	/**
	 * Clicks a WebElement matching the specified By object.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param by, By - the instance of By. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @param match, int - if multiple objects match, this determines which one to click
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonwebelementindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * (in):PARAM_MATCH=int: if multiple objects match, this determines which one to click
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnWebElement(By by, int match) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_clickonwebelementindex);
		
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		props.setProperty(Message.PARAM_MATCH, String.valueOf(match));
		return runBooleanResult(props);
	}
	
	/**
	 * Clicks a WebElement matching the specified By object.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param by, By - the instance of By. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @param match, int - if multiple objects match, this determines which one to click
	 * @param scroll, boolean - if scrolling should be performed
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonwebelementindexscroll
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * (in):PARAM_MATCH=int: if multiple objects match, this determines which one to click
	 * (in):PARAM_SCROLL=boolean: if scrolling should be performed
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnWebElement(By by, int match, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_clickonwebelementindexscroll);
		
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		props.setProperty(Message.PARAM_MATCH, String.valueOf(match));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		return runBooleanResult(props);
	}

	/**
	 * Clicks a WebElement stored in cache with a reference.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param webElementUID, String - the UID used to get a WebElement object from the cache
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_clickonwebelementuid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_REFERENCE=String: the UID used to get a WebElement object from the cache
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResultWithUID(String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean clickOnWebElement(String webElementUID) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		return runBooleanResultWithUID(Message.cmd_clickonwebelementuid, webElementUID);
	}

	/**
	 * Enters text in a WebElement matching the specified By object.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param by, By - the instance of By. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @param text, String - the text to enter in the WebElement field
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_entertextinwebelement
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * (in):PARAM_TEXT=String: the text to enter in the WebElement field
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean enterTextInWebElement(By by, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_entertextinwebelement);
		
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		props.setProperty(Message.PARAM_TEXT, text);
		return runBooleanResult(props);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current Views<br>
	 * matching the specified class located in the focused Activity or Dialog. -- <b>Robotium 4.1 required</b>.<br>br> 
	 * @param classFullName String, the specified class name to match for current views.
	 * @return ArrayList of 0 or more String UIDs for all the current Views matching the specified class located in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentviewsbyclass
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual View objects 
	 * stored in a remote cache.
	 * (in ):PARAM_CLASS=String: the specified class name to match for current views
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public ArrayList<String> getCurrentViews(String classFullName) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getcurrentviewsbyclass);
		props.setProperty(Message.PARAM_CLASS, classFullName);
		return getCurrentObjects(props);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current Views<br>
	 * matching the specified class under the specified parent. -- <b>Robotium 4.1 required</b>.<br>br> 
	 * @param classFullName String, the specified class name to match for current views.
	 * @param parentViewUID String, the specified parent where the views locate
	 * @return ArrayList of 0 or more String UIDs for all the current Views matching the specified class located in the focused window.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentviewsbyclassandparent
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual View objects 
	 * stored in a remote cache.
	 * (in ):PARAM_CLASS=String: the specified class name to match for current views
	 * (in ):PARAM_REFERENCE=String: the specified parent where the views locate
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public ArrayList<String> getCurrentViews(String classFullName, String parentViewUID) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getcurrentviewsbyclassandparent);
		props.setProperty(Message.PARAM_CLASS, classFullName);
		props.setProperty(Message.PARAM_REFERENCE, parentViewUID);
		return getCurrentObjects(props);
	}

	/**
	 * Returns an ArrayList of String UIDs for all the current WebElements displayed in the active WebView -- <b>Robotium 4.1 required</b>.<br>br> 
	 * @return ArrayList of 0 or more String UIDs for all the current WebElements displayed in the active WebView.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentwebelements
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual WebElement objects 
	 * stored in a remote cache.
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public ArrayList<String> getCurrentWebElements() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		return getCurrentObjects(Message.cmd_getcurrentwebelements);
	}
	
	/**
	 * Returns an ArrayList of String UIDs for all the current WebElements displayed in the active WebView<br>
	 * matching the specified By object -- <b>Robotium 4.1 required</b>.<br>br> 
	 * @param by the By object. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @return ArrayList of 0 or more String UIDs for all the current WebElements displayed in the active WebView.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getcurrentwebelementsby
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for actual WebElement objects 
	 * stored in a remote cache.
	 * (in ):PARAM_OBJECT=By: the encoded string of object By 
	 * <p>
	 * REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */	
	public ArrayList<String> getCurrentWebElements(By by) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getcurrentwebelementsby);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		return getCurrentObjects(props);
	}
	
	/**
	 * Returns an UID for the WebElement displayed in the active WebView matching the specified By object and index-- <b>Robotium 4.1 required</b>.<br>br> 
	 * @param by the By object. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @param index, int the index of the WebElement to get, start from 0
	 * @return String, UID for the matched WebElement displayed in the active WebView.
	 *                 null will be returned if something wrong has happened.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getwebelement
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference key for actual WebElement object 
	 * stored in a remote cache.
	 * (in ):PARAM_OBJECT=By: the encoded string of object By 
	 * (in ):PARAM_INDEX=int: the index of the WebElement to get, start from 0
	 * <p></pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */	
	public String getWebElement(By by, int index) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getwebelement);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		props.setProperty(Message.PARAM_INDEX, String.valueOf(index));
		List<String> list = getCurrentObjects(props);
		return list.size()>0? (String)list.get(0):null;
	}
	
	/**
	 * Returns the current web page URL -- <b>Robotium 4.1 required</b>.<br>br> 
	 * @return String, the current web page URL
	 *                 null will be returned if something wrong has happened.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getweburl
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String the current web page URL
	 * <p></pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */		
	public String getWebUrl() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getweburl);
		return getSingleObject(props);
	}
	
	/**
	 * Hides the soft keyboard. -- <b>Robotium 4.1 required</b>.<br>br> 
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_hidesoftkeyboard
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String a general success message
	 * <p></pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */		
	public boolean hideSoftKeyboard() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_hidesoftkeyboard);
		return runBooleanResult(props);
	}
	
	/**
	 * Types text in a WebElement matching the specified By object.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param by, By - the instance of By. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @param text, String - the text to type in the WebElement field
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_typetextinwebelement
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * (in):PARAM_TEXT=String: the text to enter in the WebElement field
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */	
	public boolean typeTextInWebElement(By by, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_typetextinwebelement);
		
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		props.setProperty(Message.PARAM_TEXT, text);
		return runBooleanResult(props);
	}
	
	/**
	 * Types text in a WebElement matching the specified By object and index.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param by, By - the instance of By. Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @param text, String - the text to type in the WebElement field
	 * @param match, String - if multiple objects match, this determines which one will be typed in
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_typetextinwebelementindex
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * (in):PARAM_TEXT=String: the text to enter in the WebElement field
	 * (in):PARAM_INDEX=String: if multiple objects match, this determines which one will be typed in
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */	
	public boolean typeTextInWebElement(By by, String text, int match) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_typetextinwebelementindex);
		
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		props.setProperty(Message.PARAM_TEXT, text);
		props.setProperty(Message.PARAM_INDEX, String.valueOf(match));
		return runBooleanResult(props);
	}
	
	/**
	 * Types text in a WebElement stored in remote cache by a reference UID.  -- <b>Robotium 4.1 required</b>.<br>
	 * <br>
	 * 
	 * @param webElementUID, sTRING - the UID used to get the WebElement object from the remote cache.
	 * @param text, String - the text to type in the WebElement field
	 * @return true if the command was successfully executed, false if not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_typetextinwebelementuid
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:String:the information of the WebElement
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * (in):PARAM_TEXT=String: the text to enter in the WebElement field
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */	
	public boolean typeTextInWebElement(String webElementUID, String text) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_typetextinwebelementuid);
		
		props.setProperty(Message.PARAM_REFERENCE, webElementUID);
		props.setProperty(Message.PARAM_TEXT, text);
		return runBooleanResult(props);
	}
	
	/**
	 * Waits for an Activity matching the specified class. Default timeout is 20 seconds.  -- <b>Robotium 4.1 required</b>.<br>
	 * @param activityClassName String - the Activity's full qualified class name
	 * @return true if Activity appears before the timeout and false if it does not. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforactivitybyclass
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the activity appear
	 * (in ):PARAM_CLASS=String the Activity's full qualified class name
	 * (in/out):PARAM_CLASS=String (in: the class name of the Activity to wait for e.g. "org.package.MyActivity", out: the actual Activity class name)
	 * (out):PARAM_NAME=String  the actual Activity name
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForActivityByClass(String activityClassName) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforactivitybyclass);
		props.setProperty(Message.PARAM_CLASS, activityClassName);
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_waitforactivitybyclass, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for an Activity matching the specified class.  -- <b>Robotium 4.1 required</b>.<br>
	 * @param activityClassName String - the Activity's full qualified class name
	 * @param timeout int - the timeout to wait for an activity, in milliseconds
	 * @return true if Activity appears before the timeout and false if it does not. 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforactivitybyclasstimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the activity appear
	 * (in/out):PARAM_CLASS=String (in: the class name of the Activity to wait for e.g. "org.package.MyActivity", out: the actual Activity class name)
	 * (out):PARAM_NAME=String  the actual Activity name
	 * (in ):PARAM_TIMEOUT=int the timeout to wait for an activity, in milliseconds
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */	
	public boolean waitForActivityByClass(String activityClassName, int timeout) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforactivitybyclasstimeout);
		props.setProperty(Message.PARAM_CLASS, activityClassName);
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout+stime);
		return getRemoteBooleanResult(success, Message.cmd_waitforactivitybyclasstimeout, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a WebElement matching the specified By object. Default timeout is 20 seconds. -- <b>Robotium 4.1 required</b>.<br>
	 * @param by By, Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @return true if WebElement appears 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforwebelement
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the WebElement appears
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForWebElement(By by) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforwebelement);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		boolean success = runBooleanResult(props);
		return getRemoteBooleanResult(success, Message.cmd_waitforwebelement, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a WebElement matching the specified By object. -- <b>Robotium 4.1 required</b>.<br>
	 * @param by By, Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @param timeout int, the the amount of time in milliseconds to wait
	 * @param scroll boolean, if scrolling should be performed
	 * @return true if WebElement appears 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforwebelementtimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the WebElement appears
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * (in):PARAM_TIMEOUT=int: the the amount of time in milliseconds to wait 
	 * (in):PARAM_SCROLL=boolean: if scrolling should be performed
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForWebElement(By by, int timeout, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforwebelementtimeout);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		
		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout+stime);
		return getRemoteBooleanResult(success, Message.cmd_waitforwebelementtimeout, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Waits for a WebElement matching the specified By object. -- <b>Robotium 4.1 required</b>.<br>
	 * @param by By, Examples are: {@code By.id("id")} and {@code By.name("name")}
	 * @param timeout int, the the amount of time in milliseconds to wait
	 * @param scroll boolean, if scrolling should be performed
	 * @param minimumNumberOfMatches int, the minimum number of matches that are expected to be found. {@code 0} means any number of matches
	 * @return true if WebElement appears 
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_waitforwebelementminmatchtimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String:boolean:if the WebElement appears
	 * (in):PARAM_OBJECT=By: the encoded string of object By
	 * (in):PARAM_TIMEOUT=int: the the amount of time in milliseconds to wait 
	 * (in):PARAM_SCROLL=boolean: if scrolling should be performed
	 * (in):PARAM_MATCH=int: the minimum number of matches that are expected to be found. {@code 0} means any number of matches
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @throws RemoteSoloException  -- if the command was not executed successfully in remote side. 
	 * @see #runBooleanResult(Properties)
	 * @see #getRemoteBooleanResult(boolean, String, String)
	 * @see #_last_remote_result
	 * @see Message
	 */
	public boolean waitForWebElement(By by, int minimumNumberOfMatches, int timeout, boolean scroll) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_waitforwebelementminmatchtimeout);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(by));
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(timeout));
		props.setProperty(Message.PARAM_SCROLL, String.valueOf(scroll));
		props.setProperty(Message.PARAM_MATCH, String.valueOf(minimumNumberOfMatches));

		int stime = timeout > 0 ? (int)Math.ceil(timeout/1000) : 0;
		boolean success = runBooleanResult(props, default_ready_stimeout, default_running_stimeout, default_result_stimeout+stime);
		return getRemoteBooleanResult(success, Message.cmd_waitforwebelement, Message.KEY_REMOTERESULTINFO);
	}
	
	/**
	 * Zooms in or out if startPoint1 and startPoint2 are larger or smaller then endPoint1 and endPoint2.  -- <b>Requires API level >= 14. Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param startPoint1 First "finger" down on the screen.
	 * @param startPoint2 Second "finger" down on the screen.
	 * @param endPoint1 Corresponding ending point of startPoint1.
	 * @param endPoint2 Corresponding ending point of startPoint2.
	 * @return true if the command executed successfully, false if it did not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_pinchtozoom
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String: nothing if command succeeds; error message if command fails.
	 * (in):PARAM_OBJECT=PointCollection: a collection of PointF used to zoom
	 * </pre>
	 * @throws IllegalThreadStateException
	 * @throws RemoteException
	 * @throws TimeoutException
	 * @throws ShutdownInvocationException
	 * @throws RemoteSoloException
	 * @see {@link #runBooleanResult(Properties)}
	 * @see #_last_remote_result
	 */
	public boolean pinchToZoom(PointF startPoint1, PointF startPoint2, PointF endPoint1, PointF endPoint2) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_pinchtozoom);
		ObjectCollection<PointF> points = new ObjectCollection<PointF>();
		points.addToObjectList(startPoint1);
		points.addToObjectList(startPoint2);
		points.addToObjectList(endPoint1);
		points.addToObjectList(endPoint2);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(points));
		
		return runBooleanResult(props);
	}
	
	/**
	 * Draws two semi-circles at the specified centers. Both circles are larger than rotateSmall(). -- <b>Requires API level >= 14. Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param center1 Center of semi-circle drawn from [0, Pi]
	 * @param center2 Center of semi-circle drawn from [Pi, 3*Pi]
	 * @return true if the command executed successfully, false if it did not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_rotatelarge
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String: nothing if command succeeds; error message if command fails.
	 * (in):PARAM_OBJECT=ObjectCollection<PointF>: a collection of PointF used to rotate
	 * </pre>
	 * @throws IllegalThreadStateException
	 * @throws RemoteException
	 * @throws TimeoutException
	 * @throws ShutdownInvocationException
	 * @throws RemoteSoloException
	 * @see {@link #runBooleanResult(Properties)}
	 * @see #_last_remote_result
	 */
	public boolean rotateLarge(PointF center1, PointF center2) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_rotatelarge);
		ObjectCollection<PointF> points = new ObjectCollection<PointF>();
		points.addToObjectList(center1);
		points.addToObjectList(center2);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(points));
		
		return runBooleanResult(props);
	}
	
	/**
	 * Draws two semi-circles at the specified centers. Both circles are smaller than rotateLarge(). -- <b>Requires API level >= 14. Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param center1 Center of semi-circle drawn from [0, Pi]
	 * @param center2 Center of semi-circle drawn from [Pi, 3*Pi]
	 * @return true if the command executed successfully, false if it did not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_rotatesmall
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String: nothing if command succeeds; error message if command fails.
	 * (in):PARAM_OBJECT=ObjectCollection<PointF>: a collection of PointF used to rotate
	 * </pre>
	 * @throws IllegalThreadStateException
	 * @throws RemoteException
	 * @throws TimeoutException
	 * @throws ShutdownInvocationException
	 * @throws RemoteSoloException
	 * @see {@link #runBooleanResult(Properties)}
	 * @see #_last_remote_result
	 */
	public boolean rotateSmall(PointF center1, PointF center2) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_rotatesmall);
		ObjectCollection<PointF> points = new ObjectCollection<PointF>();
		points.addToObjectList(center1);
		points.addToObjectList(center2);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(points));
		
		return runBooleanResult(props);
	}

	/**
	 * Swipes with two fingers in a linear path determined by starting and ending points. -- <b>Requires API level >= 14. Robotium 4.1+ required.</b>.<br>
	 * 
	 * @param startPoint1 First "finger" down on the screen.
	 * @param startPoint2 Second "finger" down on the screen.
	 * @param endPoint1 Corresponding ending point of startPoint1
	 * @param endPoint2 Corresponding ending point of startPoint2
	 * @return true if the command executed successfully, false if it did not.
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_swipe
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String: nothing if command succeeds; error message if command fails.
	 * (in):PARAM_OBJECT=ObjectCollection<PointF>: a collection of PointF used to swipe
	 * </pre>
	 * @throws IllegalThreadStateException
	 * @throws RemoteException
	 * @throws TimeoutException
	 * @throws ShutdownInvocationException
	 * @throws RemoteSoloException
	 * @see {@link #runBooleanResult(Properties)}
	 * @see #_last_remote_result
	 */
	public boolean swipe(PointF startPoint1, PointF startPoint2, PointF endPoint1, PointF endPoint2) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException, RemoteSoloException{
		Properties props = prepSoloDispatch(Message.cmd_swipe);
		ObjectCollection<PointF> points = new ObjectCollection<PointF>();
		points.addToObjectList(startPoint1);
		points.addToObjectList(startPoint2);
		points.addToObjectList(endPoint1);
		points.addToObjectList(endPoint2);
		props.setProperty(Message.PARAM_OBJECT, Message.encodeBase64Object(points));
		
		return runBooleanResult(props);
	}
	
	/* RobotiumUtils methods begin */
	/**
	 * Filters Views based on the given class type.  -- <b>Robotium 4.1 required</b>.<br>
	 * @param className, String, the class name used to filter views.
	 * @param viewUIDList, List, a list of UID (unique ID) reference keys for Views to be filtered.
	 * @return List a List with filtered views' UID.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_utilsfilterviews
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for Filtered Views stored in a remote cache.
	 * (in ):PARAM_CLASS=String, the class name used to filter views.
	 * (in ):PARAM_REFERENCES=String, containing the UID (unique ID) reference keys for Views to be filtered.
	 * <p>
	 * PARAM_REFERENCES, KEY_REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	List<String> filterViews(String className,  List<String>viewUIDList) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_utilsfilterviews);
		props.setProperty(Message.PARAM_CLASS, className);
		props.setProperty(Message.PARAM_REFERENCES, Message.convertToDelimitedString(viewUIDList));
		return getCurrentObjects(props);
	}

	/**
	 * Filters a collection of Views and returns a list that contains only Views
	 * with text that matches a specified regular expression.  -- <b>Robotium 4.1 required</b>.<br>
	 * 
	 * @param viewUIDList, List, a list of UID (unique ID) reference keys for Views to be filtered.
	 * @param regex, String, The text regular pattern to search for.
	 * @return List a List with filtered views' UID.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_utilsfilterviewsbytext
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for Filtered Views stored in a remote cache.
	 * (in ):PARAM_REFERENCES=String, containing the UID (unique ID) reference keys for Views to be filtered.
	 * (in ):PARAM_REGEX_STRING=String, The text regular pattern to search for.
	 * <p>
	 * PARAM_REFERENCES, KEY_REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	List<String> filterViewsByText(List<String>viewUIDList, String regex) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_utilsfilterviewsbytext);
		props.setProperty(Message.PARAM_REFERENCES, Message.convertToDelimitedString(viewUIDList));
		props.setProperty(Message.PARAM_REGEX_STRING, regex);
		return getCurrentObjects(props);
	}
	
	/**
	 * Filters all Views not within the given set.  -- <b>Robotium 4.1 required</b>.<br>
	 * 
	 * @param classNameList, List, contains 'full qualified class name' for all classes that are OK to pass the filter
	 * @param viewUIDList, List, a list of UID (unique ID) reference keys for Views to be filtered.
	 * @param regex, String, The text regular pattern to search for.
	 * @return List a List with filtered views' UID.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_utilsfilterviewstoset
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for Filtered Views stored in a remote cache.
	 * (in ):PARAM_CLASSES=String, a delimited string containing classname for all classes that are OK to pass the filter
	 * (in ):PARAM_REFERENCES=String, containing the UID (unique ID) reference keys for Views to be filtered.
	 * <p>
	 * PARAM_CLASSES, PARAM_REFERENCES, KEY_REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	List<String> filterViewsToSet(List<String>classNameList, List<String>viewUIDList) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_utilsfilterviewstoset);
		props.setProperty(Message.PARAM_CLASSES, Message.convertToDelimitedString(classNameList));
		props.setProperty(Message.PARAM_REFERENCES, Message.convertToDelimitedString(viewUIDList));
		return getCurrentObjects(props);
	}

	/**
	 * Checks if a View matches a certain string and returns the amount of total matches.  -- <b>Robotium 4.1 required</b>.<br>
	 * 
	 * @param regex, String, The text regular pattern to match.
	 * @param textViewUID, String, the UID (unique ID) reference key for View to be matched.
	 * @param matchedViewUIDList, Set<String>, set of UID for views that have matched
	 * @return int, number of total matches  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_utilsfilterviewstoset
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for matched Views stored in a remote cache.
	 * (in ):PARAM_REGEX_STRING=String, The text regular pattern to match.
	 * (in ):PARAM_REFERENCE=String, containing the UID (unique ID) reference keys for Views to be matched.
	 * <p>
	 * KEY_REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	int getNumberOfMatches(String regex, String textViewUID, Set<String>matchedViewUIDList) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_utilsgetnumberofmatches);
		props.setProperty(Message.PARAM_REGEX_STRING, regex);
		props.setProperty(Message.PARAM_REFERENCE, textViewUID);
		List<String> matchedList = getCurrentObjects(props);

		if(matchedViewUIDList!=null){
			matchedViewUIDList.addAll(matchedList);
			return matchedViewUIDList.size();
		}else{
			return matchedList.size();
		}
	}
	
	/**
	 * Removes invisible Views.  -- <b>Robotium 4.1 required</b>.<br>
	 * 
	 * @param viewUIDList, List, a list of UID (unique ID) reference keys for Views to be filtered.
	 * @return List, a List with filtered views' UID.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_utilsremoveinvisibleviews
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for Filtered Views stored in a remote cache.
	 * (in ):PARAM_REFERENCES=String, containing the UID (unique ID) reference keys for Views to be filtered.
	 * <p>
	 * PARAM_REFERENCES, KEY_REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	List<String> removeInvisibleViews(List<String>viewUIDList) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_utilsremoveinvisibleviews);
		props.setProperty(Message.PARAM_REFERENCES, Message.convertToDelimitedString(viewUIDList));
		return getCurrentObjects(props);
	}
	
	/**
	 * Orders Views by their location on-screen.  -- <b>Robotium 4.1 required</b>.<br>
	 * 
	 * @return
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_utilssortviewsbylocationonscreen
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for sorted Views stored in a remote cache.
	 * (in ):PARAM_REFERENCES=String, containing the UID (unique ID) reference keys for Views to be sorted.
	 * <p>
	 * PARAM_REFERENCES, KEY_REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @param viewUIDList, List, a list of UID (unique ID) reference keys for Views to be sorted.
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	void sortViewsByLocationOnScreen(List<String>viewUIDList) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_utilssortviewsbylocationonscreen);
		props.setProperty(Message.PARAM_REFERENCES, Message.convertToDelimitedString(viewUIDList));
		viewUIDList.clear();
		viewUIDList.addAll(getCurrentObjects(props));
	}
	
	/**
	 * Orders Views by their location on-screen.  -- <b>Robotium 4.1 required</b>.<br>
	 * 
	 * @return
	 * Field {@link #_last_remote_result} contains the returned Properties object.
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_utilssortviewsbylocationonscreenyfirst
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String containing the UID (unique ID) reference keys for sorted Views stored in a remote cache.
	 * (in ):PARAM_REFERENCES=String, containing the UID (unique ID) reference keys for Views to be sorted.
	 * (in ):PARAM_YAXISFIRST=boolean, Whether the y-axis should be compared before the x-axis.
	 * <p>
	 * PARAM_REFERENCES, KEY_REMOTERESULTINFO content format: ";UID;UID;UID"
	 * <p>
	 * The first character is the delimiter used to delimit each UID followed by each UID separated by the delimiter. 
	 * &nbsp;Each UID must be a unique String reference key to an object in the remote cache.
	 * </pre>
	 * @param viewUIDList, List, a list of UID (unique ID) reference keys for Views to be sorted.
	 * @param yAxisFirst, boolean, Whether the y-axis should be compared before the x-axis.
	 * @throws RemoteException -- if remote execution raised an Exception
	 * @throws TimeoutException -- if remote command did not complete in timeout period.
	 * @throws ShutdownInvocationException -- if remote clients have shutdown unexpectedly.
	 * @throws IllegalThreadStateException -- if the command was not sent for some reason.
	 * @see #getCurrentObjects(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	void sortViewsByLocationOnScreen(List<String>viewUIDList, boolean yAxisFirst) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_utilssortviewsbylocationonscreenyfirst);
		props.setProperty(Message.PARAM_REFERENCES, Message.convertToDelimitedString(viewUIDList));
		props.setProperty(Message.PARAM_YAXISFIRST, Boolean.toString(yAxisFirst));
		viewUIDList.clear();
		viewUIDList.addAll(getCurrentObjects(props));
	}
	/* RobotiumUtils methods end */
	
	/* Timeout methods begin */
	/**
	 * Sets the default timeout length of the waitFor methods. Its by default set to 20 000 milliseconds.  -- <b>Robotium 4.1+ required</b>.<br>
	 * @param milliseconds, int, the timeout of the waitFor methods, in milliseconds.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.  
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setlargetimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (in ):PARAM_TIMEOUT=int 
	 * </pre>
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	boolean setLargeTimeout(int milliseconds) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setlargetimeout);
		
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(milliseconds));
		return runBooleanResult(props);
	}
	
	/**
	 * Sets the default timeout length of the get, is, set, assert, enter and click methods. Its by default set to 10 000 milliseconds.  -- <b>Robotium 4.1+ required</b>.<br>
	 * @param milliseconds, int, the timeout of the get, is, set, assert, enter and click methods, in milliseconds.
	 * @return true if the command executed successfully, false if it did not.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.  
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_setsmalltimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (in ):PARAM_TIMEOUT=int 
	 * </pre>
	 * @see #runBooleanResult(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	boolean setSmallTimeout(int milliseconds) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_setsmalltimeout);
		
		props.setProperty(Message.PARAM_TIMEOUT, String.valueOf(milliseconds));
		return runBooleanResult(props);
	}
	
	/**
	 * Gets the default timeout length of the waitFor methods.  -- <b>Robotium 4.1+ required</b>.<br>
	 * @return int, the timeout in milliseconds.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.  
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getlargetimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String, the timeout in milliseconds
	 * </pre>
	 * @see #getSingleObject(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	int getLargeTimeout() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getlargetimeout);
		try{ return Integer.parseInt(getSingleObject(props));}
		catch(NumberFormatException x){
			debug("Can't get large timeout due to "+Message.getStackTrace(x));
			throw new IllegalThreadStateException(Message.getStackTrace(x));
		}
	}
	
	/**
	 * Gets the default timeout length of the get, is, set, assert, enter and click methods.  -- <b>Robotium 4.1+ required</b>.<br>
	 * @return int, the timeout in milliseconds.  
	 * Field {@link #_last_remote_result} contains the returned Properties object.  
	 * <p>
	 * The KEY_, PARAM_, STATUS_, cmd_, and target_ constants all come from the Message class and are 
	 * used here and in the device/emulator Robotium Test Runner.
	 * <p><pre>
	 * (in ):KEY_TARGET= solo 
	 * (in ):KEY_COMMAND= cmd_getsmalltimeout
	 * (out):KEY_ISREMOTERESULT=true
	 * (out):KEY_REMOTERESULTCODE=String:int:0=success/normal=STATUS_REMOTERESULT_OK
	 * (out):KEY_REMOTERESULTINFO=String, the timeout in milliseconds
	 * </pre>
	 * @see #getSingleObject(Properties)
	 * @see #_last_remote_result
	 * @see Message
	 */
	int getSmallTimeout() throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		Properties props = prepSoloDispatch(Message.cmd_getsmalltimeout);
		try{ return Integer.parseInt(getSingleObject(props));}
		catch(NumberFormatException x){
			debug("Can't get small timeout due to "+Message.getStackTrace(x));
			throw new IllegalThreadStateException(Message.getStackTrace(x));
		}
	}
	/* Timeout methods begin */
}