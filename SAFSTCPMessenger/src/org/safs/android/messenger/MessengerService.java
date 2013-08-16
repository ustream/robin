/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger;

import org.safs.sockets.DebugListener;
import org.safs.sockets.SocketProtocol;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * TCP Messenger Service acting as intermediary between a remote test controller communicating over TCP Sockets 
 * and an Android test package expecting two-way communication for receiving test commands and returning 
 * test results and data.  This Service allows a test package with no INTERNET permissions to effectively be 
 * remotely controlled via TCP Sockets.
 * <p>
 * This Service handles the TCP Sockets communication with the remote test controller and forwards all data 
 * exchanges with the local test package via the Inter-Process Communication Messenger facilities provided 
 * by the Android OS.
 * <p>
 * Currently, this service is accepting connections on port 2410.
 * Currently, the remote controller server uses port 2411 to contact and attempt a connection to this service.
 * Both sides eventually need to be able to use a broader range of ports to prevent conflicts with 
 * other system resources.
 * <p>
 * When using the Android Emulator, the emulator must be configured to "see" Socket requests on its local 
 * port 2410 coming from the controller on port 2411.  Do this with the adb forwarding command to the running 
 * emulator as follows:
 * <p>
 * adb forward tcp:2411 tcp:2410
 * <p>
 * There is an initial handshake or verification that occurs between the remote controller server and  
 * this Service to confirm the device port owner is a SAFS TCP Messenger Service.
 * 
 * @see org.safs.sockets.RemoteClientRunner
 * @author Carl Nagle, SAS Institute, Inc.
 * @since   FEB 04, 2012	(CANAGL)	Initial version
 * 	<br>	APR 25, 2013	(LeiWang)	Handle message of big size.
 */
public class MessengerService extends Service implements RemoteClientListener, 
                                                         DebugListener, 
                                                         MessengerListener
{
	public static final String TAG = "SAFSMessenger";
	
    public static final String KEY_ENGINE_NAME="ENGINE_NAME";
    
	public static final CharSequence NOTIFICATION_TEXT = "SAFS Messenger Service";
	public static final int NOTIFICATION_DEFAULT_ID = 1;
		
	NotificationManager mNM;	
	RemoteClientRunner tcpServer;
	Looper mServiceLooper;
	MessengerHandler mServiceHandler;
	Messenger serviceMessenger;
	Messenger engineMessenger;
	org.safs.sockets.Message safs = new org.safs.sockets.Message();
	
    protected void debug(String text){
    	onReceiveDebug(text);
    }
    
    public void onReceiveDebug(String text){
    	Log.d(TAG, text);
    }
    
	/**
	 * Show an Android on-device Notification while running....
	 */
	private void showNotification(){
		Notification notification = new Notification(R.drawable.bidi_arrows, 
				                    NOTIFICATION_TEXT,  
				                    System.currentTimeMillis());
		//Intent notificationIntent = new Intent(this, StartupActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MessengerService.class),0);
		notification.setLatestEventInfo(this, NOTIFICATION_TEXT, NOTIFICATION_TEXT, pendingIntent);
		startForeground(NOTIFICATION_DEFAULT_ID, notification);
	}
	
	
	/**
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("SAFSMessengerService", android.os.Process.THREAD_PRIORITY_FOREGROUND);
		thread.start();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new MessengerHandler(mServiceLooper, this);
		serviceMessenger = new Messenger(mServiceHandler);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		showNotification();
		tcpServer = new RemoteClientRunner(this);
		new Thread(tcpServer).start();
	}

	/**
	 * Automatically called when the Service is being started up--usually from the test package engine 
	 * requesting to bind with the Service.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		android.os.Message msg = mServiceHandler.obtainMessage();		
		msg.arg1 = startId;		
		mServiceHandler.sendMessage(msg);
		//return START_STICKY;
		return START_NOT_STICKY;
	}
	
	/** 
	 * Capture the Message handler for the test package that is binding with the Service.
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return serviceMessenger.getBinder();
	}

	/**
	 * Cancel any Notification we have as we die and shutdown our SocketServer.
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		tcpServer.shutdownThread();
		tcpServer = null;
		debug("Service has been destroyed!");
		mNM.cancel(NOTIFICATION_DEFAULT_ID);
	}

	protected void sendTCPMessage(String message){
		debug("sendTCPMessage: "+ message);
		try{ tcpServer.sendProtocolMessage(message);}
		catch(Exception x){
			debug("sendTCPMessage "+ x.getClass().getSimpleName()+", "+ x.getMessage());
		}
	}
	
	public String getListenerName() {
		return tcpServer.getListenerName();
	}

	//***********************************************************************
	// Begin MessengerListener API
	//***********************************************************************
	
	public void onMessengerDebug(String message) {
		debug(message);
	}

	public void onEngineDebug(String message) {
		sendTCPMessage(safs.msg_debug +	safs.msg_sep + message);
	}

	public void onEngineException(String message) {
		sendTCPMessage(safs.msg_exception + safs.msg_sep + message);
	}

	public void onEngineMessage(String message) {
		sendTCPMessage(safs.msg_message + safs.msg_sep + message);
	}

	public void onEngineResult(int statuscode, String statusinfo) {
		sendTCPMessage(safs.msg_result + safs.msg_sep + String.valueOf(statuscode).trim() +
				       safs.msg_sep + statusinfo);
	}

	public void onEngineResultProps(char[] props) {
		sendTCPMessage(safs.msg_resultprops + safs.msg_sep + String.valueOf(props));
	}

	public void onEngineShutdown(int cause) {
		if(cause == SocketProtocol.STATUS_SHUTDOWN_NORMAL)
			sendTCPMessage(safs.msg_remoteshutdown);
		else
			sendTCPMessage(safs.msg_shutdown);
	}

	public void onEngineReady() {
		sendTCPMessage(safs.msg_ready);
	}

	public void onEngineRegistered(Messenger messenger) {
		debug("received EngineRegistered notification...");
		engineMessenger = messenger;
	}

	public void onEngineUnRegistered() {
		engineMessenger = null;
		sendTCPMessage(safs.msg_remoteshutdown);
		stopSelf();
	}

	public void onEngineRunning() {
		sendTCPMessage(safs.msg_running);
	}

	//***********************************************************************
	// Begin SocketServerListener API
	//***********************************************************************
	
	protected void sendIPCMessage(int what){
		Message msg = mServiceHandler.obtainMessage(what);
		msg.replyTo = serviceMessenger;
		try{
			debug("sending IPC Message ID: "+ what);
			engineMessenger.send(msg);
		}
		catch(Exception x){
			debug("sendIPCEvent "+ x.getClass().getSimpleName()+", "+ x.getMessage());
		}
	}

	protected void sendServiceParcelAcknowledge(String messageID, int index){
		Message msg = mServiceHandler.obtainMessage(MessageUtil.ID_PARCEL_ACKNOWLEDGMENT);
		msg.replyTo = serviceMessenger;
		msg.arg1 = index;
		
		try{
			debug("sending IPC parcel acknowledge: "+ messageID +", "+ index);
			if(messageID!=null) msg.obj = MessageUtil.setParcelableMessage(messageID);
			engineMessenger.send(msg);
		}
		catch(Exception x){
			debug("sendIPCEvent "+ x.getClass().getSimpleName()+", "+ x.getMessage());
		}
	}
	
	protected void sendIPCMessage(int what, String msgobj){
		Message msg = mServiceHandler.obtainMessage(what);
		msg.replyTo = serviceMessenger;
		try{ 
			debug("sending IPC Message ID: "+ what +", "+ msgobj);
			mServiceHandler.sendMessageAsMultipleParcels(engineMessenger, msg, msgobj);
		}
		catch(Exception x){
			debug("sendIPCString "+ x.getClass().getSimpleName()+", "+ x.getMessage());
		}
	}

	protected void sendIPCMessage(int what, char[] props){
		Message msg = mServiceHandler.obtainMessage(what);
		msg.replyTo = serviceMessenger;
		try{ 
			debug("sending IPC Message ID: "+ what +", "+ String.valueOf(props));
			mServiceHandler.sendMessageAsMultipleParcels(engineMessenger, msg, props);
		}
		catch(Exception x){
			debug("sendIPCProps "+ x.getClass().getSimpleName()+", "+ x.getMessage());
		}
	}
	
	public void onReceiveDispatchFile(String filepath) {
		sendIPCMessage(MessageUtil.ID_ENGINE_DISPATCHFILE, filepath);
	}

	public void onReceiveDispatchProps(char[] props) {
		sendIPCMessage(MessageUtil.ID_ENGINE_DISPATCHPROPS, props);
	}

	public void onReceiveMessage(String message) {
		sendIPCMessage(MessageUtil.ID_ENGINE_MESSAGE, message);
	}	
	public void onReceiveConnection() {
		sendIPCMessage(MessageUtil.ID_SERVER_CONNECTED);
	}

	/**
	 * The controller has requested a shutdown.
	 */
	public void onReceiveRemoteShutdown(int cause) {
		sendIPCMessage(MessageUtil.ID_ENGINE_SHUTDOWN);
	}

	/**
	 * The service has shutdown.  Catastrophic?
	 */
	public void onReceiveLocalShutdown(int cause) {
		sendIPCMessage(MessageUtil.ID_SERVER_SHUTDOWN);
	}

	public void onAllParcelsHaveBeenHandled(String messageID) {
		sendIPCMessage(MessageUtil.ID_ALL_PARCELS_ACKNOWLEDGMENT, messageID);
	}

	public void onParcelHasBeenHandled(String messageID, int index){
		sendServiceParcelAcknowledge(messageID, index);		
	}
}
