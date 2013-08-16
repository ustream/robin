/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.messenger;

import java.util.Properties;

import org.safs.sockets.AbstractProtocolRunner;
import org.safs.sockets.Message;
import org.safs.sockets.SocketProtocol;

public class RemoteClientRunner extends AbstractProtocolRunner{

	public RemoteClientRunner() {
		super();
		setListenerName("MessengerService");		
		protocolserver.setLocalMode(false);
	}

	public RemoteClientRunner(RemoteClientListener listener) {
		this();
		addListener(listener);
	}

	protected void notifyMessage(String message){
		if(runnerlisteners.size() > 0){
			for(int i=0;i<runnerlisteners.size();i++)
			  try{((RemoteClientListener)runnerlisteners.get(i)).onReceiveMessage(message);}
			  catch(Exception x){}
		}else{
			debug("No Registered Listeners to receive custom Message: "+ message);
		}
	}
	
	protected void notifyDispatchFile(String filepath){
		if(runnerlisteners.size() > 0){
			for(int i=0;i<runnerlisteners.size();i++){
			  try{((RemoteClientListener)runnerlisteners.get(i)).onReceiveDispatchFile(filepath);}
			  catch(Exception x){}
			}
		}else{
			debug("No Registered Listeners to receive DispatchFile: "+ filepath);
		}
	}
	
	protected void notifyDispatchProps(char[] props){
		if(runnerlisteners.size() > 0){
			for(int i=0;i<runnerlisteners.size();i++)
			  try{((RemoteClientListener)runnerlisteners.get(i)).onReceiveDispatchProps(props);}
			  catch(Exception x){}
		}else{
			debug("No Registered Listeners to receive DispatchProps.");
		}
	}
	
	@Override
	public void processProtocolMessage(String message) {
		int sepindex;
		String lcprefix;
		if(message != null && message.length() > 0){
			sepindex = message.indexOf(Message.msg_sep);
			if(sepindex < 1){							
	   			if (message.equalsIgnoreCase(Message.msg_remoteshutdown)){ 
					onReceiveRemoteShutdown(SocketProtocol.STATUS_SHUTDOWN_NORMAL);
	   			}else 
	   			if (message.equalsIgnoreCase(Message.msg_shutdown)){ 
					onReceiveRemoteShutdown(SocketProtocol.STATUS_SHUTDOWN_REMOTE_CLIENT);
	   			}else {
	   				//otherwise treat as unknown message
	   				notifyMessage(message);
	   			}
			}else{ // prefix:content
				try{
					lcprefix = message.substring(0, sepindex).toLowerCase();
					if(lcprefix.equals(Message.msg_message)){
						notifyMessage(message.substring(sepindex+1));
					}else 
					if(lcprefix.equals(Message.msg_dispatchfile)){
						notifyDispatchFile(message.substring(sepindex+1));
					}else 
					if(lcprefix.equals(Message.msg_dispatchprops)){
						try{
							// avoid props.load blocking with alternate input mechanism
							notifyDispatchProps(message.substring(sepindex+1).toCharArray());
						}catch(Exception x){
							// the engine will not receive the dispatch
							// thus it will not respond with RUNNING
							debug("remote Properties load "+ x.getClass().getSimpleName()+", "+x.getMessage());
							notifyMessage(message);
						}									
					}else{
						// unknown message type
						notifyMessage(message);
					}							
				}catch(Exception x){// unknown or malformed message
					notifyMessage(message);
				}
			}
		} // message == null || length() == 0
	}

	/**
	 * Remote client does nothing with this.
	 * @return false.
	 */
	@Override
	public boolean sendShutdown() {
		return false;
	}

	/**
	 * Remote client does nothing with this.
	 * @return false.
	 */
	@Override
	public boolean sendDispatchProps(Properties trd) {
		return false;
	}

	/**
	 * Remote client does nothing with this.
	 * @return false.
	 */
	@Override
	public boolean sendDispatchFile(String filepath) {
		return false;
	}
}
