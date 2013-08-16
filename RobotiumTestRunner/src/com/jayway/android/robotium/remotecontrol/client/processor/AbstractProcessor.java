/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.client.processor;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;

import org.safs.android.messenger.client.MessageResult;

import com.jayway.android.robotium.remotecontrol.client.AbstractTestRunner;
import com.jayway.android.robotium.remotecontrol.client.SoloMessage;

/** 
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since
 * <br>May 17, 2013		(SBJLWA)	Move some static methods to com.jayway.android.robotium.remotecontrol.solo.Message
 */
public abstract class AbstractProcessor implements ProcessorInterface {
	public static String TAG = AbstractProcessor.class.getName();
	
	/**
	 * The command from the 'reomote solo'
	 */
	protected String remoteCommand = null;
	
	/**
	 * A TestRunner by definition is already a DebugListener.
	 * This field can serve as both, if desired.
	 */
	protected AbstractTestRunner testRunner = null;
	
	public AbstractProcessor(AbstractTestRunner testRunner){
		this.testRunner = testRunner;
	}
	
	/**
	 * Forward debug messages to our testRunner which may or may not have debug enabled
	 * for performance reasons.
	 * <p>
	 * This method will NOT add any prefix or suffix to the message.  It is the responsibility 
	 * of the caller to provide the complete message to be logged.  This is to prevent cases where 
	 * debugPrefix is NOT used or changed by the caller and incorrect debug output occurs.
	 */
	protected void debug(String message){
		testRunner.debug(message);
	}
	
	/**
	 * Create a String from a Throwable suitable for debug output that provides 
	 * comparable information to x.printStackTrace();
	 * @param x
	 * @return String ready for output to debug(String) or other sink.
	 */
	protected String getStackTrace(Throwable x){
		String rc = x.getClass().getName()+", "+ x.getMessage()+"\n";
		StackTraceElement[] se = x.getStackTrace();
		for(StackTraceElement s:se){ rc += s.toString()+"\n"; }
		return rc;
	}
	
	/**
	 * Before calling this method, you may need to call setRemoteCommand()
	 *  
	 * @see ProcessorInterface#processProperties(Properties)
	 */
	public void processProperties(Properties props) {
		
	}

	/**
	 *  
	 * @see ProcessorInterface#processMessage(String)
	 */
	public MessageResult processMessage(String message) {
		return null;
	}

	/**
	 *  
	 * @see ProcessorInterface#setRemoteCommand(String)
	 */
	public void setRemoteCommand(String remoteCommand) {
		this.remoteCommand = remoteCommand;
	}
	
	/**
	 * Call to set a "&lt;command> : successful" result into the properties object.
	 * <p>  
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_OK_STRING}<br>
	 * 
	 * <b>Note:</b> This method will concatenate {@link #remoteCommand} with <br>
	 * {@link SoloMessage#RESULT_INFO_GENERAL_SUCCESS}<br> and set this string to remoteresultinfo<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param resultInfo	The result information to be modified and set to remoteresultinfo of props.
	 */
	protected void setGeneralSuccess(Properties props){
		setGeneralSuccess(props, "");
	}	
	
	/**
	 * Call to set a "&lt;command> : &lt;resultInfo> successful." result into the properties object.
	 * <p>
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_OK_STRING}<br>
	 * 
	 * <b>Note:</b> This method will concatenate {@link #remoteCommand} with 'resultInfo', then with <br>
	 * {@link SoloMessage#RESULT_INFO_GENERAL_SUCCESS}<br> and set this string to remoteresultinfo<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param resultInfo	The result information to be modified and set to remoteresultinfo of props.
	 */
	protected void setGeneralSuccess(Properties props, String resultInfo){
		resultInfo = resultInfo != null ? resultInfo: SoloMessage.NULL_VALUE;
		if(remoteCommand!=null) resultInfo +=  " : '"+ remoteCommand + "' ";
		resultInfo += SoloMessage.RESULT_INFO_GENERAL_SUCCESS;
		setGeneralSuccessWithSpecialInfo(props, resultInfo);
	}
	
	/**
	 * Call to set a "&lt;resultInfo>" remoteresultinfo into the properties object unmodified.
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_OK_STRING}<br>
	 * set the remoteresultinfo to the string given by parameter resultInfo<br>
	 * 
	 * <b>Note:</b> If remoteresultinfo should contain a result of a predefined or unmodified format 
	 * you MUST call this method.<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param resultInfo	The result information to be set unmodified to remoteresultinfo of props.
	 */
	protected void setGeneralSuccessWithSpecialInfo(Properties props, String resultInfo){
		try{
			resultInfo = resultInfo != null ? resultInfo: SoloMessage.NULL_VALUE;
			props.setProperty(SoloMessage.KEY_ISREMOTERESULT, String.valueOf(true));
			props.setProperty(SoloMessage.KEY_REMOTERESULTCODE, SoloMessage.STATUS_REMOTERESULT_OK_STRING);
			props.setProperty(SoloMessage.KEY_REMOTERESULTINFO, resultInfo);
		}catch(Exception e){
			//Properties.setProperty() may throw NullPointerException
			String debugmsg = TAG+".setGeneralSuccessWithSpecialInfo() ";
			debug(debugmsg+" Met Exception="+e.getMessage());
		}
	}
	
	/**
	 * Call to set a "&lt;resultInfo>" remoteresultinfo into the properties object unmodified.
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_WARN_STRING}<br>
	 * set the remoteresultinfo to the string given by parameter resultInfo<br>
	 * 
	 * <b>Note:</b> If remoteresultinfo should contain a result of a predefined or unmodified format 
	 * you MUST call this method.<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param resultInfo	The result information to be set unmodified to remoteresultinfo of props.
	 */
	protected void setGeneralWarningWithSpecialInfo(Properties props, String resultInfo){
		try{
			resultInfo = resultInfo != null ? resultInfo: SoloMessage.NULL_VALUE;
			props.setProperty(SoloMessage.KEY_ISREMOTERESULT, String.valueOf(true));
			props.setProperty(SoloMessage.KEY_REMOTERESULTCODE, SoloMessage.STATUS_REMOTERESULT_WARN_STRING);
			props.setProperty(SoloMessage.KEY_REMOTERESULTINFO, resultInfo);
		}catch(Exception e){
			//Properties.setProperty() may throw NullPointerException
			String debugmsg = TAG+".setGeneralWarningWithSpecialInfo() ";
			debug(debugmsg+" Met Exception="+e.getMessage());
		}
	}
	
	/**
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_FAIL_STRING}<br>
	 * 
	 * <b>Note:</b> This method will concatenate {@link #remoteCommand} with 'resultInfo', then with <br>
	 * {@link SoloMessage#RESULT_INFO_GENERAL_FAIL}<br> and set this string to remoteresultinfo<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param resultInfo	The result information to be modified and set to remoteresultinfo of props.
	 */
	protected void setGeneralError(Properties props, String resultInfo){
		setGeneralError(props, resultInfo, null);
	}
	
	/**
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_FAIL_STRING}<br>
	 * set the errormsg to the string given by parameter errorMessage<br>
	 * 
	 * <b>Note:</b> This method will concatenate {@link #remoteCommand} with 'resultInfo', then with <br>
	 * {@link SoloMessage#RESULT_INFO_GENERAL_FAIL}<br> and set this string to remoteresultinfo<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param resultInfo	The result information to be set to resultinfo of props.
	 * @param errorMessage	The error message to be set to errormessage of props.  Null if no 
	 * error message is to be sent.
	 */
	protected void setGeneralError(Properties props, String resultInfo, String errorMessage){
		resultInfo = resultInfo != null ? resultInfo: SoloMessage.NULL_VALUE;
		if(remoteCommand!=null) resultInfo +=  " : '"+remoteCommand+"'";
		resultInfo += SoloMessage.RESULT_INFO_GENERAL_FAIL;
		setGeneralErrorWithSpecialInfo(props, resultInfo, errorMessage);
	}

	/**
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_FAIL_STRING}<br>
	 * set the remoteresultinfo to the string given by parameter resultInfo<br>
	 * 
	 * <b>Note:</b> If remoteresultinfo should contain result of a predefined or unmodified format 
	 * you MUST call this method.<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param resultInfo	The result information to be set unmodified to remoteresultinfo of props.
	 */
	protected void setGeneralErrorWithSpecialInfo(Properties props, String resultInfo){
		setGeneralErrorWithSpecialInfo(props, resultInfo, null);
	}
	
	/**
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_FAIL_STRING}<br>
	 * set the errormsg to the string given by parameter errorMessage<br>
	 * 
	 * <b>Note:</b> If remoteresultinfo should contain result of 'predefined format', you MUST call this method.<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param resultInfo	The result information to be set unmodified to resultinfo of props.
	 * @param errorMessage	The error message to be set to errormessage of props.  Null if no error message 
	 * is to be sent.
	 */
	protected void setGeneralErrorWithSpecialInfo(Properties props, String resultInfo, String errorMessage){
		try{
			resultInfo = resultInfo != null ? resultInfo: SoloMessage.NULL_VALUE;
			props.setProperty(SoloMessage.KEY_ISREMOTERESULT, String.valueOf(true));
			props.setProperty(SoloMessage.KEY_REMOTERESULTCODE, SoloMessage.STATUS_REMOTERESULT_FAIL_STRING);
			props.setProperty(SoloMessage.KEY_REMOTERESULTINFO, resultInfo);
			if(errorMessage!=null){
				props.setProperty(SoloMessage.PARAM_ERRORMSG, errorMessage);
			}
		}catch(Exception e){
			//Properties.setProperty() may throw NullPointerException
			String debugmsg = TAG+".setGeneralErrorWithSpecialInfo() ";
			debug(debugmsg+" Met Exception="+e.getMessage());
		}
	}
	
	
	protected int INITIAL_CACHE_SIZE = 50;
	/**
	 * Clear and\or reset the internal component cache used in non-typical modes 
	 * of operation like MODE_EXTERNAL_PROCESSING (Process Container). 
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @see #makeUniqueCacheKey(Object)
	 * @see #putCachedItem(Hashtable, Object, Object)
	 * @see #getCachedItem(Hashtable, Object)
	 * @see #removeCachedItem(Hashtable, Object)
	 */
	public void resetExternalModeCache(Hashtable cache){
		if (cache != null) cache.clear();
	}
	
	/**
	 * Attempts to retrieve an item from cache using the provided key.
	 * 
	 * If not found will attempt to use key as-is.
	 * If not found will return key as-is.
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @param key Object to use as lookup reference into cache
	 * @return Object stored in cache or null.
	 * @see #makeUniqueCacheKey(Object)
	 * @see #putCachedItem(Hashtable, Object, Object)
	 * @see #removeCachedItem(Hashtable, Object)
	 */
	protected Object getCachedItem(Hashtable cache, Object key){
		Object item = null;
		if (cache==null) return key;
		if(key==null) return null;
		if (key instanceof String) item = cache.get(key);		
		if (item==null) item = cache.get(key);
//		if (item==null) item = key;
		return item;
	}
	
	/** 
	 * Remove an item from cache.  
	 * Will attempt to use key as-is.
	 * 
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @param key Object to use as lookup reference into cache
	 * @return the Object removed or null if not found.
	 * @see #makeUniqueCacheKey(Object)
	 * @see #getCachedItem(Hashtable, Object)
	 * @see #putCachedItem(Hashtable, Object, Object)
	 */
	protected Object removeCachedItem(Hashtable cache, Object key){
		Object item = null;
		if ((cache==null)||(key==null)) return null;
		item = cache.remove(key);
		if ((item==null)&&(key instanceof String)) item = cache.remove(key);		
		return item;
	}

	/**
	 * Attempts to put an item in cache using the provided key.
	 * 
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @param key Object to use as lookup reference into cache.
	 * @param item Item to store in the cache.
	 * @throws IllegalArgumentException if either cache or key or item is null. 
	 * @see #makeUniqueCacheKey(Object)
	 * @see #getCachedItem(Hashtable, Object)
	 * @see #removeCachedItem(Hashtable, Object)
	 */
	protected void putCachedItem(Hashtable cache, Object key, Object item){
		if (cache==null){
			throw new IllegalArgumentException("The cache CAN NOT be null!");
		}
		
		try{
			cache.put(key, item);
			
		}catch(NullPointerException np){
			throw new IllegalArgumentException("Neither cache key nor item can be null.");
		}
	}
	
	/**
	 * Test if the value is contained in the local cache.<br>
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @param value		Object to be checked if it is in the cache.
	 */
	boolean cacheContainValue(Hashtable cache, Object value){
		if(cache == null) return false;
		
		return cache.containsValue(value);
	}
	
	/**
	 * To check if the local cache contains the expectedValue.<br>
	 * If found, return the corresponding key, otherwise return null.<br>
	 * 
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @param expectedValue		Object, the value to be checked
	 * @return		Object, the key for the value found in cache.<br>
	 *              null,   if the value can't be found in cache.<br>
	 */
	Object getCacheKeyForValue(Hashtable cache, Object expectedValue){
		Object key = null;
		Object value = null;
		
		if(cacheContainValue(cache, expectedValue)){
			Enumeration<Object> enumerator = cache.keys();
			while(enumerator.hasMoreElements()){
				key = enumerator.nextElement();
				value = cache.get(key);
				//As cache is a Hashtable, which doesn't permit null as key or value
				//so all the values got from it will NOT be null.
				if(value.equals(expectedValue)) break;
			}
		}
		
		return key;
	}
	
	/**
	 * Convert a list of engine-specific objects to an array of unique keys 
	 * in the cache.  The items will be stored in the cache using the unique keys.
	 * 
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @param itemsList List of objects to store in the cache.
	 * @return an array of String keys used to retrieve the items from the cache.
	 * @see #convertToKeys(Hashtable, Object[])
	 * 
	 */
	protected String[] convertToKeys(Hashtable cache, List itemsList){
		Object[] items = null;
		
		if(itemsList != null){
			items = itemsList.toArray();
		}

		return	convertToKeys(cache, items);
	}
	
	/**
	 * Convert an array of engine-specific objects to an array of unique keys 
	 * in the cache.  The items will be stored in the cache using the unique keys.
	 * 
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @param items Array of objects to store in the cache.
	 * @return an array of String keys used to retrieve the items from the cache.
	 * 
	 * @see #convertToKey(Hashtable, Object)
	 * @see #makeUniqueCacheKey(Object)
	 * @see #getCachedItem(Hashtable, Object)
	 * @see #putCachedItem(Hashtable, Object, Object)
	 * 
	 * @author CANAGL APR 23,2010 handle case of null items in items array.
	 * @author sbjlwa MAR 01,2012 Return an array of String instead of Object, 
	 *                            as makeUniqueCacheKey() return only String.
	 */
	protected String[] convertToKeys(Hashtable cache, Object[] items){
		String[] keyarray = new String[0];
		
		if (items == null) return keyarray;
		Vector<String> keys = new Vector<String>();
		String key = null;
		
		for(int it=0; it<items.length;it++){
			key = convertToKey(cache, items[it]);
			if(key != null) keys.add(key);
		}
		
		if(keys.size()==0) return keyarray;

		return keys.toArray(keyarray);
	}
	
	/**
	 * Convert a item to a unique key.<br>
	 * If the cache contain the item, return the cached key directly.<br>
	 * Else, generate a new unique key and put the item into cache with that key<br>
	 * then return the key.<br>
	 * 
	 * @param cache  	Hashtable, MUST be an initialized object
	 * @param items	 	Array of objects to store in the cache.
	 * @return          String key used to retrieve the item from the cache.
	 * 
	 * @see #makeUniqueCacheKey(Object)
	 * @see #getCachedItem(Hashtable, Object)
	 * @see #putCachedItem(Hashtable, Object, Object)
	 * 
	 */
	protected String convertToKey(Hashtable cache, Object item){
		String debugPrefix = TAG+".convertToKey(): ";
		String key = null;
		
		if (item != null) {
			// check if item is in local cache
			try {
				key = (String) getCacheKeyForValue(cache, item);
			} catch (ClassCastException e) {
				// item exist in cache, but the key is not a String!!!
				key = null;
			}
			if (key == null) {
				key = makeUniqueCacheKey(item);
				try{
					//putCachedItem can throw IllegalArgumentException, if one of its parameter is null
					//although we are sure that key and item will not be null, but the cache may be null
					//we still need to catch this Exception
					putCachedItem(cache, key, item);
				}catch(Exception e){
					key = null;
					debug(debugPrefix+" Can NOT put object to cache. Exception="+e.getMessage());
				}
			}
		}else{
			debug(debugPrefix+" Can NOT generate a key for null object.");
		}
		
		if(key==null){
			//This should hardly happen
			debug(debugPrefix+" Can't get key for item!!!");
		}
		
		return key;
	}
	
	private static String __last_unique_key = "";
	/**
	 * Routine is used to create a unique ID String key that can be used by external 
	 * processes like Process Container to identify an engine-specific item in the 
	 * cache.<br>
	 * This method is thread-safe: it guarantees that multiple threads can get unique ID.<br>
	 * 
	 * @param item to be stored in cache.
	 * @return unique String suitable to be the key for the item.
	 * @see #putCachedItem(Hashtable, Object, Object)
	 * @see #getCachedItem(Hashtable, Object)
	 * @see #removeCachedItem(Hashtable, Object)
	 */
	protected String makeUniqueCacheKey(Object item){
		
		String timestamp = "";
		synchronized(__last_unique_key){
			do{	timestamp = UUID.randomUUID().toString();
			}while(timestamp.equals(__last_unique_key));
			__last_unique_key = timestamp;
		}
		return timestamp;
	}
	
	/**
	 * Test if the second string match with the first string.
	 * 
	 * @param string1, 		String, the first string
	 * @param string2, 		String, the second string
	 * @param partial		boolean, if the comparison is partial match.
	 * @param caseSensitive boolean, if the comparison is case sensitive.
	 * 
	 */
	public static boolean stringIsMatched(String string1, String string2, boolean partial, boolean caseSensitive){
		boolean matched = false;
		
		if(string1==null || string2==null) return string1==string2;
		
		if(caseSensitive){
			if(partial){
				matched = string1.contains(string2);
			}else{
				matched = string1.equals(string2);
			}
		}else{
			if(partial){
				matched= string1.toLowerCase().contains(string2.toLowerCase());
			}else{
				matched = string1.equalsIgnoreCase(string2);
			}
		}
		
		return matched;
	}
}
