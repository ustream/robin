/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.solo;

import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import org.safs.android.auto.lib.AndroidTools;
import org.safs.android.auto.lib.DUtilities;
import org.safs.sockets.ConnectionListener;
import org.safs.sockets.DebugListener;
import org.safs.sockets.NamedListener;
import org.safs.sockets.RemoteException;
import org.safs.sockets.ShutdownInvocationException;
import org.safs.sockets.SocketProtocol;
import org.safs.sockets.SocketProtocolListener;
import org.safs.sockets.android.DroidSocketProtocol;

/** 
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * 
 * OCT 11, 2012	(SBJLWA) Remove field startEmulatorPortForwarding, because we can use portForwarding instead.
 * FEB 06, 2013 (CANAGL) Fixed performRemoteShutdown to properly use secsWaitShutdown.
 */
public class SoloRemoteControl implements SocketProtocolListener, DebugListener{
		
	public static final String listenername = "SoloRemoteControl";
	
	/************************************************************************** 
	 * Default: C:\Program Files\Android\android-sdk\<br>
	 * Set to the root directory where the Droid Development SDK is located. 	 
	 **/
	public static String ROOT_DROID_SDK_DIR = "C:\\Program Files\\Android\\android-sdk\\";
	
	/************************************************************************** 
	 * Default: C:\Program Files\Android\android-sdk\tools<br>
	 * Set to the directory where the Droid Development SDK Tools are located. 	 
	 **/
	public static String ROOT_DROID_SDK_TOOLS = ROOT_DROID_SDK_DIR +"tools";
	
	protected Vector listeners = new Vector();	
	protected static LogsInterface log = null;
	protected SoloRemoteControlRunner runner = null;
	protected Thread runnerThread = null;
	
	public int controllerPort = SocketProtocol.DEFAULT_CONTROLLER_PORT;
	
	public boolean portForwarding = true;
	
	public static AndroidTools sdk = null;
	
	public boolean enableProtocolDebug = true;
	public boolean enableRunnerDebug = true;
	
	/**
	 * No-arg constructor to instantiate and use all defaults.
	 * The default implementation does NOT have a LogsInterface object set until a 
	 * successful call to setLogsInterface is made.
	 * @see #setLogsInterface(LogsInterface)
	 */
	public SoloRemoteControl(){
		super();
	}

	/**
	 * Set our static sdk tool to the one appropriate for the OS (Windows or Unix).
	 * The routine does nothing if the appropriate sdk instance is already set.
	 * 
	 * @see DUtilities#getAndroidTools()
	 */
	public void initAndroidTools(){
		if (sdk == null){
			debug("Setting Android Tools SDK Dir to "+ ROOT_DROID_SDK_DIR);
			sdk = DUtilities.getAndroidTools(ROOT_DROID_SDK_DIR);
		}
	}
	
	/**
	 * Set the LogsInterface to be used by the class instance.
	 * @param ilog
	 */
	public void setLogsInterface(LogsInterface ilog){
		log = ilog;
	}
	
	/**
	 * Add a listener to this Runner. Could be a DebugListener, ConnectionListener, or a 
	 * SoloRemoteControlListener--any subclass of NamedListener.
	 * @param listen
	 * @see NamedListener
	 * @see ConnectionListener
	 * @see SoloRemoteControlListener
	 */
	public void addListener(NamedListener listen){
		if(! listeners.contains(listen)) listeners.add(listen);
	}
	
	/**
	 * Remove a previously added listener from this Runner.
	 * @param listen
	 */
	public void removeListener(NamedListener listen){
		if(listeners.contains(listen)) listeners.remove(listen);
	}
	
	/**
	 * Invoked from start().
	 * Creates the default instance of our Runner.  
	 * @return true to allow normal execution to proceed.
	 * Returning false should cause an abort of the test startup procedure.
	 * @see #start()
	 */
	protected boolean createProtocolRunner(){
		runner = new SoloRemoteControlRunner(this);
		return true;
	}
	
	/**
	 * Invoked from start().
	 * Creates the default instance of the runnerThread providing the current Runner 
	 * as its Runnable argument.  The runnerThread is then immediately started. 
	 * @return true to allow normal execution to proceed.
	 * Returning false should cause an abort of the test startup procedure.
	 * @see #start()
	 */
	protected boolean startProtocolRunner() {
	    try{
	    	runnerThread = new Thread(runner);
		    runnerThread.start();
		    return true;
	    }catch(Exception x){ return false; }
	}

	/**
	 * Creates and Starts the asynchronous RemoteControl runner.
	 * @throws IllegalThreadStateException if we were unable to create and run the protocol runner.
	 */
	public void start() throws IllegalThreadStateException{
		if(!createProtocolRunner()) throw new IllegalThreadStateException("Failed to create the RemoteControlRunner!");
		//before we start the protocol runner, we can modify some properties of embedded #DroidSocketProtocol
		modifyEmbeddedProtocol();
		if(!startProtocolRunner()) throw new IllegalThreadStateException("Failed to start the RemoteControlRunner!");
	}
	
	/**
	 * In this method, we can modify some properties of the embedded protocol {@link DroidSocketProtocol}.<br>
	 * <b>Note:</b> this method MUST be called after {@link #createProtocolRunner()}
	 *              and before {@link #startProtocolRunner()}. Just like in {@link #start()}.<br>
	 * 
	 * For example, we can modify the controllerPort as {@link DroidSocketProtocol#setControllerPort(int)}<br>
	 * or modify portForwarding as {@link DroidSocketProtocol#setPortForwarding(boolean)}<br>
	 * 
	 * If the 'port forwarding' is set to true, we MUST call {@link DroidSocketProtocol#adjustControllerPort()}
	 * to choose an available port for 'controller'.<br>
	 * 
	 * @see #start()
	 * @see DroidSocketProtocol#setControllerPort(int)
	 * @see DroidSocketProtocol#setPortForwarding(boolean)
	 * @see DroidSocketProtocol#adjustControllerPort()
	 * 
	 */
	protected void modifyEmbeddedProtocol(){
		if(runner!=null && runner.droidprotocolserver!=null){
			runner.droidprotocolserver.setPortForwarding(portForwarding);
			runner.droidprotocolserver.adjustControllerPort();
		}else{
			debug("runner or runner.droidprotocolserver is null.");
		}
	}
	
	/**
	 * Set the 'controller port' where we will connect for messenger service.<br>
	 * The 'controller port' should be an available port, if it is not, it will be<br>
	 * modified to an available one in method {@link #modifyEmbeddedProtocol()}<br>
	 * 
	 * @param controllerPort	int,	an available port number
	 * @see #modifyEmbeddedProtocol()
	 */
	public void setControllerPort(int controllerPort){
		this.controllerPort = controllerPort;
	}
	
	/**
	 * Set if we will forward 'controller port' to remote messenger service's port.
	 * Must be set before {@link #start()} is called to have affect.
	 * @param portForwarding set true if the controller should attempt port forwarding 
	 * 		  before starting the runner thread.
	 */
	public void setPortForwarding(boolean portForwarding){
		this.portForwarding = portForwarding;
	}
	
	/**
	 * Command the RemoteControl infrastructure to shutdown.
	 * @see SoloRemoteControlRunner#shutdownThread()
	 */
	public void shutdown(){
		runner.shutdownThread();
	}
	
	boolean remoteException = false;
	boolean remoteMessage = false;
	String remoteMessageString = null;
	boolean remoteReady = false;	
	boolean remoteConnected = false;
	boolean remoteRunning = false;
	boolean remoteResult = false;
	int remoteResultCode = 0;
	String remoteResultInfo = null;
	Properties remoteResultProperties = null;
	boolean localShutdown = false;
	boolean remoteShutdown = false;
	int shutdownCause = -1;
	Object lock = new Object();

	/** 
	 * reset the results fields and properties prior to dispatching a request.
	 * remoteResult = false<br>
	 * remoteResultCode = -99<br>
	 * remoteResultInfo = null<br>
	 * remoteResultProperties = null<br>
	 * remoteComment = null<br>
	 * remoteDetail = null<br>
	 */
	public void resetResults(){
		remoteResult = false;
		remoteResultCode = Message.STATUS_REMOTERESULT_UNKNOWN;
		remoteResultInfo = null;
		remoteResultProperties = null;
	}
	
	/** 
	 * Used internally. Reset the ready field--normally immediately after having received it.
	 * remoteReady = false<br>
	 */
	protected void resetReady(){
		remoteReady = false;
	}
	
	/**
	 * Consolidates the remoteResult, remoteResultCode, and remoteResultInfo into any remoteResultProperties.  
	 * If remoteResultProperties is null, the routine will create the Properties object before the consolidation. 
	 * remoteResultInfo will only be written to the Properties if it is a non-null String value. 
	 * The routine will NOT overwrite any of these remoteResult properties if they already exist in the 
	 * remoteResultProperties object.
	 * @return remoteResultProperties
	 */
	protected Properties consolidateResults(){
		if(remoteResultProperties == null) remoteResultProperties = new Properties();
		if(! remoteResultProperties.containsKey(Message.KEY_ISREMOTERESULT)) 
			remoteResultProperties.setProperty(Message.KEY_ISREMOTERESULT, String.valueOf(remoteResult));
		if(!remoteResultProperties.containsKey(Message.KEY_REMOTERESULTCODE))
			remoteResultProperties.setProperty(Message.KEY_REMOTERESULTCODE, String.valueOf(remoteResultCode));
		if((!remoteResultProperties.containsKey(Message.KEY_REMOTERESULTINFO))&& 
				remoteResultInfo != null) 
			remoteResultProperties.setProperty(Message.KEY_REMOTERESULTINFO, remoteResultInfo);
		return remoteResultProperties;
	}
	
	public void resetRunningState(){
		synchronized(lock){
			resetResults();
			remoteException = false;
			remoteRunning = false;
			remoteMessage = false;
			remoteMessageString = null;
			remoteShutdown = false;
			localShutdown = false;
			shutdownCause = -1;
		}		
	}
	
	public void onReceiveConnection() {
		synchronized(lock){
			remoteConnected = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyConnectionListeners();
	}

	public void onReceiveReady() {
		synchronized(lock){
			remoteException = false;
			//remoteRunning = false;
			remoteReady = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyReadyListeners();
	}

	public void onReceiveRunning() {
		synchronized(lock){
			remoteException = false;
			//remoteReady = false;
			remoteRunning = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyRunningListeners();
	}

	/**
	 * Implemented here, but not currently used by this implementation.
	 * This implementation is using ResultProperties.
	 * @see #onReceiveResultProperties(Properties)
	 */
	public void onReceiveResult(int rc, String info) {
		synchronized(lock){
			remoteException = false;
			//remoteRunning = false;
			remoteResultCode = rc;
			remoteResultInfo = info;
			remoteResultProperties = null;
			remoteResult = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyResultsListeners(rc, info);
	}

	public void onReceiveResultProperties(Properties result) {
		synchronized(lock){
			remoteException = false;
			//remoteRunning = false;
			remoteResultCode = -1;
			remoteResultInfo = null;
			remoteResultProperties = result;
			remoteResult = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyResultPropsListeners(result);
	}

	public void onReceiveException(String message) {
		synchronized(lock){
			//remoteRunning = false;
			remoteMessageString = message;
			remoteException = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyExceptionListeners(message);
	}

	public void onReceiveMessage(String message) {
		synchronized(lock){
			remoteException = false;
			//remoteRunning = false;
			remoteMessageString = message;
			remoteMessage = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyMessageListeners(message);
	}

	public void onReceiveLocalShutdown(int shutdownCause) {
		synchronized(lock){
			remoteException = false;
			//remoteRunning = false;
			this.shutdownCause = shutdownCause;
			localShutdown = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyLocalShutdownListeners(shutdownCause);
	}

	public void onReceiveRemoteShutdown(int shutdownCause) {
		synchronized(lock){
			remoteException = false;
			remoteRunning = false;
			this.shutdownCause = shutdownCause;
			remoteShutdown = true;
			try{ lock.notifyAll();}catch(Throwable e){}
		}
		notifyRemoteShutdownListeners(shutdownCause);
	}

	public String getListenerName() {
		return listenername;
	}

	public void onReceiveDebug(String message) {
		debug(message);
	}
	
	public void debug(String message){
		boolean logged = false;
		for(int i=0;i< listeners.size(); i++){
			try{
				((DebugListener)listeners.get(i)).onReceiveDebug(message);
				logged = true;
			}catch(Exception x){}
		}
		if(!logged){
			try{
				log.debug(message);
			}
			catch(Exception x){
				System.out.println(message);
			}
		}
	}

	/** 
	 * Throw a ShutdownInvocationException only *IF* we have received a localShutdown 
	 * or remoteShutdown message.  Throw a RemoteException only *IF* we have received 
	 * a remoteException.  Otherwise, this routine returns doing nothing.
	 * @throws ShutdownInvocationException if localShutdown or remoteShutdown have been 
	 * received.
	 * @throws RemoteException if remoteException has been received.
	 */
	protected void checkExceptions() throws RemoteException, ShutdownInvocationException{
		if (localShutdown){
			throw new ShutdownInvocationException("Unexpected Local Shutdown has been initiated.", false, shutdownCause);
		}else if (remoteShutdown){
			throw new ShutdownInvocationException("Unexpected Remote Shutdown has been initiated.", true, shutdownCause);			
		}else if (remoteException){
			throw new RemoteException(remoteMessageString);		
		}
	}
	
	/**
	 * Wait for the remote client to get connected.
	 * This is typically called from an external Thread.
	 * @param sTimeout in seconds to wait for connection.  
	 * If timeout is < 1 then the there is no wait.
	 * 
	 * @throws TimeoutException if connection was not made in the timeout period.
	 * @throws RemoteException if we received an unexpected remote exception instead.  
	 * @throws ShutdownInvocationException if either a local or remote shutdown has been 
	 * initiated.
	 */
	public void waitForRemoteConnected(int sTimeout)throws TimeoutException, 
														   RemoteException,
														   ShutdownInvocationException{		
		if (sTimeout < 1) {
			// do nothing
		}else{
			long millis = sTimeout * 1000;
			long maxTicks = System.currentTimeMillis() + millis;
			synchronized(lock){
				while(!remoteConnected && !remoteException &&
					  !localShutdown   && !remoteShutdown  &&
					  System.currentTimeMillis() < maxTicks){
						try{ lock.wait(millis); }
						catch(InterruptedException e){}
				}
			}
		}
		checkExceptions();
		if(! remoteConnected) throw new TimeoutException("waitForRemoteConnected Timeout before Connect");
	}
	
	protected void notifyConnectionListeners(){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((ConnectionListener)listeners.get(i)).onReceiveConnection();
			}catch(Exception x){}
		}
	}
	
	/**
	 * Wait for the remote client to signal Ready for a new command.
	 * This is typically called from an external Thread.
	 * @param sTimeout in seconds to wait for ready.  If timeout is < 1 then there 
	 * is no wait.
	 * 
	 * @throws TimeoutException if Ready was not seen in the timeout period. 
	 * @throws RemoteException if we received an unexpected remote exception instead.  
	 * @throws ShutdownInvocationException if either a local or remote shutdown has been 
	 * initiated.
	 */
	public void waitForRemoteReady(int sTimeout)throws TimeoutException, 
													   RemoteException,
													   ShutdownInvocationException{
		if (sTimeout < 1) {
			// do nothing
		}else{
			long millis = sTimeout * 1000;
			long maxTicks = System.currentTimeMillis() + millis;
			synchronized(lock){
				while(!remoteReady && !remoteException &&
						  !localShutdown   && !remoteShutdown  &&
						  System.currentTimeMillis() < maxTicks){
						try{ lock.wait(millis); }
						catch(InterruptedException e){}
				}
			}
		}
		checkExceptions();
		if(! remoteReady) throw new TimeoutException("waitForRemoteReady Timeout before Ready");
	}
	
	protected void notifyReadyListeners(){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((SocketProtocolListener)listeners.get(i)).onReceiveReady();
			}catch(Exception x){}
		}
	}
	
	/**
	 * Default implementation performs the following:
	 * <p><pre>
	 * resetResults();
	 * waitForRemoteReady(secsWaitReady);
	 * runner.sendDispatchProps(props);
	 * waitForRemoteRunning(secsWaitRunning);
	 * return waitForRemoteResult(secsWaitResult);
	 *  </pre>
	 * @param props - the Dispatch Properties object containing all required command parameters for the remote client.
	 * @param secsWaitRead -- timeout in seconds to detect remoteReady.
	 * @param secsWaitRunning -- timeout in seconds to detect remoteRunning following the dispatch.
	 * @param secsWaitResults -- timeout in seconds to wait for results from the remote client.
	 * @return - the Result Properties returned by the remote client.
	 * @throws ShutdownInvocationException 
	 * @throws TimeoutException 
	 * @throws RemoteException 
	 * @throws IllegalThreadStateException -- if the attempt to send the properties failed for some unknown reason.
	 * @see SoloRemoteControlRunner#sendDispatchProps(Properties)
	 */
	public Properties performRemotePropsCommand(Properties props, int secsWaitReady, int secsWaitRunning, int secsWaitResult) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		resetResults();
		waitForRemoteReady(secsWaitReady);
		resetReady();
		if(runner.sendDispatchProps(props)) {
			waitForRemoteRunning(secsWaitRunning);
			return waitForRemoteResult(secsWaitResult);
		}
		throw new IllegalThreadStateException("Local ProtocolRunner failed to sendDispatchProps.");
	}
	
	/**
	 * Default implementation performs the following:
	 * <p><pre>
	 * resetResults();
	 * waitForRemoteReady(secsWaitReady);
	 * runner.sendDispatchFile(filepath);
	 * waitForRemoteRunning(secsWaitRunning);
	 * return waitForRemoteResult(secsWaitResult);
	 *  </pre>
	 * @param filepath - the filepath to the File to process.
	 * @param secsWaitRead -- timeout in seconds to detect remoteReady.
	 * @param secsWaitRunning -- timeout in seconds to detect remoteRunning following the dispatch.
	 * @param secsWaitResults -- timeout in seconds to wait for results from the remote client.
	 * @return - the Result Properties returned by the remote client.
	 * @throws ShutdownInvocationException 
	 * @throws TimeoutException 
	 * @throws RemoteException 
	 * @throws IllegalThreadStateException -- if the attempt to send failed for some unknown reason.
	 * @see SoloRemoteControlRunner#sendDispatchProps(Properties)
	 */
	public Properties performRemoteFileCommand(String filepath, int secsWaitReady, int secsWaitRunning, int secsWaitResult) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		resetResults();
		waitForRemoteReady(secsWaitReady);
		resetReady();
		if(runner.sendDispatchFile(filepath)) {
			waitForRemoteRunning(secsWaitRunning);
			return waitForRemoteResult(secsWaitResult);
		}
		throw new IllegalThreadStateException("Local ProtocolRunner failed to sendDispatchProps.");
	}
	
	/**
	 * Default implementation performs the following:
	 * <p><pre>
	 * resetResults();
	 * waitForRemoteReady(secsWaitReady);
	 * runner.sendMessage(message);
	 * waitForRemoteRunning(secsWaitRunning);
	 * </pre>
	 * <p>
	 * The routine does NOT waitForResults.  This allows arbitrary messaging not always expecting a response.
	 * @param message to send to remote client
	 * @param secsWaitRead -- timeout in seconds to detect remoteReady.
	 * @param secsWaitRunning -- timeout in seconds to detect remoteRunning following the dispatch.
	 * @param secsWaitResults -- timeout in seconds to wait for results from the remote client.
	 * @throws ShutdownInvocationException 
	 * @throws TimeoutException 
	 * @throws RemoteException 
	 * @throws IllegalThreadStateException -- if the attempt to send the message failed for some unknown reason.
	 * @see SoloRemoteControlRunner#sendDispatchProps(Properties)
	 */
	public void performRemoteMessageCommand(String message, int secsWaitReady, int secsWaitRunning) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		resetResults();
		waitForRemoteReady(secsWaitReady);
		resetReady();
		if(runner.sendMessage(message)) {
			waitForRemoteRunning(secsWaitRunning);
			return;
		}
		throw new IllegalThreadStateException("Local ProtocolRunner failed to sendDispatchProps.");
	}
	
	/**
	 * Default implementation performs the following:
	 * <p><pre>
	 * resetResults();
	 * waitForRemoteReady(secsWaitReady);
	 * runner.sendShutdown();
	 * waitForRemoteRunning(secsWaitRunning);
	 * </pre>
	 * <p>
	 * The routine does NOT waitForResults.  This allows arbitrary messaging not always expecting a response.
	 * @param message to send to remote client
	 * @param secsWaitRead -- timeout in seconds to detect remoteReady.
	 * @param secsWaitRunning -- timeout in seconds to detect remoteRunning following the dispatch.
	 * @param secsWaitShutdown -- timeout in seconds to wait to receive shutdown confirmation.
	 * @throws TimeoutException 
	 * @throws RemoteException 
	 * @throws IllegalThreadStateException -- if the attempt to send the message failed for some unknown reason.
	 * @see SoloRemoteControlRunner#sendDispatchProps(Properties)
	 */
	public void performRemoteShutdown(int secsWaitReady, int secsWaitRunning, int secsWaitShutdown) throws IllegalThreadStateException, RemoteException, TimeoutException, ShutdownInvocationException{
		resetResults();
		waitForRemoteReady(secsWaitReady);
		resetReady();
		if(runner.sendShutdown()) {
			try{
				waitForRemoteRunning(secsWaitRunning);
				waitForRemoteShutdown(secsWaitShutdown);
				return;
			}
			catch(TimeoutException x){
				debug("performRemoteShutdown ignoring TimeoutException: "+ x.getMessage());
			}
			catch(RemoteException x){
				debug("performRemoteShutdown ignoring RemoteException: "+ x.getMessage());
			}
			catch(ShutdownInvocationException x){
				debug("performRemoteShutdown ignoring ShutdownInvocationException: "+ x.getMessage());
			}
		}else{
			throw new IllegalThreadStateException("Local ProtocolRunner failed to sendShutdown request.");
		}
	}
	
	/**
	 * Wait for the remote client to signal Running a new command.
	 * This is typically called from an external Thread.
	 * @param sTimeout in seconds to wait for Running.  If timeout is < 1 then there 
	 * is no wait.
	 * 
	 * @throws TimeoutException if Running was not seen in the timeout period. 
	 * @throws RemoteException if we received an unexpected remote exception instead.  
	 * @throws ShutdownInvocationException if either a local or remote shutdown has been 
	 * initiated.
	 */
	public void waitForRemoteRunning(int sTimeout)throws TimeoutException, 
														 RemoteException,
														 ShutdownInvocationException{
		if (sTimeout < 1) {
			// do nothing
		}else{
			long millis = sTimeout * 1000;
			long maxTicks = System.currentTimeMillis() + millis;
			synchronized(lock){
				while(!remoteRunning && !remoteResult && !remoteException &&
						  !localShutdown   && !remoteShutdown  &&
						  System.currentTimeMillis() < maxTicks){
						try{ lock.wait(millis); }
						catch(InterruptedException e){}
				}
			}
		}
		checkExceptions();
		if(! remoteRunning && !remoteResult) throw new TimeoutException("waitForRemoteRunning Timeout before Running");
	}	

	protected void notifyRunningListeners(){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((SocketProtocolListener)listeners.get(i)).onReceiveRunning();
			}catch(Exception x){}
		}
	}
	
	
	/**
	 * Wait for the remote client to signal results are available and return those results.
	 * This is typically called from an external Thread immediately following a "dispatch" call.
	 * <p>
	 * When a valid remote result has been received it is first checked for the presence of a 
	 * special property {@value Message#KEY_CHANGETIMEOUT}.  If that key is present then the property  
	 * value is processed for a new timeout value and the routine waits for a new remote result using 
	 * the new timeout value.  If no new timeout is provided, or the format is non-numeric, the 
	 * routine will simply wait again using the original sTimeout value.
	 * <p>
	 * @param sTimeout in seconds to wait for Results.  If timeout is < 1 then there 
	 * is no wait.
	 * @return Properties -- the resultProps returned from the remote client or a new Properties 
	 * object containing the consolidated remote result information.
	 * @throws TimeoutException if Results was not seen in the timeout period. 
	 * @throws RemoteException if we received an unexpected remote exception instead.  
	 * @throws ShutdownInvocationException if either a local or remote shutdown has been 
	 * initiated.
	 * @see #consolidateResults()
	 */
	public Properties waitForRemoteResult(int sTimeout)throws TimeoutException, 
														 RemoteException,
														 ShutdownInvocationException{
		if (sTimeout < 1) {
			// do nothing
		}else{
			long millis = sTimeout * 1000;
			long maxTicks = System.currentTimeMillis() + millis;
			synchronized(lock){
				while(!remoteResult && !remoteException &&
						  !localShutdown   && !remoteShutdown  &&
						  System.currentTimeMillis() < maxTicks){
						try{ lock.wait(millis); }
						catch(InterruptedException e){}
				}
			}
		}
		checkExceptions();
		if(! remoteResult) throw new TimeoutException("waitForRemoteResult Timeout before Result");
		remoteResultProperties = consolidateResults();		
		if(remoteResultProperties.containsKey(Message.KEY_CHANGETIMEOUT)){
			try{
				int newtimeout = Integer.valueOf(remoteResultProperties.getProperty(Message.KEY_CHANGETIMEOUT));
				if (newtimeout > 0) {
					debug("waitForRemoteResult detecting device-side timeout extension of "+ newtimeout);
					resetResults();
					return waitForRemoteResult(newtimeout);
				}else{
					debug("waitForRemoteResult ignoring invalid device-side timeout extension of "+ remoteResultProperties.getProperty(Message.KEY_CHANGETIMEOUT));
				}
			}catch(Exception x){
				debug("waitForRemoteResult ignoring "+ x.getClass().getSimpleName()+" and relooping...");
				resetResults();
				return waitForRemoteResult(sTimeout);
			}
		}
		return remoteResultProperties;
	}	

	protected void notifyResultsListeners(int rc, String info){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((SocketProtocolListener)listeners.get(i)).onReceiveResult(rc, info);
			}catch(Exception x){}
		}
	}
	
	protected void notifyResultPropsListeners(Properties props){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((SocketProtocolListener)listeners.get(i)).onReceiveResultProperties(props);
			}catch(Exception x){}
		}
	}
	
	
	
	/**
	 * Wait for the remote client to signal its shutdown.
	 * This is typically called from an external Thread.
	 * @param sTimeout in seconds to wait for shutdown.  If timeout is < 1 then there 
	 * is no wait.
	 * 
	 * @throws TimeoutException if shutdown was not seen in the timeout period. 
	 * @throws RemoteException if we received an unexpected remote exception instead.  
	 */
	public void waitForRemoteShutdown(int sTimeout)throws TimeoutException, 
														  RemoteException{
		if (sTimeout < 1) {
			// do nothing
		}else{
			long millis = sTimeout * 1000;
			long maxTicks = System.currentTimeMillis() + millis;
			synchronized(lock){
				while(!remoteShutdown && !remoteException && !localShutdown && 
					  System.currentTimeMillis() < maxTicks){
						try{ lock.wait(millis); }
						catch(InterruptedException e){}
				}
			}
		}
		if(remoteException) throw new RemoteException(remoteMessageString);
		if(! (remoteShutdown || localShutdown)) throw new TimeoutException("waitForRemoteShutdown Timeout before Shutdown");
	}	
	
	protected void notifyRemoteShutdownListeners(int cause){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((SocketProtocolListener)listeners.get(i)).onReceiveRemoteShutdown(cause);
			}catch(Exception x){}
		}
	}
	
	protected void notifyLocalShutdownListeners(int cause){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((SocketProtocolListener)listeners.get(i)).onReceiveLocalShutdown(cause);
			}catch(Exception x){}
		}
	}

	protected void notifyExceptionListeners(String message){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((SocketProtocolListener)listeners.get(i)).onReceiveException(message);
			}catch(Exception x){}
		}
	}

	protected void notifyMessageListeners(String message){
		for(int i=0;i< this.listeners.size(); i++){
			try{
				((SocketProtocolListener)listeners.get(i)).onReceiveMessage(message);
			}catch(Exception x){}
		}
	}
}
