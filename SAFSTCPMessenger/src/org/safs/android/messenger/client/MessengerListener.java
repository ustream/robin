/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger.client;

import java.util.Properties;

import org.safs.android.messenger.MultipleParcelListener;
import org.safs.sockets.DebugListener;
/** 
 * 
 * @author Carl Nagle, SAS Institute, Inc.
 * @since  Feb 02, 2012
 *
 */
public interface MessengerListener extends DebugListener, MultipleParcelListener{

	public void prepareNotification(int what);
	
	public void onRemoteConnected();
	public void onRemoteDisconnected();
	
	public void onRemoteDispatchFile(String filepath);
	public void onRemoteDispatchProps(Properties props);
	
	public void onRemoteMessage(String message);

	/** Notification that the Remote Controller has shutdown and is no longer available. */
	public void onRemoteShutdown();
	/** Notification that the Messenger Service has shutdown and is no longer available. */
	public void onServiceShutdown();
	/** Remote request/command to tell the engine to perform a normal shutdown. */
	public void onRemoteEngineShutdown();
	
}
