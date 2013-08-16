/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger.client;

import org.safs.sockets.Message;

/** 
 *  
 * @author Lei Wang, SAS Institute, Inc
 * @since  Feb 16, 2012
 */
public class MessageResult {
	/**
	 * statuscode can be one of constants like STATUS_REMOTERESULT_XXX defined in MessageUtil
	 * @see org.safs.android.messenger.MessageUtil
	 */
	private int statuscode;
	/**
	 * statusinfo is any string to describe the running resutlt
	 * 
	 */	
	private String statusinfo;
	
	public int getStatuscode() {
		return statuscode;
	}
	public void setStatuscode(int statuscode) {
		this.statuscode = statuscode;
	}
	public String getStatusinfo() {
		return statusinfo;
	}
	public void setStatusinfo(String statusinfo) {
		this.statusinfo = statusinfo;
	}
	
	/**
	 * Reset or create a new MessageResult preset with STATUS_REMOTERESULT_OK.
	 * @param result MessageResult to be preset to OK.  If null a new MessageResult 
	 * will be created.
	 * @return  MessageResult reset with STATUS_REMOTERESULT_OK
	 * @see org.safs.sockets.Message#STATUS_REMOTERESULT_OK
	 */
	public static MessageResult getSuccessTestResult(MessageResult result){
		MessageResult myresult = null;
		if(result==null){
			myresult = new MessageResult();
		}else{
			myresult = result;
		}
		myresult.resetTestResult();
		myresult.setStatuscode(Message.STATUS_REMOTERESULT_OK);
		return myresult;
	}

	/**
	 * Reset or create a new MessageResult preset with STATUS_REMOTERESULT_FAIL.
	 * @param result MessageResult to be preset to FAIL.  If null a new MessageResult 
	 * will be created.
	 * @return  MessageResult reset with STATUS_REMOTERESULT_FAIL
	 * @see org.safs.sockets.Message#STATUS_REMOTERESULT_FAIL
	 */
	public static MessageResult getFailTestResult(MessageResult result){
		MessageResult myresult = null;
		if(result==null){
			myresult = new MessageResult();
		}else{
			myresult = result;
		}
		myresult.resetTestResult();
		myresult.setStatuscode(Message.STATUS_REMOTERESULT_FAIL);
		return myresult;
	}
	
	/**
	 * Reset statuscode to STATUS_REMOTERESULT_UNKNOWN.
	 * Reset statusinfo to an empty string ("").
	 * @see org.safs.sockets.Message#STATUS_REMOTERESULT_UNKNOWN
	 */
	public void resetTestResult(){
		statuscode = Message.STATUS_REMOTERESULT_UNKNOWN;
		statusinfo = "";
	}
	
}
