/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger.client;

import java.io.CharArrayWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.safs.android.messenger.MessageUtil;
import org.safs.sockets.DebugListener;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * It is used to handle the messages from 'TCP Messenger' in a separated Thread.
 * 
 * @see com.jayway.android.robotium.remotecontrol.client.AbstractTestRunner
 * @see com.jayway.android.robotium.remotecontrol.client.RobotiumTestRunner
 * @author Carl Nagle, SAS Institute, Inc.
 * @since   FEB 04, 2012	(CANAGL)	Initial version
 *   <br>	APR 25, 2013	(LeiWang)	Handle message of big size.
 */
public class MessengerRunner implements MessengerListener, Runnable{

	public static final String listenerName = "MessengerRunner";
	
	boolean messageHandled = false;
	Properties trd_props = null;
	String trd_message = null;
	int iNotification = -99;
	
	/**
	 * messageHandler is used to handle the received message from 'TCP Messenger'
	 */
	MessengerHandler messageHandler = new MessengerHandler(this);
	
	/**
	 * mMessenger is used to receive message from message-service server
	 */
	final Messenger mMessenger = new Messenger(messageHandler);
	
	/**
	 * mService is the messenger to be used to send out message to 'TCP Messenger'
	 */
	Messenger mService = null;
	
	CommandListener commandListener = null;
	DebugListener debugListener = null;
	
	private boolean keeprunning = true;
	
	public MessengerRunner(Messenger mService){
		this.mService = mService;
	}
	
	public MessengerRunner(Messenger mService, CommandListener commandListener){
		this(mService);
		this.commandListener = commandListener;
		if(commandListener instanceof DebugListener) debugListener = (DebugListener) commandListener;
	}

	public String getListenerName(){
		return listenerName;
	}

	public Messenger getmService() {
		return mService;
	}
	public void setmService(Messenger mService) {
		this.mService = mService;
	}
	
	public CommandListener getCommandListener() {
		return commandListener;
	}
	public void setCommandListener(CommandListener commandListener){
		this.commandListener = commandListener;
	}
	
	public DebugListener getDebugListener() {
		return debugListener;
	}
	public void setDebugListener(DebugListener debugListener){
		this.debugListener = debugListener;
	}
	
	public void debug(String message){
		if(debugListener!=null){
			debugListener.onReceiveDebug(message);
		}else{
			Log.d(listenerName, message);
		}
	}
	
	public void onReceiveDebug(String message){
		debug(message);
	}
	
	/** MessengerHandler preparing information for Thread switching. */
	public void prepareNotification(int what){ iNotification = what;	}
	
	public void onRemoteDispatchProps(Properties props){
		debug("Listener received remoteDispatchProps...");
		trd_message = null;
		trd_props = props;
		messageHandled = false;
		synchronized(this){ this.notifyAll(); }
	}
	
	public void onRemoteDispatchFile(String message){
		debug("Listener received remoteDispatchFile...");
		trd_message = null;
		trd_props = new Properties();
		try{
			trd_props.load(new FileReader(message));
			messageHandled = false;
			debug("Properties loaded from File!");
			synchronized(this){ this.notifyAll(); }
		}catch(IOException x){
			debug("onRemoteDispatchFile "+ x.getClass().getSimpleName()+", "+x.getMessage());
		}
	}
	
	public void onRemoteMessage(String message){
		debug("Listener received remoteMessage...");
		trd_props = null;
		trd_message = message;
		messageHandled = false;
		synchronized(this){ this.notifyAll(); }
	}
	
	public void onRemoteConnected(){
		debug("Listener received remoteConnected...");
		trd_props = null;
		trd_message = null;
		messageHandled = false;
		synchronized(this){ this.notifyAll(); }
	}
	
	public void onRemoteDisconnected(){
		debug("Listener received remoteDisconnected...");
		trd_props = null;
		trd_message = null;
		messageHandled = false;
		synchronized(this){ this.notifyAll(); }
	}
	
	/** Notification that the Remote Controller has shutdown and is no longer available. */
	public void onRemoteShutdown(){
		debug("Listener received remoteShutdown notice...");
		trd_props = null;
		trd_message = null;
		messageHandled = false;
		synchronized(this){ this.notifyAll(); }
	}
	
	/** Notification that the Messenger Service has shutdown and is no longer available. */
	public void onServiceShutdown(){
		debug("Listener received serviceShutdown...");
		trd_props = null;
		trd_message = null;
		messageHandled = false;
		synchronized(this){ this.notifyAll(); }
	}

	/** Remote request/command to tell the engine to perform a normal shutdown. */
	public void onRemoteEngineShutdown(){
		debug("Listener received remoteShutdown command...");
		trd_props = null;
		trd_message = null;
		messageHandled = false;
		synchronized(this){ this.notifyAll(); }
	}

	/**
    * Primary looping test thread remains active for as long as we are bound to a TCP Messenger Service.
    * Synchronizes with the Message handler waiting for new valid instructions then routes test 
    * actions according to the content of the data received via the Messenger.
    */
	public void run() {
		MessageResult result = null;
		while(keeprunning){
    		synchronized(this){
		    	try{ this.wait(); }catch(InterruptedException x){}
    		}
	    	if(!messageHandled){
	    		messageHandled = true;
				switch (iNotification){

					case MessageUtil.ID_ENGINE_DISPATCHPROPS:
						sendRunning();
						debug("Handler Received DispatchProps!");
						
						//handle the request						
						commandListener.handleDispatchProps(trd_props);
						
						sendServiceResult(trd_props);
						sendReady();
						break;
	
					case MessageUtil.ID_ENGINE_DISPATCHFILE:
						sendRunning();
						debug("Handler Received DispatchFile!");
						//handle the request
						commandListener.handleDispatchProps(trd_props);
						
						sendServiceResult(trd_props);
						sendReady();
						break;
	
					case MessageUtil.ID_ENGINE_SHUTDOWN:
						sendRunning();
						debug("Handler Received Shutdown Command!");	
						//If the shutdown command comes, we will stop running of this thread. keeprunning=false;
						this.stop();
						//Do we need to send the shutdown command back???
						sendShutdown();
						//Invoke the callback of commandListener, let it know the messengerRunner has stopped.
						result = commandListener.handleEngineShutdown();
						if(result != null){
							debug(result.getStatusinfo());
							sendServiceResult(result);
						}
						break;
	
					case MessageUtil.ID_ENGINE_MESSAGE:
						sendRunning();
						
						result = commandListener.handleMessage(trd_message);
						if(result != null){
							debug(result.getStatusinfo());
							sendServiceResult(result);
						}
						sendReady();
						break;
	
					case MessageUtil.ID_SERVER_CONNECTED:
						debug("Handler received CONNECTED message: "+ trd_message);
						result = commandListener.handleServerConnected();
						if(result != null){
							debug(result.getStatusinfo());
							sendServiceResult(result);
						}
						sendReady();
						break;
				}
	    	}
    	}
		commandListener.messengerRunnerStopped();
	}
	
	public boolean start(){
		if(mService==null){
			debug("The mService is null, please initialize it with setMService()");
			return false;
		}
		if(commandListener==null){
			debug("The commandListener is null, please initialize it with setCommandListener()");
			return false;
		}
		Thread thread = new Thread(this);
		thread.start();
		return true;
	}
	public void stop(){
		this.keeprunning = false;
	}
	
    /*
     * Pseudo SocketProtocolListener Interface follows
     * These are the methods used by the device-side engine to send results or other data to the 
     * TCP Messenger service which ultimately sends them over Sockets to the remote test controller.
     */
    
    /** 
     * Note: NEVER call debug() in this method or in the method it will call!!! It causes StackOverFlow.
     * Create and send any String message through the service to the remote test controller.
     * Most of the other methods of this pseudo-interface call this method with appropriate parameters.
     * @param what, the message int flagging what kind of message we are sending.
     * @param message, the actual message--which can be null.
     * @return true on successfully sent */
    boolean sendServiceMessage(int what, String message){
		Message msg;
		if(message != null){
			msg = Message.obtain(null, what, MessageUtil.setParcelableMessage(message));
		}else{
			msg = Message.obtain(null, what);
		}
		msg.replyTo = mMessenger;
		try{
			Log.d(listenerName, "Engine sending message: "+ message);
			mService.send(msg);
			return true;
		}catch(RemoteException x){
			Log.d(listenerName, "Failed to send to MessengerService due to "+ x.getClass().getSimpleName()+", "+ x.getMessage());
		}
    	return false;
		
    	//TODO Will string message exceeds the buffer size??? send it as multiple parcels???
    	//If this is needed, un-comment following codes, but remember to change use of debug()
    	//to Log.d()
//		Message msg = Message.obtain(null, what);
//		msg.replyTo = mMessenger;
//    	Log.d(listenerName, "Engine sending message: "+ message);
//		
//		return multipleParcelsHandler.sendMessageAsMultipleParcels(mService, msg, message);
    }

    public boolean sendServiceResult(MessageResult result){
   	    return sendServiceResult(result.getStatuscode(), result.getStatusinfo());
    }
    
    /**
     * Create and send a MSG_ENGINE_RESULT message with the int statuscode and String statusinfo 
     * result from processing a previous command/dispatch.  This is largely used when we cannot send 
     * result Properties instead.
     * @return true on successfully sent
     * @see #sendServiceResult(Properties)
     */
    public boolean sendServiceResult(int statuscode, String statusinfo){
		Message msg = Message.obtain(null, MessageUtil.ID_ENGINE_RESULT);
		msg.replyTo = mMessenger;
		msg.arg1 = statuscode;
		debug("Engine sending simple result: "+ statuscode +", "+ statusinfo);
		
		try {
			if(messageHandler!=null)
				return messageHandler.sendMessageAsMultipleParcels(mService, msg, statusinfo);
			else{
				if(statusinfo!=null) msg.obj = MessageUtil.setParcelableMessage(statusinfo);
				mService.send(msg);
				return true;
			}
		} catch (RemoteException x) {
			debug(listenerName + ": Failed to send to MessengerService due to " + org.safs.sockets.Message.getStackTrace(x));
		}
		return false;
    }
    
    public boolean sendServiceParcelAcknowledge(String messageID, int index){
		Message msg = Message.obtain(null, MessageUtil.ID_PARCEL_ACKNOWLEDGMENT);
		msg.replyTo = mMessenger;
		debug("Engine sending parcel acknowledge: "+ messageID +", "+ index);
		msg.arg1 = index;
		
		try {
			if(messageID!=null) msg.obj = MessageUtil.setParcelableMessage(messageID);
			mService.send(msg);
			return true;
		} catch (RemoteException x) {
			debug(listenerName + ": Failed to send to MessengerService due to " + org.safs.sockets.Message.getStackTrace(x));
		}
		return false;
    }

    /**
     * Create and send a MSG_ENGINE_RESULTPROPS message with the Properties  
     * result from processing a previous command/dispatch.  This is the preferred way to return results 
     * since more information can be transferred.
     * @return true on successfully sent
     * @see #sendServiceResult(int,String)
     */
    public boolean sendServiceResult(Properties props){
		return sendServiceProperties(MessageUtil.ID_ENGINE_RESULTPROPS, props);		
    }
    
    public boolean sendServiceProperties(int what, Properties props){
		Message msg = Message.obtain(null, what);
		msg.replyTo = mMessenger;
		debug("Engine sending result Propertie.");
		
		try {
			if(messageHandler!=null)
				return messageHandler.sendMessageAsMultipleParcels(mService, msg, props);
			else{
				CharArrayWriter chars = new CharArrayWriter();
				props.store(chars, "ResultProperties");
				char[] buffer = chars.toCharArray();
				msg.obj = MessageUtil.setParcelableProps(buffer);
				mService.send(msg);
				return true;
			}
		} catch (Exception x) {
			debug(listenerName + ": Failed to send to MessengerService due to " + org.safs.sockets.Message.getStackTrace(x));
		}
		
		return false;		
    }

    /**
     * Create and send a MSG_ENGINE_SHUTDOWN signaling the engine has or is in the process of shutting down. 
     * @return true on successfully sent
     * @see #sendServiceMessage(int, String) 
     */
    public boolean sendShutdown(){
    	debug("sendShutdown...(null)");
    	return sendServiceMessage(MessageUtil.ID_ENGINE_SHUTDOWN, null);
    }

    /**
     * Create and send a MSG_ENGINE_READY signaling the engine is ready to process remote commands. 
     * @return true on successfully sent
     * @see #sendServiceMessage(int, String) 
     */
    public boolean sendReady(){
    	debug("sendReady...(null)");
    	return sendServiceMessage(MessageUtil.ID_ENGINE_READY, null);
    }

    /**
     * Create and send a MSG_ENGINE_RUNNING signaling the engine is processing the remote command. 
     * @return true on successfully sent
     * @see #sendServiceMessage(int, String) 
     */
    public boolean sendRunning(){
    	debug("sendRunning...(null)");
    	return sendServiceMessage(MessageUtil.ID_ENGINE_RUNNING, null);
    }

    /**
     * Create and send a MSG_ENGINE_DEBUG message. 
     * This allows the engine to route debug messages to 
     * external debug logging mechanisms handled by the remote test controller. 
     * @return true on successfully sent
     * @see #sendServiceMessage(int, String) 
     */
    public boolean sendDebug(String message){
    	//Log.d(listenerName, "sendDebug..."); /* infinite loop for some debugListeners if sent to debug() */
    	return sendServiceMessage(MessageUtil.ID_ENGINE_DEBUG, message);
    }

    /**
     * Create and send a MSG_SERVER_MESSAGE signaling the engine has sent a custom/arbitrary message or response 
     * to the remote test controller.  It is the controller\engine developer that provides the logic to send and receive 
     * these messages and to know what to do with them when received. 
     * @return true on successfully sent
     * @see #sendServiceMessage(int, String) 
     */
    public boolean sendMessage(String message){
    	debug("sendMessage...");
    	return sendServiceMessage(MessageUtil.ID_ENGINE_MESSAGE, message);
    }

    /**
     * Create and send a MSG_ENGINE_EXCEPTION signaling the engine has detected and caught an Exception and 
     * is reporting that to the remote test controller. 
     * @return true on successfully sent
     * @see #sendServiceMessage(int, String) 
     */
    public boolean sendException(String message){
    	debug("sendException...");
    	return sendServiceMessage(MessageUtil.ID_ENGINE_EXCEPTION, message);
    }
    
    /**
     * Create and send a ID_REGISTER_ENGINE signaling the engine is registered. 
     * @return true on successfully sent
     * @see #sendServiceMessage(int, String) 
     */
    public boolean sendRegisterEngine(){
    	debug("sendRegisterEngine...");
    	return sendServiceMessage(MessageUtil.ID_REGISTER_ENGINE, null);
    }
    
    /**
     * Create and send a ID_UNREGISTER_ENGINE signaling the engine is un-registered. 
     * @return true on successfully sent
     * @see #sendServiceMessage(int, String) 
     */
    public boolean sendUnRegisterEngine(){
    	debug("sendUnRegisterEngine...");
    	return sendServiceMessage(MessageUtil.ID_UNREGISTER_ENGINE, null);
    }

	public void onAllParcelsHaveBeenHandled(String messageID) {
		sendServiceMessage(MessageUtil.ID_ALL_PARCELS_ACKNOWLEDGMENT, messageID);
	}
	
	public void onParcelHasBeenHandled(String messageID, int index){
		sendServiceParcelAcknowledge(messageID, index);		
	}
}
