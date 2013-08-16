/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.solo;

import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.safs.sockets.DebugListener;
import org.safs.sockets.RemoteException;
import org.safs.sockets.ShutdownInvocationException;
import org.safs.sockets.SocketProtocol;

/**
 * Normally, this class would not be used directly.  Use the Solo class, instead.
 * Default abstract usage:
 * <p><pre>
 * SoloWorker solo = new SoloWorker();
 * solo.setLogsInterface(alog);
 * solo.initialize();
 * (use the API)
 * solo.shutdown();
 * </pre>
 * @see Solo<br>
 * @author Carl Nagle, SAS Institute, Inc.
 * @since
 * <br>(LeiWang)	Mar 09, 2012	Add methods to turn on/off debug log of protocol/runner.
 *                              Add method to shutdown remote service on device.
 * <br>(LeiWang)	Mar 09, 2012	Move static method parseStringArrayList() to Message class.
 */
public class SoloWorker implements DebugListener{

	public final String TAG = getClass().getSimpleName();
	/** How long to wait for a remote connection before issuing timeout.  Default 120 seconds. */
	public static int default_connect_stimeout = 120;
	/** How long to wait for a READY signal before issuing timeout. Default 120 seconds. */
	public static int default_ready_stimeout   = 120;
	/** How long to wait for a RUNNING signal after dispatch before issuing timeout. Default 60 seconds. */
	public static int default_running_stimeout = 60;
	/** How long to wait for a RESULT signal after dispatch before issuing timeout. Default 120 seconds. */
	public static int default_result_stimeout = 120;
	/** How long to wait for remote shutdown confirmation after dispatch before aborting the wait. Default 7 seconds. */
	public static int default_shutdown_stimeout = 7;
	/** Average Network TCP transaction latency allowance for sockets communications. Default 2 seconds. */
	public static int tcp_delay = 2; //previous use of + 1 was insufficient in Solo subclass
	
	
	public static String listenername = "Solo";
	
	/** Initializes to System.out */
	public static PrintStream out = System.out;
	/** Initializes to System.err */
	public static PrintStream err = System.err;
	
	protected LogsInterface log = null;
	protected SoloRemoteControl control = null;
	
	private int controllerPort = SocketProtocol.DEFAULT_CONTROLLER_PORT;
	private boolean portForwarding = true;
	
	public SoloWorker() {	}

	/**
	 * Set the LogsInterface to be used by the class instance.
	 * This call also attempts to set the ProtocolRunner to use the same LogsInterface.
	 * @param ilog
	 * @see LogsInterface
	 */
	public void setLogsInterface(LogsInterface ilog){
		log = ilog;
		try{ control.setLogsInterface(log);}catch(Exception x){}
	}

	/**
	 *@see DebugListener
	 */
	public String getListenerName() {return listenername; }

	/**
	 *@see DebugListener
	 */
	public void onReceiveDebug(String message) {
		debug(message);
	}
	
	/**
	 * Output debug messages to our LogsInterface, or to our out PrintStream if the LogsInterface is not set.
	 * @param message
	 * @see #out
	 */
	protected void debug(String message) {
		try{log.debug(message);}
		catch(Exception x){
			out.println(message);
		}
	}
	
	/**
	 * Called internally by the initialize() routine to get the desired instance/subclass of 
	 * SoloRemoteControl.  This routine returns a new instance of a SoloRemoteControl subclass 
	 * even if we already have a controller set.
	 * @return new SoloRemoteControl().  Subclasses may wish to provide a different subclass of 
	 * SoloRemoteControl().
	 */
	protected SoloRemoteControl createRemoteControl(){
		return new SoloRemoteControl();
	}
	
	/**
	 * @return current SoloRemoteControl subclass or null if not set.
	 */
	public SoloRemoteControl getRemoteControl(){
		return control;
	}
	
	/**
	 * Set the SoloRemoteControl instance to be used by this worker. 
	 * We will not override an existing controller unless force = true.
	 * @param controller
	 * @param force true to overwrite an existing controller with a different one, or null.
	 * @return calls {@link #getRemoteControl()}
	 */
	public SoloRemoteControl setRemoteControl(SoloRemoteControl controller, boolean force){
		if(control == null || force){
			control = controller;
		}
		return getRemoteControl();
	}
	
	/**
	 * Called to initialize RemoteControl communications with a remote client and get it to the Ready state. 
	 * However, the routine only performs this initialization if it is instancing a new remote controller. 
	 * If the remote controller already exists, then we must assume initialization of that remote control 
	 * object has opened or will happen elsewhere since it was not instanced by this class. 
	 * Initial default implementation performs:
	 * <p><pre>
	 * control = createRemoteControll();
	 * control.addListener(this);
	 * control.setLogsInterface(log);
	 * control.start();
	 * control.waitForRemoteConnected(default_connect_stimeout);
	 * control.waitForRemoteReady(default_ready_stimeout);
	 * </pre>
	 * @throws RemoteException -- if there is a problem with RemoteControl initialization.
	 * @throws TimeoutException -- if the initialization and remote connection does not complete in timeout period.
	 * @throws ShutdownInvocationException -- if the remote client unexpectedly performs a shutdown.
	 * @see #setRemoteControl(SoloRemoteControl, boolean)
	 * @see SoloRemoteControl#waitForRemoteReady(int)
	 * @see SoloRemoteControl#setLogsInterface(LogsInterface)
	 */
	public void initialize() throws RemoteException, TimeoutException, ShutdownInvocationException{
		if(control == null){
			control = createRemoteControl();
			control.setPortForwarding(portForwarding);
			control.setControllerPort(controllerPort);
			control.addListener(this);
			control.setLogsInterface(log);
			control.start();
			control.waitForRemoteConnected(default_connect_stimeout);
			control.waitForRemoteReady(default_ready_stimeout);
		}
	}
	
	/**
	 * Set the controller port where we will connect for messenger service.
	 * @param controllerPort
	 */
	public void setControllerPort(int controllerPort){
		this.controllerPort = controllerPort;
	}
	
	/**
	 * Set if we will forward 'controller port' to remote messenger service's port.
	 * @param portForwarding
	 */
	public void setPortForwarding(boolean portForwarding){
		this.portForwarding = portForwarding;
	}
	
	/**
	 * Turn on/off the runner's debug message.<br>
	 * This MUST be called after invoking {@link #initialize()}<br>
	 * @param enableDebug
	 */
	public void turnRunnerDebug(boolean enableDebug){
		String debugPrefix = TAG+".turnRunnerDebug() ";
		if(control==null || control.runner==null){
			debug(debugPrefix+" you MUST call initialize() before calling this method.");
		}else{
			control.runner._debugEnabled = enableDebug;
		}
	}
	
	/**
	 * Turn on/off the protocol's debug message.<br>
	 * This MUST be called after invoking {@link #initialize()}<br>
	 * @param enableDebug
	 */
	public void turnProtocolDebug(boolean enableDebug){
		String debugPrefix = TAG+".turnRunnerDebug() ";
		if(control==null || control.runner==null || control.runner.protocolserver==null){
			debug(debugPrefix+" you MUST call initialize() before calling this method.");
		}else{
			control.runner.protocolserver._debugEnabled = enableDebug;
		}
	}
	
	/** ": " */
	public static String CAUSE_SEP = ": ";
	/** "  OK  " */
	public static String PASS_SUFFIX = "  OK  ";
	/** "FAILED" */
	public static String FAIL_SUFFIX = "FAILED";

	/** 
	 * Set this value to false to bypass or ignore default failure processing. 
	 * This allows the API caller to handle the failures in their own way.
	 * @see #processFailure(String, String) 
	 */
	public boolean doProcessFailure = true;
		
	/**
	 * Handle the reporting or logging of action or test failures.
	 * <p>
	 * This is enabled by default.  Callers can override default failure processing by setting 
	 * doProcessFailure = false.
	 * <p>
	 * This implementation uses the LogsInterface that should be provided at or immediately 
	 * following the creation of the Class instance.  Subclasses may wish to use true 
	 * jUnit reporting or other mechanisms.
	 * <p>
	 * If the LogsInterface call throws an Exception for any reason--including a NullPointerException 
	 * because it was never provided or initialized--the implementation will log to our err PrintStream  
	 * with the following format: cause +": "+ message
	 * 
	 * @param cause -- Normally, the action or id of the call that generated the failure.
	 * @param message -- The failure message provided for that action or id.  If the message 
	 * is null the implementation will use FAIL_SUFFIX. 
	 * @see LogsInterface#fail(String, String)
	 * @see #FAIL_SUFFIX 
	 */
	public void processFailure(String action, String message){
		if(doProcessFailure){
			if(message == null) message = FAIL_SUFFIX;
			try{ log.fail(action, message);}
			catch(Exception x){
				err.println(action +CAUSE_SEP+ message);
			}
		}
	}

	/** 
	 * Set this value to false to bypass or ignore default success processing. 
	 * This allows the API caller to handle success in their own way.
	 * @see #processSuccess(String, String) 
	 */
	public boolean doProcessSuccess = true;
		
	/**
	 * Handle the reporting or logging of action or test success.
	 * <p>
	 * This is enabled by default.  A truer jUnit experience can be achieved by setting 
	 * doHandleSuccess = false.
	 * <p>
	 * This implementation uses the LogsInterface that should be provided at or immediately 
	 * following the creation of the Class instance.  Subclasses may wish to use true 
	 * jUnit reporting or other mechanisms.
	 * <p>
	 * If the LogsInterface call throws an Exception for any reason--including a NullPointerException 
	 * because it was never provided or initialized--the implementation will log to our out PrintStream 
	 * with the following format: cause +": "+ message
	 * 
	 * @param cause -- Normally, the action or id of the call that generated the success.
	 * @param message -- The success message provided for that action or id, if any. This can be 
	 * null.  If the message is null this implementation will use PASS_SUFFIX.
	 * @see #doHandleSuccess 
	 * @see LogsInterface#pass(String, String)
	 * @see #PASS_SUFFIX
	 */
	public void processSuccess(String action, String message){
		if(doProcessSuccess){
			if(message == null) message = PASS_SUFFIX;			
			try{ log.pass(action, message);}
			catch(Exception x){
				out.println(action +CAUSE_SEP+ message);
			}
		}
	}
	
	
	/**
	 * Initial default implementation performs:
	 * <p><pre>
	 * control.shutdown();
	 * This will stop the {@link SoloRemoteControlRunner} on the computer side.
	 * </pre>  
	 * 
	 * <b>Note:</b> If you want to stop the remote service on the device side, <br>
	 * you should call {@link #shutdownRemote()}, and you MUST call it before <br>
	 * calling this {@link #shutdown()} method.<br>
	 * 
	 * @see SoloRemoteControl#shutdown()
	 * @see #shutdownRemote()
	 */
	public void shutdown(){
		control.shutdown();
	}

	/**
	 * Initial default implementation performs:
	 * <p><pre>
	 * control.performRemoteShutdown(int,int,int);
	 * </pre>  
	 * 
	 * <b>Note:</b> This method will stop the remote service on the device side, <br>
	 * you MUST call it before calling {@link #shutdown()} method.<br>
	 * 
	 * @see SoloRemoteControl#performRemoteShutdown(int, int, int)
	 * @see #shutdown()
	 */
	public boolean shutdownRemote(){
		String debugPrefix = TAG+".shutdownRemote() ";
		boolean remoteShutdown = false;
		try {
			control.performRemoteShutdown(default_ready_stimeout, default_running_stimeout, default_shutdown_stimeout);
			remoteShutdown = true;
		} catch (Exception e) {
			debug(debugPrefix+" During shutdown remote service, met Exception="+e.getMessage());
		}
		
		return remoteShutdown;
	}
	
	protected Properties _props = new Properties();

	/**
	 * Prepare a dispatchProps object targeting a remote "instrument" command instead of a remote "solo" command.
	 * @param command
	 * @return Properties ready to be populated with command-specific parameters.
	 */
	protected Properties prepInstrumentDispatch(String command){
		try{_props.clear();}catch(NullPointerException x){_props = new Properties();}
		_props.setProperty(Message.KEY_COMMAND, command);
		_props.setProperty(Message.KEY_TARGET, Message.target_instrument);
		return _props;
	}
		
	/**
	 * Prepare a dispatchProps object targeting a remote "solo" command instead of a remote "instrument" command.
	 * @param command
	 * @return Properties ready to be populated with command-specific parameters.
	 */
	protected Properties prepSoloDispatch(String command){
		try{_props.clear();}catch(NullPointerException x){_props = new Properties();}
		_props.setProperty(Message.KEY_COMMAND, command);
		_props.setProperty(Message.KEY_TARGET, Message.target_solo);
		return _props;
	}		
}
