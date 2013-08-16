/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.client.processor;

/**
 * Defines the required API for a Processor able to share cached object references.
 * @author canagl
 */
public interface CacheReferenceInterface {

	/**
	 * Retrieve a cached object from the class instance.
	 * @param key -- String key for identifying the object stored in cache.
	 * @param useChain -- true if the instance should search in all chained caches,  
	 * false if only the individual local cache should be searched.
	 * @return -- the object stored in cache identified by the key, or null if not found.
	 */
	public Object getCachedObject(String key, boolean useChain);
	
	/**
	 * Add an instance of CacheReferenceInterface to the set of caches to be chained 
	 * when seeking an object by key.
	 * @param cache
	 */
	public void addCacheReferenceInterface(CacheReferenceInterface cache);

	/**
	 * Remove an instance of CacheReferenceInterface from the set of caches to be chained  
	 * when seeking an object by key.
	 * @param cache
	 */
	public void removeCacheReferenceInterface(CacheReferenceInterface cache);

	/**
	 * Clear the cache of all object and key references.
	 * @param useChain -- true if the instance should clear all chained caches,  
	 * false if only the individual local cache should be cleared.
	 */
	public void clearCache(boolean useChain);
}
