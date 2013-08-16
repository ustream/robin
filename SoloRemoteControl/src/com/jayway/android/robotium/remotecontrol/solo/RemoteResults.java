/** Copyright (C) SAS Institute All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.solo;

import java.util.Properties;

/**
 * Parses a SoloRemoteControl results Properties object into useful primitives. 
 * @author canagl
 */
public class RemoteResults {
 
	private Properties _resultProperties = null;
	private boolean _isRemoteResult = false;
	private int _statusCode = Message.STATUS_REMOTE_NOT_EXECUTED;
	private String _statusInfo = null;
	private String _errorMessage = null;
	
	private RemoteResults(){}
	
	/**
	 * Instantiate a RemoteResults Properties object and parse the known values from 
	 * the Properties into useful primitive methods.
	 * @param resultProperties
	 */
	public RemoteResults(Properties resultProperties){
		this._resultProperties = resultProperties;
		try{
			_isRemoteResult = getBoolean(Message.KEY_ISREMOTERESULT);
			_statusCode = getInt(Message.KEY_REMOTERESULTCODE);
			try{ _statusInfo = getString(Message.KEY_REMOTERESULTINFO);}catch(Exception x){}
			try{ _errorMessage = getString(Message.PARAM_ERRORMSG);}catch(Exception x){}
		}catch(Exception x){}
	}

	/** @return the original Properties used when created, if any. */
	public Properties getResultsProperties(){ return _resultProperties; }
	/** @return true if the parsed results set this to true. */
	public boolean isRemoteResult()         { return _isRemoteResult; }
	/** @return the statusCode parsed from the results. */
	public int getStatusCode()              { return _statusCode; }
	/** @return the statusInfo parsed from the results, if any.  Can be null. */
	public String getStatusInfo()           { return _statusInfo; }
	/** @return the errorMessage parsed from the results, if any. Can be null. */
	public String getErrorMessage()         { return _errorMessage; }
	
	/** @return true if the wrapped result contains a value for the provided key. */
	public boolean hasItem(String key){
		try{ return _resultProperties.containsKey(key);}
		catch(Exception x){ return false;}
	}
	
	/**
	 * Retrieves an item out of the results.
	 * @param key name of the item in the results to retrieve.  This is case-sensitive. 
	 * Subclasses could override this method to make item names insensitive to case.
	 * @return the item as stored in the results.
	 * @throws RuntimeException if the item does not exist in the results.
	 */
	public String getString(String item){
		if(hasItem(item)) return _resultProperties.getProperty(item);
		throw new RuntimeException(item +" does not exist in RemoteResults.");
	}
	
	/**
	 * Retrieves an item out of the results or a defaultValue if the item was not found.
	 * @param key name of the item in the results to retrieve.  This is case-sensitive. 
	 * Subclasses could override this method to make item names insensitive to case.
	 * @param defaultValue to return if the sought item is not in the result.
	 * @return the item as stored in the results, or the default if results does not contain the item.
	 */
	public String getString(String item, String defaultValue){
		try{ return getString(item); }catch(Exception x){}
		return defaultValue;
	}
	
	/**
	 * Retrieve an expected int value from the results.
	 * @param item Key or Parameter name expected to be stored in the results.
	 * @return the int value of a results item expected to be an integer.
	 * @throws NumberFormatException if the requested item is not a parsable integer.
	 * @throws RuntimeException if the requested item does not exist in the results.
	 * @see Integer#parseInt(String)
	 */
	public int getInt(String item){
		return Integer.parseInt(getString(item));
	}
	
	/**
	 * Retrieve an expected int value from the results or a defaultValue if the item was not found.
	 * @param item Key or Parameter name expected to be stored in the results.
	 * @param defaultValue to return if the sought item is not in the result.
	 * @return the int value of a results item expected to be an integer, or the defaultValue.
	 */
	public int getInt(String item, int defaultValue){
		try{ return getInt(item);}catch(Exception x){}
		return defaultValue;
	}
	
	/**
	 * Retrieve an expected boolean value from the results.
	 * @param item Key or Parameter name expected to be stored in the results.
	 * @return true if the requested item exists and the Boolean class recognizes the 
	 * value as equivalent to boolean true.  Otherwise, returns false.
	 * @throws RuntimeException if the requested item does not exist in the results.
	 * @see Boolean#parseBoolean(String)
	 */
	public boolean getBoolean(String item){
		return Boolean.parseBoolean(getString(item));
	}

	/**
	 * Retrieve an expected boolean value from the results or a defaultValue if the item is not found.
	 * @param item Key or Parameter name expected to be stored in the results.
	 * @param defaultValue to return if the sought item is not in the result.
	 * @return true if the requested item exists and the Boolean class recognizes the 
	 * value as equivalent to boolean true.  Otherwise, returns false.
	 * @see Boolean#parseBoolean(String)
	 */
	public boolean getBoolean(String item, boolean defaultValue){
		try{ return getBoolean(item);}catch(Exception x){}
		return defaultValue;
	}
}
