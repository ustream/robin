/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.client;

import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.safs.android.messenger.MessageUtil;
import org.safs.android.messenger.MultipleParcelsHandler;
import org.safs.android.messenger.client.CommandListener;
import org.safs.android.messenger.client.MessageResult;
import org.safs.android.messenger.client.MessengerRunner;
import org.safs.sockets.DebugListener;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.test.InstrumentationTestRunner;
import android.util.Log;

import com.jayway.android.robotium.remotecontrol.client.processor.ProcessorInterface;

/**
 * Primary InstrumentationTestRunner used for binding and un-binding TCP Messenger Service.<br>
 * It contains a MessengerRunner, which is used to handle the messages from 'TCP Messenger'.<br>
 * When the service is connected, MessengerRunner will be started in a separated thread.<br>
 * <p>
 * This main thread will wait there until the thread MessengerRunner's termination.<br>
 * This class implements the interface CommandListener, which describes the actions to do when <br>
 * receiving a message from 'TCP Messenger'. The subclass of this class should give a concrete<br>
 * implementation for methods described in CommandListener.<br>
 * 
 * @see org.safs.android.messenger.MessengerService
 * @author Carl Nagle, SAS Institute, Inc.
 * @since   FEB 04, 2012	(CANAGL)	Initial version
 *   <br>	APR 25, 2013	(LeiWang)	Handle message of big size. 
 */
public abstract class AbstractTestRunner extends InstrumentationTestRunner implements CommandListener, DebugListener
{
	public static final String TAG = "AbstractTestRunner";
	
    public static final String resource_service_attached ="SAFS TCP Messenger Attached";
    public static final String resource_service_disconnect ="SAFS TCP Messenger Disconnect";
    public static final String resource_service_release ="SAFS TCP Messenger Release";
    public static final String resource_bind_service ="SAFS TCP Messenger Binding";

	protected MessengerRunner messageRunner = null;
	
	Object lock = new Object(); 
	boolean runnerStopped = false;
	/**
	 * Do we need multiple processors for one target?
	 * If not, we may just define the cache as HashMap<String, ProcessorInterface>
	 */
	HashMap<String, Vector<ProcessorInterface>> processorsMap = new HashMap<String, Vector<ProcessorInterface>>();
	
	/** 
	 * true if we are successfully bound to the TCP Messenger Service. 
	 * false if we have disconnected ourselves from the service.
	 * <p>
	 * CAUTION: The Instrumentation thread monitors this to determine if the thread should exit. */
	boolean mIsBound = false;
    
	/** Exposes the messageRunner.sendServiceResult(Properties) method. */
	public boolean sendServiceResult(Properties props){
		if(messageRunner != null) return messageRunner.sendServiceResult(props);
		return false;
	}
	
	/****************************************************************
	 * In-line ServiceConnection object for local notification of connecting and disconnecting 
	 * of the TCP Messenger Service. 
	 ****************************************************************/
	private ServiceConnection mConnection = new ServiceConnection(){

		/**
		 * Upon receiving a bind to the TCP Messenger Service we register our own Messenger 
		 * with the service for two-way communication across processes.
		 * @see Messenger#Messenger(IBinder)
		 * @see Message#obtain(Handler, int)
		 * @see AbstractTestRunner#MSG_REGISTER_ENGINE
		 */
		public void onServiceConnected(ComponentName className, IBinder service){
			Messenger mService = new Messenger(service);
			debug(resource_service_attached +":"+className);
			if(messageRunner==null){
				messageRunner = new MessengerRunner(mService, AbstractTestRunner.this);
				messageRunner.start();
				messageRunner.sendRegisterEngine();
			}else{
				debug("Failed!!! "+resource_service_attached +":"+className);
			}
		}
		/**
		 * Destroy our reference to the TCP Messenger Service once disconnected.
		 */
		public void onServiceDisconnected(ComponentName className){
    		if(messageRunner != null){
    			messageRunner.sendUnRegisterEngine();
    			debug(resource_service_disconnect+":"+className);
    			messageRunner = null;
    		}else{
    			debug("Failed!!! "+resource_service_disconnect +":"+className);
    		}
		}
	};

	private boolean debugEnabled = true;
	public void setDebugEnabled(boolean enable){ debugEnabled = enable; }
	public boolean isDebugEnabled(){ return debugEnabled;}
	public void debug(String message){
		if (debugEnabled) Log.d(getListenerName(), message);		
	}
	
	/**
	 * Receive debug logging requests from the Messenger Service
	 */
	public void onReceiveDebug(String message){ debug(message); }
	

	/** 
	 * Called when the Instrumentation class is first created. 
	 * Here we launch and bind to the TCP Messenger Service and then attempt to deduce the 
	 * target Application we are going to be testing.
	 * <p>
	 * We do this through the following sequence 
	 * of execution:
	 * <ol>
	 * <li>{@link #doBindService()}
	 * <li>{@link #beforeStart()}
	 * <li>{@link #start()}
	 * <li>{@link #afterStart()}
	 * </ol>
	 */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
		if(!doBindService()){
			debug("doBindService Fail! Can't continue.");
			return;
		}
		if(beforeStart()){
			start();
			afterStart();
		}
    }

    /**
     * Called as part of the {@link #onCreate(Bundle)} initialization after {@link #doBindService()}
     * immediately before {@link #start()}.
     * <p>
     * Allows subclasses to do any additional setup prior to starting the Instrumentation Thread as 
     * part of the onCreate call.
     * @return true to continue normal operation. false to abort and NOT call start().
     */
    public abstract boolean beforeStart();
    
    /**
     * Called as part of the {@link #onCreate(Bundle)} initialization after {@link #start()}.
     * <p>
     * Allows subclasses to do any additional setup after the Instrumentation Thread has been 
     * started as part of the onCreate call.
     */
    public abstract void afterStart();
    
    /**
     * Called automatically from start().
     * @see #start()
     */
    public void onStart(){
    	while(mIsBound && !runnerStopped){
    		synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					debug("Receive InterruptedException during wait()");
				}
			}
    	}
		finishInstrumentation();
    }

    public void finishInstrumentation(){
		//This will terminate the Instrumentation of application
		try{finish(0, new Bundle());}catch(Throwable x){}
    }
    
	public void messengerRunnerStopped(){
		runnerStopped = true;
		synchronized (lock) {
			lock.notifyAll();
		}
	}
	
    /**
     * Attempt to force the launch and persistent binding to the separate TCP Messenger Service.
     * A successful binding will set mIsBound true which will cause our instrumentation to loop 
     * until this is reset to false.
     */
    protected boolean doBindService(){
    	try{
    		mIsBound = getContext().bindService(new Intent(MessageUtil.SERVICE_CONNECT_INTENT), mConnection, Context.BIND_AUTO_CREATE);
    		if(mIsBound){
    			debug(resource_bind_service+":MessengerService");
    			return true;
    		}else{
    			debug(resource_bind_service+":UNSUCCESSFUL");
    			return false;
    		}
    	}catch(Exception x){
			debug("doBindService Exception:"+ x.getClass().getSimpleName()+" "+ x.getMessage());
			return false;
    	}
    }

    /**
     * Unregister and then unbind with the separate TCP Messenger Service.
     * This will set reset our mIsBound boolean to false which will allow our instrumentation 
     * thread to shutdown. 
     */
    protected boolean doUnbindService(){
    	if(mIsBound){
    		try{
    			getContext().unbindService(mConnection);
    			debug("doUnbindService issuing stopService(shutdown)...");
    			getContext().stopService(new Intent(MessageUtil.SERVICE_SHUTDOWN_INTENT));
    		}catch(Exception e){
    			debug("doUnbindService Exception:"+ e.getClass().getSimpleName()+" "+ e.getMessage());
    			return false;
    		}
    		mIsBound = false;
    		debug(resource_service_release+":MessengerService");
    		return true;
    	}else{
    		debug(resource_service_release+":MessengerService warning, this serviced is not bound.");
    		return true;
    	}
    }

	public String getListenerName() {
		return TAG;
	}

	public MessageResult handleEngineShutdown(){
		MessageResult result = null;		
		if(doUnbindService()){
			result = MessageResult.getSuccessTestResult(result);
			result.setStatusinfo("Success: "+resource_service_release+":MessengerService");
		}else{
			result = MessageResult.getFailTestResult(result);
			result.setStatusinfo("Fail: "+resource_service_release+":MessengerService");
		}		
		return result;
	}
	
	/**
	 * ===========================================================================================
	 * Following codes are used to add to, get/remove processor from a cache 
	 * ===========================================================================================
	 */
	
	/**
	 * According to the target, put the processor to a cache
	 * 
	 * @param target		The key with which the processors are stored in cache
	 * @param processor		The processor to be stored in cache
	 */
	public void addProcessor(String target, ProcessorInterface processor){
		Vector<ProcessorInterface> processors = null;
		
		if(processorsMap.containsKey(target)){
			processors = processorsMap.get(target);
			processors.add(processor);
		}else{
			processors = new Vector<ProcessorInterface>();
			processors.add(processor);
			processorsMap.put(target, processors);
		}
	}
	
	/**
	 * According to the target, get the processors from a cache
	 * 
	 * @param target		The key with which the processors are stored in cache
	 * @return a Vector containing 0 or more ProcessorInterface objects.
	 */
	public Vector<ProcessorInterface> getProcessors(String target){
		Vector<ProcessorInterface> processors = null;
		
		if(processorsMap.containsKey(target)){
			processors = processorsMap.get(target);
		}else{
			processors = new Vector<ProcessorInterface>();
		}
		
		return processors;
	}
	
	/**
	 * Be careful when you call this method, which will remove all the processors from cache
	 */
	public void removeProcessors(){
		processorsMap.clear();
	}
	
	/**
	 * Be careful when you call this method, which will remove all the processors
	 * related to 'target' from cache
	 * 
	 * @param target		The key with which the processors are stored in cache
	 */
	public void removeProcessors(String target){
		Vector<ProcessorInterface> processors = null;
		
		if(processorsMap.containsKey(target)){
			processors = processorsMap.get(target);
			processors.clear();
		}else{
			debug("The processors cache doesn't contain processors related to '"+target+"'");
		}
	}

	/**
	 * Remove the processor from the cache, the processor should belong to 'target'
	 * 
	 * @param target		The key with which the processors are stored in cache
	 * @param processor		The processor to be removed from cache
	 */
	public void removeProcessor(String target, ProcessorInterface processor){
		Vector<ProcessorInterface> processors = null;
		
		if(processorsMap.containsKey(target)){
			processors = processorsMap.get(target);
			if(processors.remove(processor)){
				debug("Processor '"+processor.getClass().getSimpleName()+"' has been removed.");
			}
		}else{
			debug("The processors cache doesn't contain processors related to '"+target+"'");
		}
	}
	
	/**
	 * ===========================================================================================
	 * Following are the call-back methods inherited from CommandListener
	 * ===========================================================================================
	 */
	
	/**
	 * @param props		A Properties object, contains the input parameters including command, target etc.<br>
	 *                  To get the command, target, use the key defined in SoloMessage, such as KEY_COMMAND,KEY_TARGET<br>
	 *                  
	 *                  It also serves as the output for result:<br>
	 *                  To set the result, use the key defined in SoloMessage, such as KEY_REMOTERESULTCODE<br>
	 *                  KEY_REMOTERESULTINFO, KEY_ISREMOTERESULT etc.<br>
	 *                  
	 * @see org.safs.sockets.Message
	 * @see com.jayway.android.robotium.remotecontrol.solo.Message
	 * @see com.jayway.android.robotium.remotecontrol.client.SoloMessage
	 */
	public void handleDispatchProps(Properties props) {
		String debugmsg = getClass().getName()+"handleDispatchProps(): ";
		String command = null;
		String target = null;
		Vector<ProcessorInterface> processors = null;
		ProcessorInterface processor = null;
		
		if(props==null){
			debug("Fatal Error: the properties is null");
			return;
		}
		//Set value for key "isremoteresult"="false" until we know we have executed the command.
		props.setProperty(SoloMessage.KEY_ISREMOTERESULT, Boolean.toString(false));

		//get the target's value
		target = props.getProperty(SoloMessage.KEY_TARGET);
		if(target!=null){
			debug(debugmsg+" target is '"+target+"'");
			processors = getProcessors(target);
		}
		
		//the argument props will take back the result, we don't need the MessageResult
		command = props.getProperty(SoloMessage.KEY_COMMAND);
		if(command==null){
			props.setProperty(SoloMessage.KEY_REMOTERESULTCODE, SoloMessage.STATUS_REMOTERESULT_FAIL_STRING);
			props.setProperty(SoloMessage.KEY_REMOTERESULTINFO, SoloMessage.RESULT_INFO_COMMAND_ISNULL);
		}else{
			//Preset the "not executed" result in the properties.
			//Command will be handled in methods of SoloProcessor, 
			//when the command is properly handled--success or failure--the result will be replaced there.
			props.setProperty(SoloMessage.KEY_REMOTERESULTCODE, SoloMessage.STATUS_REMOTE_NOT_EXECUTED_STRING);
			props.setProperty(SoloMessage.KEY_REMOTERESULTINFO, command+SoloMessage.RESULT_INFO_COMMAND_UNKNOWN);
			boolean processed = false;
			// cycle through chained target processors only until one of them handles the command
			for(int i=0; i<processors.size()&& !processed;i++){
				processor = processors.get(i);
				processor.setRemoteCommand(command);
				processor.processProperties(props);
				try{processed = ! SoloMessage.STATUS_REMOTE_NOT_EXECUTED_STRING.equals(props.getProperty(SoloMessage.KEY_REMOTERESULTCODE));}catch(NullPointerException x){}
			}
		}
	}
}
