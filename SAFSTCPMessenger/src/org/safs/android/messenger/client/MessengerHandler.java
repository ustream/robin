/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger.client;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Properties;

import org.safs.android.messenger.MessageUtil;
import org.safs.android.messenger.MultipleParcelsHandler;

import android.os.Message;
import android.os.Parcelable;

/**
 * 
 * @see org.safs.android.messenger.client.MessengerRunner
 * @author Carl Nagle, SAS Institute, Inc.
 * @since   FEB 04, 2012	(CANAGL)	Initial version
 *   <br>	APR 25, 2013	(LeiWang)	Handle message of big size. 
 */
public class MessengerHandler extends MultipleParcelsHandler{

	MessengerListener listener = null;
	
	public MessengerHandler(MessengerListener listener) {
		super(listener);
		this.listener = listener;
	}

	protected void debug(String message){
		if(listener!=null){
			listener.onReceiveDebug(message);
		}else{
			System.err.println(message);
		}
	}
	
	@Override
	public void handleWholeMessage(Message msg){
		
		listener.prepareNotification(msg.what);
		switch (msg.what){
			case MessageUtil.ID_ENGINE_DISPATCHPROPS:
			try{
				Properties props = new Properties();
					props.load(new CharArrayReader(MessageUtil.getParcelableProps((Parcelable)msg.obj)));
					listener.onRemoteDispatchProps(props);
				}catch(NullPointerException x){
					debug("DispatchProps message did NOT have required Properties Parcel!");
				}catch(IOException x){
					debug("DispatchProps IOException "+ x.getMessage());
				}
				break;
			case MessageUtil.ID_ENGINE_DISPATCHFILE:
				listener.onRemoteDispatchFile(MessageUtil.getParcelableMessage((Parcelable)msg.obj));
				break;
	
			case MessageUtil.ID_ENGINE_SHUTDOWN:
				listener.onRemoteEngineShutdown();
				break;
	
			case MessageUtil.ID_SERVER_SHUTDOWN:
				listener.onServiceShutdown();
				break;
	
			case MessageUtil.ID_ENGINE_MESSAGE:
				listener.onRemoteMessage(MessageUtil.getParcelableMessage((Parcelable)msg.obj));
				break;
	
			case MessageUtil.ID_SERVER_CONNECTED:
				listener.onRemoteConnected();
				break;
	
			case MessageUtil.ID_SERVER_DISCONNECTED:
				listener.onRemoteDisconnected();
				break;
		}

	}
}
