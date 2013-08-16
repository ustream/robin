/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger.client;

import java.util.Properties;

import org.safs.sockets.NamedListener;


/** 
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  Feb 16, 2012
 *
 */
public interface CommandListener extends NamedListener{
	//props will take back the result
	public void handleDispatchProps(Properties props);
	public MessageResult handleDispatchFile(String filename);
	public MessageResult handleMessage(String message);
	public MessageResult handleServerConnected();
	public MessageResult handleServerDisconnected();
	public MessageResult handleEngineShutdown();
	public MessageResult handleServerShutdown();
	public MessageResult handleRemoteShutdown();
	
	public void messengerRunnerStopped();
}
