/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger;

import org.safs.sockets.ConnectionListener;

/**
 * @author Carl Nagle, SAS Institute, Inc.
 *
 */
public interface RemoteClientListener extends ConnectionListener {
	
	public void onReceiveDispatchFile(String filepath);
	public void onReceiveDispatchProps(char[] props);
	public void onReceiveMessage(String message);
}
