/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.client.processor;

import java.util.Properties;

import org.safs.android.messenger.client.MessageResult;


public interface ProcessorInterface {
	public void processProperties(Properties props);
	public MessageResult processMessage(String message);
	public void setRemoteCommand(String remoteCommand);
}
