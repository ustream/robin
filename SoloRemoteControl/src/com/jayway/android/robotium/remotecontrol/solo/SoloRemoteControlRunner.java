/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.solo;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.InvalidObjectException;
import java.util.Properties;

import org.safs.sockets.AbstractProtocolRunner;
import org.safs.sockets.SocketProtocolListener;
import org.safs.sockets.android.DroidSocketProtocol;

public class SoloRemoteControlRunner extends AbstractProtocolRunner implements SocketProtocolListener{
	
	/**
	 * a convenient wrapper to {@link AbstractProtocolRunner#protocolserver}
	 * we should use this field instead of {@link AbstractProtocolRunner#protocolserver}
	 */
	public DroidSocketProtocol droidprotocolserver = null;
	
	private SoloRemoteControlRunner() {
		protocolserver = droidprotocolserver = new DroidSocketProtocol(this);
	}
	
	public SoloRemoteControlRunner(SocketProtocolListener listener){
		this();
		setListenerName(listener.getListenerName());
		addListener(listener);		
	}

	@Override
	public void processProtocolMessage(String message) {
		int sepindex = -1;
		String lcprefix;
		// route message to appropriate listener callbacks
		if(message != null && message.length() > 0){
			sepindex = message.indexOf(Message.msg_sep);
			if(sepindex < 1){
				if(message.equalsIgnoreCase(Message.msg_connected)){
					onReceiveConnection();
				}else 
				if(message.equalsIgnoreCase(Message.msg_ready)){
					onReceiveReady();
				}else 
				if(message.equalsIgnoreCase(Message.msg_running)){
					onReceiveRunning();
				}else{
					//unknown message
					onReceiveMessage(message);
				}
			}else{
				try{
					lcprefix = message.substring(0, sepindex).toLowerCase();
					if(lcprefix.equals(Message.msg_debug)){
						onReceiveDebug(message.substring(sepindex + 1));
					}else 
					if(lcprefix.equals(Message.msg_exception)){
						onReceiveException(message.substring(sepindex + 1));
					}else 
					if(lcprefix.equals(Message.msg_result)){
						String tempmsg = message.substring(sepindex +1);
						String rcmsg = null;
						String infomsg = null;
						// "-1"   or
						// "-1:statusinfo"
						sepindex = tempmsg.indexOf(Message.msg_sep);
						if(sepindex < 1){ 
							rcmsg = tempmsg.trim();
						}
						else{
							rcmsg = tempmsg.substring(0, sepindex);
							try{ infomsg = tempmsg.substring(sepindex+1);}
							catch(Exception x){infomsg="";}
						}
						int rc = -99;
						try{ rc = Integer.parseInt(rcmsg); }
						catch(NumberFormatException x){
							debug("Received immproperly formatted Result code: "+ rcmsg);
							onReceiveMessage(message);
						}
						onReceiveResult(rc, infomsg);
					}else 
					if(lcprefix.equals(Message.msg_resultprops)){
						Properties props = new Properties();
						try{
							props.load(new CharArrayReader(message.substring(sepindex + 1).toCharArray()));
							onReceiveResultProperties(props);
						}catch(Exception x){
							debug("Error loading results Properties: "+
									          x.getClass().getSimpleName()+", "+ x.getMessage());
							onReceiveMessage(message);
						}
					}else
					if(lcprefix.equals(Message.msg_remoteshutdown)){
						String tempmsg = message.substring(sepindex +1);
						int rc = -99;
						try{ rc = Integer.parseInt(tempmsg); }
						catch(NumberFormatException x){
							debug("Received immproperly formatted Remote Shutdown cause: "+ tempmsg);
							onReceiveMessage(message);
						}
						onReceiveRemoteShutdown(rc);
						//shutdownThread(); // ???
					}else
					if(lcprefix.equals(Message.msg_shutdown)){
						String tempmsg = message.substring(sepindex +1);
						int rc = -99;
						try{ rc = Integer.parseInt(tempmsg); }
						catch(NumberFormatException x){
							debug("Received immproperly formatted Shutdown cause: "+ tempmsg);
							onReceiveMessage(message);
						}
						onReceiveLocalShutdown(rc);
						//shutdownThread(); // ???
					}else
					if(lcprefix.equals(Message.msg_message)){
						onReceiveMessage(message.substring(sepindex +1));
					}else{
						// unknown message type
						onReceiveMessage(message);
					}							
				}catch(Exception x){// unknown or malformed message
					// unknown message type
					onReceiveMessage(message);
				}
			}
		}
	}
	
	public void onReceiveReady() {
		boolean sent = false;
		for(int n = 0; n < runnerlisteners.size(); n++){
			try{
				((SocketProtocolListener)runnerlisteners.get(n)).onReceiveReady();
				sent = true;
			}catch(ClassCastException e){}
		}
		if(!sent) System.out.println("Received a remote Ready signal.");
	}

	public void onReceiveRunning() {
		boolean sent = false;
		for(int n = 0; n < runnerlisteners.size(); n++){
			try{
				((SocketProtocolListener)runnerlisteners.get(n)).onReceiveRunning();
				sent = true;
			}catch(ClassCastException e){}
		}
		if(!sent) System.out.println("Received a remote Running signal.");
	}

	public void onReceiveResult(int rc, String info) {
		boolean sent = false;
		for(int n = 0; n < runnerlisteners.size(); n++){
			try{
				((SocketProtocolListener)runnerlisteners.get(n)).onReceiveResult(rc, info);
				sent = true;
			}catch(ClassCastException e){}
		}
		if(!sent) System.out.println("Received a remote Result: "+ rc +", "+ info);
	}

	public void onReceiveResultProperties(Properties props) {
		boolean sent = false;
		for(int n = 0; n < runnerlisteners.size(); n++){
			try{
				((SocketProtocolListener)runnerlisteners.get(n)).onReceiveResultProperties(props);
				sent = true;
			}catch(ClassCastException e){}
		}
		if(!sent) System.out.println("Received a remote Result Properties: "+ props.toString());
	}

	public void onReceiveMessage(String message) {
		boolean sent = false;
		for(int n = 0; n < runnerlisteners.size(); n++){
			try{
				((SocketProtocolListener)runnerlisteners.get(n)).onReceiveMessage(message);
				sent = true;
			}catch(ClassCastException e){}
		}
		if(!sent) System.out.println("Received a remote Message: "+ message);
	}

	public void onReceiveException(String message) {
		boolean sent = false;
		for(int n = 0; n < runnerlisteners.size(); n++){
			try{
				((SocketProtocolListener)runnerlisteners.get(n)).onReceiveException(message);
				sent = true;
			}catch(ClassCastException e){}
		}
		if(!sent) System.out.println("Received a Remote Exception: "+ message);
	}

	@Override
	public boolean sendShutdown() {
		try{ return sendProtocolMessage(Message.msg_remoteshutdown);}
		catch(InvalidObjectException x){ return false;}
	}

	@Override
	public boolean sendDispatchProps(Properties trd) {
		try{ 
			StringBuffer buffer = new StringBuffer(Message.msg_dispatchprops+ Message.msg_sep);
			CharArrayWriter writer = new CharArrayWriter();
			trd.store(writer, "testRecordData");
			buffer.append(writer.toCharArray());
			return sendProtocolMessage(buffer.toString());
		}
		catch(Exception x){	return false; }
	}

	@Override
	public boolean sendDispatchFile(String filepath) {
		try{ return sendProtocolMessage(Message.msg_dispatchfile+ Message.msg_sep + filepath);}
		catch(InvalidObjectException x){ return false;}
	}
	
	/**
	 * Send the remote client and arbitrary MESSAGE content.
	 * <p>
	 * The remote client is expected to forward the message to the test app with  
	 * the "message:" prefix stripped off.  These messages are NOT part of the standard 
	 * protocol and it is up to the local and remote code to know what to do with 
	 * them.
	 * @param message
	 * @return true if the message was successfully sent.
	 */
	public boolean sendMessage(String message){
		String fullmsg = Message.msg_message + Message.msg_sep;
		if(message != null && message.length() > 0) fullmsg += message;
		try{ return sendProtocolMessage(fullmsg);}
		catch(InvalidObjectException x){ return false;}
	}
	
	
}
