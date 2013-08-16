/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger.client;

/**
 * Encapsulates abstract logging or other reporting activity allowing for different 
 * outlets of test logging and reporting.
 * @author Carl Nagle, SAS Institute, Inc.
 */
public interface LogsInterface {

	/** Log or otherwise report a passed/success message */
	public void pass(String action, String message);
	/** Log or otherwise report a failure/error message */
	public void fail(String action, String message);
	/** Log or otherwise report a warning message */
	public void warn(String action, String message);
	/** Log or otherwise report a generic informative message */
	public void info(String message);
	/** Log or otherwise report a debug message */
	public void debug(String message);

	/** 
	 * enable or disable the logging or reporting of debug messages. 
	 * In many implementations, disabling debug logging can improve overall runtime performance. */
	public void enableDebug(boolean enabled);
	
	/** @return true if debug logging is enabled. false otherwise. */
	public boolean isDebugEnabled();
}
