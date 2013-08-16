package org.safs.android.messenger;

import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.safs.android.messenger.client.MessageResult;
import org.safs.sockets.DebugListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.util.Log;

/** 
 * This class contains methods to handle message of big size.<br>
 * At the message-sender side, the message will be divided to multiples parcels. These multiples<br>
 * parcels contain an ID to indicate to which message they belong; They contains an index to<br>
 * indicate their order; They also contains a filed to indicate if it is the last parcel.<br>
 * 
 * At the message-receiver side, the multiple parcels will be assembled to one message.<br>
 * 
 * <p>Message-Sender:<br>
 * {@link org.safs.android.messenger.MessengerService}<br>
 * {@link org.safs.android.messenger.client.MessengerRunner}<br>
 * 
 * <p>Message-Receiver:<br>
 * {@link org.safs.android.messenger.MessengerHandler}<br>
 * {@link org.safs.android.messenger.client.MessengerHandler}<br>
 * 
 * @see org.safs.android.messenger.MessengerService
 * @see org.safs.android.messenger.client.MessengerRunner
 * @see org.safs.android.messenger.MessengerHandler
 * @see org.safs.android.messenger.client.MessengerHandler
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since	APR 25, 2013
 * 
 */
public abstract class MultipleParcelsHandler extends Handler{
	public String TAG = getClass().getName();
	private DebugListener debugListener = null;
	private MultipleParcelListener multipleParcelListener = null;
	
	public static final int INITIAL_MESSAGE_CACHE_SIZE = 10;
	public static final int INITIAL_PARCEL_CACHE_SIZE = 5;
	/** time to wait for acknowledgment of parcel in milliseconds*/
	public static final int TIMEOUT_WAIT_FOR_PARCEL_ACKNOWLEDGMENT = 5000;
	
	public MultipleParcelsHandler(Looper looper, MultipleParcelListener multipleParcelListener) {
		super(looper);
		this.multipleParcelListener = multipleParcelListener;
		if(multipleParcelListener instanceof DebugListener){
			this.debugListener = (DebugListener) multipleParcelListener;
		}
	}
	
	public MultipleParcelsHandler(MultipleParcelListener multipleParcelListener) {
		super();
		this.multipleParcelListener = multipleParcelListener;
		if(multipleParcelListener instanceof DebugListener){
			this.debugListener = (DebugListener) multipleParcelListener;
		}
	}
	
	protected void debug(String message){
		if(debugListener==null){
			Log.d(TAG, message);
		}else{
			debugListener.onReceiveDebug(message);
		}
	}
	
	/* ============================================================================================
	 * ===                                Message-Receiver Side                                 ===
	 * === Following fields and methods are used  to handle and assemble small parcels          ===
	 * === to create a whole message.                                                           ===
	 * === Message sender should never touch them!!!                                            ===
	 * ============================================================================================*/	
	private static Hashtable<String, ParcelBuffer> messageReceivedCache = new Hashtable<String, ParcelBuffer>(INITIAL_MESSAGE_CACHE_SIZE);

	private String __last_handled_message_id = null;
	/**
	 * This method will accumulate small parcels of one message, store them into<br>
	 * a cache for future assembly.<br>
	 * 
	 * Message.obj will hold the final result of message:<br>
	 * If the message is not divided into parcels, this method will do no thing.<br>
	 * If the message is divided into parcels, this method will store them into<br>
	 * a cache {@link #messageReceivedCache} and assemble them and assign it to <br>
	 * parameter msg.obj when the last parcel arrives. Each time a parcel arrives<br>
	 * and be stored in the cache {@link #messageReceivedCache}, an acknowledgment<br>
	 * will be sent back to the message-sender.<br>
	 * 
	 * The message is sent out by method {@link #sendMessageAsMultipleParcels(Messenger, Message, Object)}<br>
	 * at the message-sender side.<br>
	 * 
	 * @param msg Message
	 * @return Boolean, if we have finished handling all the parcels of a whole message.
	 * 
	 * @see #sendMessageAsMultipleParcels(Messenger, Message, Object)
	 */
	public synchronized boolean assembleSmallParcels(Message msg) throws Exception{
		Bundle dataBundle = msg.getData();
		String messageID = null;
		int messageIndex = -1;
		int totalNumber = -1;
		ParcelBuffer parcelBuffer = null;
		SmallParcel smallParcel = null;
		boolean finished = false;
		
//		debug("dataBundle is "+dataBundle);
		if(MessageUtil.isSmallParcelOfWholeMessage(dataBundle)){
			//The message is too big so that it is divided into small parcels, we should
			//accumulate them into a cache for future assembly
			messageID = MessageUtil.getParcelableIDFromSmallParcel(dataBundle);
			messageIndex = MessageUtil.getParcelableIndexFromSmallParcel(dataBundle);
			totalNumber = MessageUtil.getParcelableTotalNumberFromSmallParcel(dataBundle);
			debug("handling Small Parcel, messageID="+messageID+"; messageIndex="+messageIndex+"; totalNumber="+totalNumber);
			
			if(messageID==null) return finished;
			
			if(messageID.equals(__last_handled_message_id) &&
			   MessageUtil.getParcelableResentParcelFromSmallParcel(dataBundle)){
				//If the message has been handled and its parcels arrive again, ignore it.
				return finished;
			}
			
			smallParcel = new SmallParcel(msg, messageIndex);
			
			parcelBuffer = messageReceivedCache.get(messageID);
			if(parcelBuffer==null){
				parcelBuffer = new ParcelBuffer(smallParcel, totalNumber);
				messageReceivedCache.put(messageID, parcelBuffer);
			}else{
				parcelBuffer.addParcel(smallParcel);
			}
			
			//Send acknowledgment that one parcel has been received.
			sendAcknowledgment(messageID, messageIndex);
			
			if(parcelBuffer.receivedAllParcels()){
				debug("Trying to assemble message, messageID="+messageID);
				
				//Assign the assembled data to input parameter msg.obj
				msg.obj = parcelBuffer.assembleParcels().object;
				debug("assembly finished for message, messageID="+messageID);
				
				messageReceivedCache.remove(messageID);
				__last_handled_message_id = messageID;
				finished = true;
			}
		}else{
			//else, nothing needs to do, just send back the result.
			finished = true;
		}
		
		return finished;
	}
	
	/**
	 * This class is used to wrap a small parcel of a whole message.
	 *
	 */
	protected class SmallParcel implements Comparable<SmallParcel>{
		/** The order of parcel in one whole message.*/
		private int index = -1;
		/** The type of parcel of one whole message.*/
		private int msgType = -1;
		/** The content of parcel, which is part of one whole message.*/
		private Parcelable object = null;//Parcelable containing String or char[] or other types
		
		public SmallParcel(){}
		public SmallParcel(Message message, int index){
			this.index = index;
			this.msgType = message.what;
			try{
				this.object = (Parcelable) message.obj;
			}catch(Exception e){
				debug("Can't set a part of message, due to "+e.getClass().getName()+":"+e.getMessage());
			}
		}
		
		public int compareTo(SmallParcel another) {
			//Will be sorted according orderID
			return index-another.index;
		}

		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}

		public int getMsgType() {
			return msgType;
		}
		public void setMsgType(int msgType) {
			this.msgType = msgType;
		}

		public Object getObject() {
			return object;
		}
		public void setObject(Parcelable object) {
			this.object = object;
		}
		
		public Object getRealObject() {
			if(object==null) return null;
			
			if(isStringType()){
				return MessageUtil.getParcelableMessage(object);
			}else if(isCharArrayType()){
				return MessageUtil.getParcelableProps(object);
			}else{
				return object;
			}
		}
		
		public boolean isStringType(){
			return (MessageUtil.ID_ENGINE_RESULT==msgType ||
					MessageUtil.ID_ENGINE_DEBUG==msgType ||
					MessageUtil.ID_ENGINE_EXCEPTION==msgType ||
					MessageUtil.ID_ENGINE_MESSAGE==msgType ||
					MessageUtil.ID_ENGINE_DISPATCHFILE==msgType
					);
		}
		public boolean isCharArrayType(){
			return (MessageUtil.ID_ENGINE_RESULTPROPS==msgType ||
					MessageUtil.ID_ENGINE_DISPATCHPROPS==msgType
					);
		}
		
		/**
		 * This method is used to concatenate the contents of two parcels.<br>
		 * It will append the content provided by parameter 'another' to this<br> 
		 * SmallParcel object's content.<br>
		 * 
		 * @param another	SmallParcel, whose content will be added to this parcel.
		 * @return SmallParcel, contains the concatenated content.
		 */
		public SmallParcel add(SmallParcel another) throws Exception{
			//If this parcel has no type, assign one for it.
			if(this.msgType==-1){
				this.msgType = another.msgType;
			}
			
			//We need to add parcel of same type, and in order
			if(this.msgType==another.msgType &&
			   another.index>this.index){
				this.index = another.index;
				if(isStringType()){
					object = MessageUtil.assembleParcelMessage(this.object, another.object);
				}else if(isCharArrayType()){
					object = MessageUtil.assembleParcelProps(this.object, another.object);
				}else{
					debug("The message type is "+this.msgType+", we don't know how to assemble.");
				}
			}else{
				//else we ignore the parcel.
				debug("wrong parcel to add, or the order is wrong.");
			}
			
			return this;
		}
	}
	
	protected class ParcelBuffer{
		private List<SmallParcel> parcels = new ArrayList<SmallParcel>(INITIAL_PARCEL_CACHE_SIZE);
		private int totalParcelNumber = 0;
		
		public ParcelBuffer(SmallParcel parcel, int totalNumber){
			addParcel(parcel);
			totalParcelNumber = totalNumber;
		}
		
		public synchronized int addParcel(SmallParcel parcel){
			parcels.add(parcel);
			return parcels.size();
		}
		
		public synchronized int getTotalParcelNumber(){
			return parcels.size();
		}
		
		/**
		 * We need to sort all the small parcels according to parcel's index<br>
		 * before assembling them. This method is called in {@link #assembleParcels()}<br>
		 * 
		 * @see #assembleParcels()
		 */
		public synchronized void sortBuffer(){
			Collections.sort(parcels);
		}
		/**
		 * @return boolean, true if all the parcels have been received.
		 */
		public synchronized boolean receivedAllParcels(){
			return parcels.size()==totalParcelNumber;
		}
		/**
		 * [Note] This method should NOT be called if {@link #receivedAllParcels()} return false.<br>
		 * Assemble all parcels stored in this buffer.<br>
		 * 
		 * @return {@link SmallParcel}, the assembled parcel containing content of all parcels.
		 * @see #receivedAllParcels()
		 */
		public synchronized SmallParcel assembleParcels() throws Exception{
			SmallParcel wholeMessage = new SmallParcel();
			
			if(parcels.size()==0){
				throw new Exception("There is no parcels in the parcel buffer!!!");
			}
			
			//To keep the received parcels in order.
			sortBuffer();
			
			for(int i=0;i<parcels.size();i++){
//				debug("assembling part "+parcels.get(i).index);
				wholeMessage.add(parcels.get(i));
			}
			return wholeMessage;
		}
	}
	
	protected void sendAcknowledgment(String messageID){
		multipleParcelListener.onAllParcelsHaveBeenHandled(messageID);
	}
	protected void sendAcknowledgment(String messageID, int index){
		multipleParcelListener.onParcelHasBeenHandled(messageID, index);
	}
	
	/**
	 * The subclass of this class must implement this method to handle message.<br>
	 */
	abstract protected void handleWholeMessage(Message msg);
	
	
	
	/* ============================================================================================
	 * ===                                Message-Sender side                                   ===
	 * === Following fields and methods are used to generate multiple parcels of one message    ===
	 * === Message receiver should never touch them!!!                                          ===
	 * ============================================================================================
	 */
	private static String __last_unique_key = "";
	/**
	 * Routine is used to create a unique ID String key that can be used by "message-sender"
	 * to identify different small parcels of the same message.<br>
	 * This method is thread-safe: it guarantees that multiple threads can get unique ID.<br>
	 * 
	 * @return unique String suitable to be the key for the item.
	 */
	protected String getUniqueKey() {
		String timestamp = "";
		synchronized (__last_unique_key) {
			do {timestamp = UUID.randomUUID().toString();
			} while (timestamp.equals(__last_unique_key));
			__last_unique_key = timestamp;
		}
		return timestamp;
	}
	/**
	 * This cache is used to store the message (small parcels) that we have sent out.<br>
	 * The key is the messageID, the value is a set of small parcels (part of a message).<br>
	 * If some parcels are lost, we can get them and send again.<br>
	 * This cache needs to be cleared when all the parcels arrive at the receiver side.<br> 
	 */
	private static Hashtable<String, Hashtable<Integer, Message>> messageSendedCache = new Hashtable<String, Hashtable<Integer, Message>>(INITIAL_MESSAGE_CACHE_SIZE);
	
	/**
	 * Store parcel (part of a message) into a cache according to the message ID.<br>
	 * If the parcel is not a part of a whole message, it will not be stored in the cache<br>
	 * We use {@link MessageUtil#isSmallParcelOfWholeMessage(Bundle)} to test if this<br>
	 * parcel is a part of message.<br>
	 * 
	 * @param parcel, Message, one parcel of a whole message, 
	 *                         it should contain a data(ID,index,totalNumber)
	 * @return String, the messageID of the parcel stored. 
	 */
	protected synchronized String storeParcelMessage(Message parcel){
		String messageID = null;
		//Verify that this parcel's has correct format as a part of whole message
		if(!MessageUtil.isSmallParcelOfWholeMessage(parcel.getData())){
			debug("This parcel is not part of message! We will not store it in the cache.");
			return messageID;
		}
		
		messageID = MessageUtil.getParcelableIDFromSmallParcel(parcel.getData());
		Hashtable<Integer, Message> parcels = messageSendedCache.get(messageID);
		if(parcels==null){
			parcels = new Hashtable<Integer, Message>(INITIAL_PARCEL_CACHE_SIZE);
			messageSendedCache.put(messageID, parcels);
		}
		
		int index = MessageUtil.getParcelableIndexFromSmallParcel(parcel.getData());
		parcels.put(Integer.valueOf(index), parcel);
		debug("Stored parcel, messageID="+messageID+", index="+index);
		return messageID;
	}
	/**
	 * Clear the cache {@link #messageSendedCache}
	 */
	protected synchronized void clearParcelMessage(String messageID){
		debug("Clearing message, messageID="+messageID);
		messageSendedCache.remove(messageID);
		notifyAll();
		debug("notifying with 'all parcels arrived' messageID="+messageID);
	}
	/**
	 * Clear one parcel of a message from the cache {@link #messageSendedCache}
	 */
	protected synchronized void clearParcelMessage(String messageID,  int index){
		debug("Clearing parcel, messageID="+messageID+", index="+index);
		Hashtable<Integer, Message> parcels = messageSendedCache.get(messageID);
		if(parcels!=null) parcels.remove(Integer.valueOf(index));
		notifyAll();
		debug("notifying with 'one parcel arrived' messageID="+messageID+" index="+index);
	}
	/**
	 * According to messageId to get a set of parcels of a whole messagefrom<br>
	 * the cache {@link #messageSendedCache}.<br>
	 * 
	 * @param messageID, String, represent a message
	 * @return Message[], an array of parcels of a message.
	 */
	protected synchronized Message[] getParcelMessage(String messageID){
		Hashtable<Integer, Message> parcels = messageSendedCache.get(messageID);
		Message[] messages = null;
		if(parcels!=null) messages = parcels.values().toArray(new Message[0]);
		return messages;
	}
	/**
	 * According to messageId and index to get one parcel of a whole message from<br>
	 * the cache {@link #messageSendedCache}.<br>
	 * 
	 * @param messageID, String, represent a message.
	 * @param index, int, the index of the parcel.
	 * @return Message, one parcel of a message.
	 */
	protected synchronized Message getParcelMessage(String messageID, int index){
		Hashtable<Integer, Message> parcels = messageSendedCache.get(messageID);
		Message message = null;
		if(parcels!=null){
			message = parcels.get(Integer.valueOf(index));
		}
		return message;
	}
	/**
	 * It is used to test if the cache {@link #messageSendedCache} still contains a<br>
	 * certain message indicated by parameter messageID.<br>
	 * 
	 * @see #clearParcelMessage(String)
	 * @see #clearParcelMessage(String, int)
	 */
	protected synchronized boolean containMessageWithoutAck(String messageID){
		if(messageID==null) return false;
		Hashtable<Integer, Message> parcels = messageSendedCache.get(messageID);
		return (parcels!=null && parcels.size()>0);
	}
	
	/**
	 * Calculate the total number of parcels the message will be divided.<br>
	 */
	protected int getTotalNumberOfParcels(int dataLength, int parcelLength){
		if((dataLength%parcelLength)==0) return dataLength/parcelLength;
		return dataLength/parcelLength +1;
	}
	
	/**
	 * Divide message's content into small parcels if the message content size is too large.<br>
	 * If the content size is bigger than {@link MessageUtil#MAX_TRANSFER_BYTE_SIZE}, it will<br>
	 * be divided into small parcels, wrapped to a Message object and put into a List to return.<br>
	 * If the content size is smaller than {@link MessageUtil#MAX_TRANSFER_BYTE_SIZE}, it will<br>
	 * be set to the original Message object and put into a List to return.<br>
	 * 
	 * @param msg	Message, the original message to send. It contains 'what' and 'replyTo'.
	 * @param props	Properties, the real content to send through Message.
	 * @return List of Message, to send. Never null and contain at the least one Message.
	 * 
	 * @see MessageUtil
	 */
	protected List<Message> divideMessageIntoSmallPieces(Message msg, Properties props) throws Exception{
		CharArrayWriter chars = new CharArrayWriter();
		props.store(chars, "ResultProperties");
		char[] buffer = chars.toCharArray();
		
		return divideMessageIntoSmallPieces(msg, buffer);
	}
	
	/**
	 * Divide message's content into small parcels if the message content size is too large.<br>
	 * If the content size is bigger than {@link MessageUtil#MAX_TRANSFER_BYTE_SIZE}, it will<br>
	 * be divided into small parcels, wrapped to a Message object and put into a List to return.<br>
	 * If the content size is smaller than {@link MessageUtil#MAX_TRANSFER_BYTE_SIZE}, it will<br>
	 * be set to the original Message object and put into a List to return.<br>
	 * 
	 * @param msg	Message, the original message to send. It contains 'what' and 'replyTo'.
	 * @param buffer	char[], the real content to send through Message.
	 * @return List of Message, to send. Never null and contain at the least one Message.
	 * 
	 * @see MessageUtil
	 */	
	protected List<Message> divideMessageIntoSmallPieces(Message msg, char[] buffer) throws Exception{
		List<Message> messages = new ArrayList<Message>();
		
		if(buffer==null){
			messages.add(msg);
		}else{
			debug("Trying to send message of size "+buffer.length);
			if(buffer.length>MessageUtil.MAX_TRANSFER_BYTE_SIZE){
				//Divide the message to multiple small parcels
				//Generate an ID for those small parcels so that they can be identified as one same message.
				String messageID = getUniqueKey();
				debug("The message will be sent as multiple parcels, belonging to same message, messageID="+messageID);
				int totalParcelsNumber = getTotalNumberOfParcels(buffer.length,MessageUtil.SMALL_TRANSFER_BYTE_SIZE);
				
				CharArrayWriter smallParcel = null;
				Message smallParcelMessage = null;
				int count = 0;
				int len = MessageUtil.SMALL_TRANSFER_BYTE_SIZE;
				for(int i=0;i<buffer.length; ){
					//Obtain a new Message to carry a part of content of original message
					//we need to obtain from the original one to keep the Message#what,Message#replyTo
					smallParcelMessage = Message.obtain(msg);
					
					//Let the new Message to carry part of the chars
					smallParcel = new CharArrayWriter(MessageUtil.SMALL_TRANSFER_BYTE_SIZE);
					if(i+MessageUtil.SMALL_TRANSFER_BYTE_SIZE>=buffer.length){
						//This is the last part of whole message
						len = buffer.length - i;
					}else{
						len = MessageUtil.SMALL_TRANSFER_BYTE_SIZE;
					}
					smallParcel.write(buffer, i, len);
					i += len;
					smallParcelMessage.obj = MessageUtil.setParcelableProps(smallParcel.toCharArray());
					
					//Set extra information to data of the new Message, so that the message receiver can
					//assemble the different small parcels to create a whole message.
					smallParcelMessage.setData(MessageUtil.setBundleOfSmallParcel(messageID, count++, totalParcelsNumber));
					//debug("Adding small parcel to List for message messageID="+messageID+"; parcelCount="+count+"; size="+len);
					messages.add(smallParcelMessage);
				}
			}else{
				debug("The message will be sent as one parcel.");
				msg.obj = MessageUtil.setParcelableProps(buffer);
				messages.add(msg);
			}
		}
		
		return messages;
	}
	
	/**
	 * Divide message's content into small parcels if the message content size is too large.<br>
	 * If the content size is bigger than {@link MessageUtil#MAX_TRANSFER_BYTE_SIZE}, it will<br>
	 * be divided into small parcels, wrapped to a Message object and put into a List to return.<br>
	 * If the content size is smaller than {@link MessageUtil#MAX_TRANSFER_BYTE_SIZE}, it will<br>
	 * be set to the original Message object and put into a List to return.<br>
	 * 
	 * @param msg	Message, the original message to send. It contains 'what' and 'replyTo'.
	 * @param message	String, the real content to send through Message.
	 * @return List of Message, to send. Never null and contain at the least one Message.
	 * 
	 * @see MessageUtil
	 */	
	protected List<Message> divideMessageIntoSmallPieces(Message msg, String message) throws Exception{
		List<Message> messages = new ArrayList<Message>();
		
		if(message==null){
			messages.add(msg);
		}else{
			int messageLength = message.length();
			
			debug("Trying to send message of size "+messageLength);
			if(messageLength > MessageUtil.MAX_TRANSFER_BYTE_SIZE){
				//Divide the message to multiple small parcels
				//Generate an ID for those small parcels so that they can be identified as one same message.
				String messageID = getUniqueKey();
				debug("The message will be sent as multiple parcels, belonging to same message, messageID="+messageID);
				int totalParcelsNumber = getTotalNumberOfParcels(messageLength,MessageUtil.SMALL_TRANSFER_BYTE_SIZE);
				
				String smallParcel = null;
				Message smallParcelMessage = null;
				int count = 0;
				int len = MessageUtil.SMALL_TRANSFER_BYTE_SIZE;
				for(int i=0;i<messageLength; ){
					//Obtain a new Message to carry a part of content of original message
					//we need to obtain from the original one to keep the Message#what,Message#replyTo
					smallParcelMessage = Message.obtain(msg);
					
					//Let the new Message to carry part of the chars
					if(i+MessageUtil.SMALL_TRANSFER_BYTE_SIZE>=messageLength){
						//This is the last part of whole message
						len = messageLength - i;
					}else{
						len = MessageUtil.SMALL_TRANSFER_BYTE_SIZE;
					}
					smallParcel = message.substring(i, i+len);
					i += len;
					smallParcelMessage.obj = MessageUtil.setParcelableMessage(smallParcel);
					
					//Set extra information to data of the new Message, so that the message receiver can
					//assemble the different small parcels to create a whole message.
					smallParcelMessage.setData(MessageUtil.setBundleOfSmallParcel(messageID, count++, totalParcelsNumber));
					//debug("Adding small parcel to List for message messageID="+messageID+"; parcelCount="+count+"; size="+len);
					messages.add(smallParcelMessage);
				}
			}else{
				debug("The message will be sent as one parcel.");
				msg.obj = MessageUtil.setParcelableMessage(message);
				messages.add(msg);
			}
		}
		
		return messages;
	}
	
    /**
     * If the messag's size is bigger than {@link MessageUtil#MAX_TRANSFER_BYTE_SIZE},<br>
     * Send the message as multiple parcels<br>
     * and wait for acknowledgment of the each parcel, if no acknowledgment arrive within<br>
     * timeout {@value #TIMEOUT_WAIT_FOR_PARCEL_ACKNOWLEDGMENT}, all the parcels in cache<br>
     * {@link #messageSendedCache} of a message will be re-sent out.<br>
     * 
     * At the message-receiver side, the parcels will be assembled by method {@link #assembleSmallParcels(Message)}<br>
     * 
     * @param msg, Message, a wrapper for the object.
     * @param message, Object, the object to send
     * @return boolean, true if the message is sent out.
     * 
     * @see #sendServiceResult(Properties)
     * @see #sendServiceResult(MessageResult)
     * @see #assembleSmallParcels(Message)
     */
    public boolean sendMessageAsMultipleParcels(Messenger mService, Message msg, Object object){
		try{
			List<Message> messages = null;
			if(object instanceof Properties){
				messages = divideMessageIntoSmallPieces(msg, (Properties) object);
			}else if(object instanceof String){
				messages = divideMessageIntoSmallPieces(msg, (String) object);
			}else if(object instanceof char[]){
				messages = divideMessageIntoSmallPieces(msg, (char[]) object);
			}else{
				//If we don't know the object's type, we don't know how to divide it.
				//just simply assign it directly to msg.obj, we need to implement new code
				//to handle it.
				debug("object's type is '"+object.getClass().getName()+"', NEED new code to handle.");
				messages = new ArrayList<Message>();
				//TODO Make sure to convert the object to a parcelable object before assign it to msg.obj
//				msg.obj = MessageUtil.setParcelableMessage(object);
				messages.add(msg);
			}
			
			String messageID = null;
			//If there is only 1 message in list messages, that means we didn't send it as multiple parcels
			for(int i=0;i<messages.size();i++){
				//Before sent out the message, we store it into the cache
				messageID = storeParcelMessage(messages.get(i));
//				if(i!=1) mService.send(messages.get(i));//Don't send the 2nd parcel intentionally to test lost parcel.
				mService.send(messages.get(i));
			}
			
			//Wait for the acknowledgment of the 'sent-message'.
			//If we receive an acknowledgment, that's good.
			//If we didn't receive an acknowledgment within timeout, we will send all parcels
			//stored in cache for message (indicated by messageID)
			long time = 0;
			synchronized (this) {
				while(containMessageWithoutAck(messageID)){
					time = new Date().getTime();
					debug("waitting for acknowledgments...");
					wait(TIMEOUT_WAIT_FOR_PARCEL_ACKNOWLEDGMENT);
					if(new Date().getTime()>=(time+TIMEOUT_WAIT_FOR_PARCEL_ACKNOWLEDGMENT)){
						debug("notified by timeout, re-send parcels.");
						if(containMessageWithoutAck(messageID)){
							sendMessageOfLostParcel(mService, messageID);
						}
					}else{
						debug("notified by ack, no need to re-send parcels.");
					}
				}
			}
			
			return true;
		}catch(Exception x){
			debug("Failed to send to MessengerService due to "+org.safs.sockets.Message.getStackTrace(x));
		}
		return false;
    }
    
    /**
     * According to the messageID, we will get the message from cache {@link #messageSendedCache}<br>
     * and send it out.<br>
     * We should set the field {@link MessageUtil#BUNDLE_SMALL_RESENT_PARCEL} to true,<br>
     * so that 'message-receiver' will know it is a message being resent by 'sender'<br>
     * 
     * @see #getParcelMessage(String, int)
     * @see MessageUtil#addParcelableResentParcelFromSmallParcel(Parcelable, boolean)
     */
    public boolean sendMessageOfLostParcel(Messenger mService, String messageID){
		try{
			Message[] messages = getParcelMessage(messageID);
			if(messages !=null){
				for(int i=0;i<messages.length;i++){
					//We add a field to let 'receiver' to know it is a parcel being resent
					MessageUtil.addParcelableResentParcelFromSmallParcel(messages[i].getData(),true);					
					mService.send(messages[i]);
				}
				return true;
			}else{
				return false;
			}
		}catch(Exception x){
			debug("Failed to send to MessengerService due to "+org.safs.sockets.Message.getStackTrace(x));
		}
		return false;
    }
    
    /**
     * According to the messageID and index, we will get the message from cache {@link #messageSendedCache}<br>
     * and send it out.<br>
     * We should set the field {@link MessageUtil#BUNDLE_SMALL_RESENT_PARCEL} to true,<br>
     * so that 'message-receiver' will know it is a message being resent by 'sender'<br>
     * 
     * @see #getParcelMessage(String, int)
     */
    public boolean sendMessageOfLostParcel(Messenger mService, String messageID, int index){
		try{
			Message message = getParcelMessage(messageID, index);
			if(message !=null){
				//We add a field to let 'receiver' to know it is a parcel being resent
				MessageUtil.addParcelableResentParcelFromSmallParcel(message.getData(),true);
				mService.send(message);
				return true;
			}else{
				return false;
			}
		}catch(Exception x){
			debug("Failed to send to MessengerService due to "+org.safs.sockets.Message.getStackTrace(x));
		}
		return false;
    }

    
	/* ============================================================================================
	 * ===                                Message-Sender/Receiver side                          ===
	 * === Following fields and methods are both by sender and receiver.                        ===
	 * ============================================================================================
	 */
    /**
     * Override method from {@link Handler}
     * We will handle the small parcels of a whole message. Make sure no parcels
     * are lost and assemble them in order.
     */
	public void handleMessage(Message msg){
		
		//====================  Sender Side Begin =====================================
		//Sender get an 'acknowledgment of message sent back from receiver, we just handle it and return.
		switch (msg.what){
			case MessageUtil.ID_PARCEL_ACKNOWLEDGMENT:
				//Clear the parcel cache of sent-message at the sender side
				clearParcelMessage(MessageUtil.getParcelableMessage((Parcelable)msg.obj), msg.arg1);
				return;
			case MessageUtil.ID_ALL_PARCELS_ACKNOWLEDGMENT:
				//Clear the parcel cache of sent-message at the sender side
				clearParcelMessage(MessageUtil.getParcelableMessage((Parcelable)msg.obj));
				return;
		}
		//====================  Sender Side End =====================================
		
		//====================  Receiver Side Begin =====================================
		try {
			//If we haven't finished assemble all parcels, we don't send out.
			if(!assembleSmallParcels(msg)) return;
		} catch (Exception e) {
			debug(MessageUtil.getStackTrace(e));
		}
		
		handleWholeMessage(msg);
		
		//After handled message, send acknowledgment to tell the sender to clear the cache
		if(MessageUtil.isSmallParcelOfWholeMessage(msg.getData())){
			String messageID = MessageUtil.getParcelableIDFromSmallParcel(msg.getData());
			sendAcknowledgment(messageID);
		}
		//====================  Receiver Side End =====================================
	}
}
