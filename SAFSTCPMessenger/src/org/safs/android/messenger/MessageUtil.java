/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger;

import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc.
 * @since   FEB 04, 2012	(CANAGL)	Initial version
 *   <br>	APR 25, 2013	(LeiWang)	Add some methods to assist handling message of big size.
 */
public class MessageUtil extends org.safs.sockets.Message{

	/** Server: the service is shutting down abnormally and will not be available. */
	public static final int ID_SERVER_SHUTDOWN       = 0;	
	/** Client: Register the local app client. */
	public static final int ID_REGISTER_ENGINE       = 1;
	/** Client: UnRegister the local app client. */
	public static final int ID_UNREGISTER_ENGINE     = 2;
	/** Client: local app is Ready to receive commands. */
	public static final int ID_ENGINE_READY          = 3;	
	/** Server: Notify local app a File is available for processing. */
	public static final int ID_ENGINE_DISPATCHFILE   = 4;
	/** Server: Notify local app a serialized Properties Object is available for processing. */
	public static final int ID_ENGINE_DISPATCHPROPS  = 5;	
	/** Client: local app is Running/Processing the File or Properties dispatched. */
	public static final int ID_ENGINE_RUNNING        = 6;	
	/** Client: local app is returning simple processing results: (statuscode, statusinfo). */
	public static final int ID_ENGINE_RESULT         = 7;
	/** Client: local app is returning a serialized Properties object containing results. */
	public static final int ID_ENGINE_RESULTPROPS    = 8;
	/** Client: local app is sending a generic Message. 
	    Server: remote TCP client is sending a generic Message. 	 */
	public static final int ID_ENGINE_MESSAGE        = 9;// send to engine
	/** Client: local app is sending a debug message. 
	    Server: send local app a server debug message.	 */
	public static final int ID_ENGINE_DEBUG          = 10;
	/** Client: local app is sending an Exception message. */
	public static final int ID_ENGINE_EXCEPTION      = 11;
	/** Server: a remote TCP client has connected. */
	public static final int ID_SERVER_CONNECTED      = 12;
	/** Server: a remote TCP client has disconnected. */
	public static final int ID_SERVER_DISCONNECTED   = 13;
	/** Server: a remote TCP client has requested a shutdown.
	    Client: the local appl has shutdown and should be sent no more messages.	 */
	public static final int ID_ENGINE_SHUTDOWN       = 14;

	/**
	 * If the message's size is bigger than {@value #MAX_TRANSFER_BYTE_SIZE} bytes,<br>
	 * the message will be divided into small parcels. To make sure all parcels will<br>
	 * arrive at the 'message-receiver', we need this acknowledgment.<br>
	 * Used by 'message-receiver' to send acknowledgment for all parcels arrived
	 */
	public static final int ID_ALL_PARCELS_ACKNOWLEDGMENT		= 50;
	/** 
	 * If the message's size is bigger than {@value #MAX_TRANSFER_BYTE_SIZE} bytes,<br>
	 * the message will be divided into small parcels. To make sure all parcels will<br>
	 * arrive at the 'message-receiver', we need this acknowledgment.<br>
	 * Used by 'message-receiver' to send acknowledgment for one parcel arrived
	 */
	public static final int ID_PARCEL_ACKNOWLEDGMENT			= 51;
	

	/** key used to extract generic String message from Bundle. */
    public static final String BUNDLE_MESSAGE    = "message";
	/** key used to extract serialized Properties object from Bundle. */
	public static final String BUNDLE_PROPERTIES = "properties";
	
    public static final String SERVICE_CONNECT_INTENT  = "org.safs.android.messenger.Connect";
    public static final String SERVICE_SHUTDOWN_INTENT = "org.safs.android.messenger.Shutdown";
    
    /** 
     * This field defines the max size can be transfered through Message Service in SAFS.<br>
     * <p>
     * Android Message: The Binder transaction buffer has a limited fixed size, currently 1Mb, 
     * which is shared by all transactions in progress for the process. Consequently 
     * TransactionTooLargeException can be thrown when there are many transactions 
     * in progress even when most of the individual transactions are of moderate size. 
     * </p>
     * <p>
     * So the real max size of data can be transfered is always smaller than that 1Mb.
     * Here we just give 0.25Mb as the max size to be permitted to transfer. But this also
     * may cause TransactionTooLargeException if a lot of transactions is in progress.
     * In future, we may adjust this value dynamically?
     * </p>
     * it is used to decide if a message should be transferred by a few parcels<br>
     */
    public static final int MAX_TRANSFER_BYTE_SIZE = 256*1024;
    /**
     * This field defines the block's size to be transfered, if the whole message's size<br>
     * is bigger than {@value #MAX_TRANSFER_BYTE_SIZE}<br>
     * it is used when transferring a message by a few parcels<br>
     */
    public static final int SMALL_TRANSFER_BYTE_SIZE = MAX_TRANSFER_BYTE_SIZE/4;
    /** 
     * key used to extract from Bundle a String value, which is message's ID<br>
     * it is used when transferring a message by a few parcels<br>
     */
    public static final String BUNDLE_SMALL_PARCEL_ID = "smallparcelid";
    /**
     * key used to extract from Bundle an int value, which is the index of parcels of a message<br>
     * it is used when transferring a message by a few parcels<br>
     */
    public static final String BUNDLE_SMALL_PARCEL_INDEX = "smallparcelindex";
    /**
     * key used to extract from Bundle an int value, which indicates the total number of<br> 
     * parcels of a whole message to be sent.<br>
     * it is used when transferring a message by a few parcels<br>
     */
    public static final String BUNDLE_SMALL_PARCEL_TOTALNUMBER = "smallparceltotalnumber";
    
    /**
     * key used to extract from Bundle an boolean value, which indicates if this parcel<br>
     * is sent again or not by 'message-sender'.<br>
     * it is used when transferring a message by a few parcels<br>
     */
	public static final String BUNDLE_SMALL_RESENT_PARCEL		= "smallparcelresent";
    
	/**
	 * Create a Parcelable Bundle containing a String message for transport to the test package engine.
	 * The String message is stored via Bundle.putString using {@link #BUNDLE_MESSAGE} as the key for the item. 
	 * @param message to send across processes.
	 * @return Parcelable Bundle
	 * @see Bundle#putString(String, String)
	 * @see Bundle#getString(String) 
	 */
	public static Parcelable setParcelableMessage(String message){
		Bundle bundle = new Bundle();
		bundle.putString(BUNDLE_MESSAGE, message);
		bundle.setClassLoader(Bundle.class.getClassLoader());
		return bundle;
	}
	
	/**
	 * Create a Parcelable Bundle containing a char[] for transport to the test package engine.
	 * The char[] message is stored via Bundle.putCharArray using {@link #BUNDLE_PROPERTIES} as the key for the item. 
	 * The original intent here is to store Java Properties in the Bundle that were serialized via 
	 * the Java Properties.store method.<br>
	 * NOTE: This has not yet been tested!
	 * @param char[] (Properties) to send across processes.
	 * @return Parcelable Bundle
	 * @see Bundle#putCharArray(String, char[])
	 * @see Bundle#getCharArray(String)
	 */
	public static Parcelable setParcelableProps(char[] bytes){
		Bundle bundle = new Bundle();
		bundle.putCharArray(BUNDLE_PROPERTIES, bytes);
		bundle.setClassLoader(Bundle.class.getClassLoader());
		return bundle;
	}

	/**
	 * Create a Parcelable Bundle containing 3 parameters: String, int, int for transport from the test package engine.<br>
	 * It will be used by {@link Message#setData(Bundle)} for sending a part of whole message from engine.<br>
	 * The String message is stored via Bundle.putCharArray using {@link #BUNDLE_SMALL_PARCEL_ID} as the key for the item.<br> 
	 * The int message is stored via Bundle.putCharArray using {@link #BUNDLE_SMALL_PARCEL_INDEX} as the key for the item.<br>
	 * The int message is stored via Bundle.putCharArray using {@link #BUNDLE_SMALL_PARCEL_TOTALNUMBER} as the key for the item.<br> 
	 * 
	 * @param ID String, the message's ID.
	 * @param index int, the index of this parcel of the whole message.
	 * @param totalNumber int, indicate the total number of parcels will be sent for a whole message..
	 * @return Parcelable Bundle
	 * @see Bundle#putCharArray(String, char[])
	 * @see Bundle#getCharArray(String)
	 * @see #getParcelableIDFromSmallParcel(Parcelable)
	 * @see #getParcelableIndexFromSmallParcel(Parcelable)
	 * @see #getParcelableTotalNumberFromSmallParcel(Parcelable)
	 * @see #isSmallParcelOfWholeMessage(Bundle)
	 */
	public static Bundle setBundleOfSmallParcel(String ID, int index, int totalNumber){
		Bundle bundle = new Bundle();
		bundle.putString(BUNDLE_SMALL_PARCEL_ID, ID);
		bundle.putInt(BUNDLE_SMALL_PARCEL_INDEX, index);
		bundle.putInt(BUNDLE_SMALL_PARCEL_TOTALNUMBER, totalNumber);
		bundle.setClassLoader(Bundle.class.getClassLoader());
		return bundle;
	}
	
	public static Bundle addParcelableResentParcelFromSmallParcel(Parcelable parcelable, boolean isResent){
		Bundle bundle = (Bundle)parcelable;
		bundle.putBoolean(BUNDLE_SMALL_RESENT_PARCEL, isResent);
		return bundle;
	}
	
	/**
	 * Test is the bundle contains the information of small parcel of a whole message.<br>
	 * 
	 * @param dataBundle, Bundle, get from the data property of a Message object.
	 * @return boolean, true, if the dataBundle contains the information of small parcel.
	 * @see #setBundleOfSmallParcel(String, int, int)
	 * @see #getParcelableIDFromSmallParcel(Parcelable)
	 * @see #getParcelableIndexFromSmallParcel(Parcelable)
	 * @see #getParcelableTotalNumberFromSmallParcel(Parcelable)
	 */
	public static boolean isSmallParcelOfWholeMessage(Bundle dataBundle){
		return (dataBundle!=null &&
				dataBundle.containsKey(BUNDLE_SMALL_PARCEL_ID) &&
				dataBundle.containsKey(BUNDLE_SMALL_PARCEL_INDEX) &&
				dataBundle.containsKey(BUNDLE_SMALL_PARCEL_TOTALNUMBER)
				);
	}
	
	/**
	 * Extract the String message received from the test package engine.
	 * The String message is retrieved via Bundle.getString using {@link #BUNDLE_MESSAGE} as the key for the item. 
	 * @param Parcelable Bundle received from the TCP Messenger Service.
	 * @return String message, if present
	 * @see Bundle#putString(String, String)
	 * @see Bundle#getString(String) 
	 */
	public static String getParcelableMessage(Parcelable parcelable){
		Bundle bundle = (Bundle)parcelable;
		return bundle.getString(BUNDLE_MESSAGE);
	}
	
	/**
	 * Extract a char[] received from the test package engine.
	 * The char[] message is extracted via Bundle.getCharArray using {@link #BUNDLE_PROPERTIES} as the key for the item. 
	 * The original intent here is to retrieve Java Properties stored as a char[] suitable for deserialization 
	 * via the Java Properties.load method.<br>
	 * NOTE: This has not yet been tested!
	 * @param Parcelable Bundle received from the test package engine
	 * @return char[] suitable for Java Properties.load
	 * @see Bundle#putCharArray(String, char[])
	 * @see Bundle#getCharArray(String)
	 */
	public static char[] getParcelableProps(Parcelable parcelable){
		Bundle bundle = (Bundle)parcelable;
		return bundle.getCharArray(BUNDLE_PROPERTIES);
	}
	
	/**
	 * Extract a String received from the test package engine.
	 * The String message is extracted via Bundle.getCharArray using {@link #BUNDLE_SMALL_PARCEL_ID} as the key for the item. 
	 * 
	 * @param Parcelable Bundle received from the test package engine
	 * @return String represents the ID of a message, generated at the engine side.
	 * @see Bundle#putCharArray(String, char[])
	 * @see Bundle#getCharArray(String)
	 * @see #setBundleOfSmallParcel(String, int, int)
	 */	
	public static String getParcelableIDFromSmallParcel(Parcelable parcelable){
		Bundle bundle = (Bundle)parcelable;
		return bundle.getString(BUNDLE_SMALL_PARCEL_ID);
	}
	
	/**
	 * Extract a int received from the test package engine.
	 * The int message is extracted via Bundle.getCharArray using {@link #BUNDLE_SMALL_PARCEL_INDEX} as the key for the item. 
	 * 
	 * @param Parcelable Bundle received from the test package engine
	 * @return int represents the index of a parcel of a whole message, generated at the engine side.
	 * @see Bundle#putCharArray(String, char[])
	 * @see Bundle#getCharArray(String)
	 * @see #setBundleOfSmallParcel(String, int, int)
	 */		
	public static int getParcelableIndexFromSmallParcel(Parcelable parcelable){
		Bundle bundle = (Bundle)parcelable;
		return bundle.getInt(BUNDLE_SMALL_PARCEL_INDEX);
	}
	
	/**
	 * Extract an int received from the test package engine.
	 * The int message is extracted via Bundle.getCharArray using {@link #BUNDLE_SMALL_PARCEL_TOTALNUMBER} as the key for the item. 
	 * 
	 * @param Parcelable Bundle received from the test package engine
	 * @return int the total number of parcels to be sent for one message, generated at the engine side.
	 * @see Bundle#putCharArray(String, char[])
	 * @see Bundle#getCharArray(String)
	 * @see #setBundleOfSmallParcel(String, int, int)
	 */		
	public static int getParcelableTotalNumberFromSmallParcel(Parcelable parcelable){
		Bundle bundle = (Bundle)parcelable;
		return bundle.getInt(BUNDLE_SMALL_PARCEL_TOTALNUMBER);
	}
	
	public static boolean getParcelableResentParcelFromSmallParcel(Parcelable parcelable){
		Bundle bundle = (Bundle)parcelable;
		return bundle.getBoolean(BUNDLE_SMALL_RESENT_PARCEL);
	}
	
	/**
	 * Concatenate two Parcelable Bundles containing String to form one Parcelable.<br>
	 * The String message is stored via Bundle.putCharArray using {@link #BUNDLE_MESSAGE} as the key for the item.<br>
	 *  
	 * @param one 		Parcelable Bundle, containing String message
	 * @param another 	Parcelable Bundle, containing String message
	 * @return Parcelable Bundle, including Parcelable one and another.
	 * @see Bundle#putCharArray(String, char[])
	 * @see Bundle#getCharArray(String)
	 */	
	public static Parcelable assembleParcelMessage(Parcelable one, Parcelable another){
		Bundle bundle = new Bundle();
		String message = "";
		String tmp = null;

		if(one!=null){
			tmp = ((Bundle)one).getString(BUNDLE_MESSAGE);
			if(tmp!=null) message += tmp;
		}
		if(another!=null){
			tmp = ((Bundle)another).getString(BUNDLE_MESSAGE);
			if(tmp!=null) message += tmp;
		}
		
		bundle.putString(BUNDLE_MESSAGE, message);
		bundle.setClassLoader(Bundle.class.getClassLoader());
		return bundle;
	}
	
	/**
	 * Concatenate two Parcelable Bundles containing char[] to form one Parcelable.<br>
	 * The char[] message is stored via Bundle.putCharArray using {@link #BUNDLE_PROPERTIES} as the key for the item.<br>
	 *  
	 * @param one 		Parcelable Bundle, containing char[] message
	 * @param another 	Parcelable Bundle, containing char[] message
	 * @return Parcelable Bundle, including Parcelable one and another.
	 * @see Bundle#putCharArray(String, char[])
	 * @see Bundle#getCharArray(String)
	 */	
	public static Parcelable assembleParcelProps(Parcelable one, Parcelable another){
		Bundle bundle = new Bundle();
		char[] characters = null;
		char[] characters1 = null;
		char[] characters2 = null;
		int length = 0;
		
		if(one!=null){
			characters1 = ((Bundle)one).getCharArray(BUNDLE_PROPERTIES);
			if(characters1!=null) length += characters1.length;
		}
		if(another!=null){
			characters2 = ((Bundle)another).getCharArray(BUNDLE_PROPERTIES);
			if(characters2!=null) length += characters2.length;
		}

		characters = new char[length];
		
		if(characters1!=null && characters2!=null){
			System.arraycopy(characters1, 0, characters, 0, characters1.length);
			System.arraycopy(characters2, 0, characters, characters1.length, characters2.length);
		}else if(characters1!=null && characters2==null){
			System.arraycopy(characters1, 0, characters, 0, characters1.length);
		}else if(characters1==null && characters2!=null){
			System.arraycopy(characters2, 0, characters, 0, characters2.length);
		}
		
		bundle.putCharArray(BUNDLE_PROPERTIES, characters);
		bundle.setClassLoader(Bundle.class.getClassLoader());
		return bundle;
	}

}
