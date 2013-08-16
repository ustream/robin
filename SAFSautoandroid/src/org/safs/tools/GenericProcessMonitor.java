/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.tools;

import java.io.IOException;
import java.util.Enumeration;

import org.safs.tools.consoles.GenericProcessCapture;

/**
 * Various utilities for monitoring or otherwise interrogating native system processes.
 * This "Generic" version does all logging to System.out
 * <p>
 * Subclasses should override the debug(String) method to log to alternative mechanisms 
 * and should override the 
 * <p>
 * This class contains no extended SAFS dependencies and can be readily packaged and distributed 
 * for non-SAFS installations.
 * @author canagl 2012.03.27 Original Release 
 */
public class GenericProcessMonitor {

	/**
	 * Writes to System.out .
	 * Subclasses should override to log to alternate sinks.
	 * @param message
	 */
	protected static void debug(String message){
		System.out.println(message);
	}
	
	/**
	 * Subclasses may wish to override to return a different subclass of GenericProcessCapture.
	 * @param aproc
	 * @return 
	 */
	protected static GenericProcessCapture getProcessCapture(Process aproc){
		return new GenericProcessCapture(aproc);
	}
	
	/**
	 * Return true or false that a given process is running on the system. 
	 * On Windows we are using the tasklist.exe output for comparison.
	 * On Unix we are using ps -f output for comparison.
	 * @param procid CMD name(nix ps-f) or IMAGE(win qprocess.exe) or PID to seek.  
	 * @return true if the procid (CMD or IMAGE or numeric PID) is running/listed.
	 * @throws IOException if any error occurs in getting the processes for evaluation.
	 */
	public static boolean isProcessRunning(String procid)throws IOException{

		//String wincmd = "qprocess.exe * /SYSTEM"; // alternative
		String wincmd = "tasklist.exe";
		String unxcmd = "ps -f";

		GenericProcessCapture console = null;
		boolean success = false;
		boolean run = false;
		try{
			Process proc = Runtime.getRuntime().exec(wincmd);
			console = getProcessCapture(proc);
			Thread athread = new Thread(console);
			athread.start();
			proc.waitFor();
			run = true;
			success = (proc.exitValue()==0);
		}catch(Exception x){
			// something else was wrong with the underlying process
			debug(wincmd +", "+ x.getClass().getSimpleName()+": "+ x.getMessage());
		}
		if(! run){
			try{
				Process proc = Runtime.getRuntime().exec(unxcmd);
				console = getProcessCapture(proc);
				Thread athread = new Thread(console);
				athread.start();
				proc.waitFor();
				success = (proc.exitValue()==0);
			}catch(Exception x){
				// something else was wrong with the underlying process
				debug(unxcmd +", "+ x.getClass().getSimpleName()+": "+ x.getMessage());
			}
		}
		if(success){
			success = false;
			boolean isNumeric = false;
			int pid = 0;
			int pidindex = 0;
			String pidtest = null;
			try{ 
				pid = Integer.parseInt(procid);
				isNumeric = true;
			}catch(NumberFormatException n){ }
			String line = null;
			Enumeration reader = console.getData().elements();
			while((!success) && (reader.hasMoreElements())){
				line = (String)reader.nextElement();
				if(isNumeric){
					pidindex = line.indexOf(procid);
					if(pidindex > 0){
						try{
							// grab a character before and after and make sure it is not alpha
							pidtest = line.substring(pidindex -1, procid.length()+pidindex+1).trim();
							success = ( pid == Integer.parseInt(pidtest));
						}catch(Exception x){/*ignore*/}
					}
				}else{
					success = line.contains(procid);
				}
			}			
		}
		return success;
	}
	
	/** 
	 * Attempt to forcefully kill a given process by name or PID. 
	 * On Windows we are using taskkill.exe.  
	 * On Unix we are using kill -9 for PID, or killall for process names.
	 * Of course, this must be used with care!
	 * @param procid CMD name(nix ps-f) or IMAGE(win qprocess.exe) or PID to kill.  
	 * @return true if the shutdown attempt returned with success, false otherwise.
	 * @throws IOException if no shutdown attempt was ever able to execute.
	 */
	public static boolean shutdownProcess(String procid)throws IOException{

		boolean isNumeric = false;
		try{ isNumeric = Integer.parseInt(procid) > 0;}
		catch(NumberFormatException n){ }
		
		String wincmd = "taskkill.exe /f ";
		wincmd += isNumeric ? "/pid " : "/im ";
		wincmd += procid;
		
		String unxcmd = isNumeric ? "kill -9 "+ procid:"killall "+ procid;

		GenericProcessCapture console = null;
		boolean run = false;
		boolean success = false;
		try{
			Process proc = Runtime.getRuntime().exec(wincmd);
			console = getProcessCapture(proc);
			Thread athread = new Thread(console);
			athread.start();
			proc.waitFor();
			run = true;
			success = (proc.exitValue()==0);
		}catch(Exception x){
			// something else was wrong with the underlying process
			debug(wincmd +", "+ x.getClass().getSimpleName()+": "+ x.getMessage());
		}
		if(! run){
			try{
				Process proc = Runtime.getRuntime().exec(unxcmd);
				console = getProcessCapture(proc);
				Thread athread = new Thread(console);
				athread.start();
				proc.waitFor();
				run = true;
				success = (proc.exitValue()==0);
			}catch(Exception x){
				// something else was wrong with the underlying process
				debug(unxcmd +", "+ x.getClass().getSimpleName()+": "+ x.getMessage());
			}
		}
		if (!run) throw new IOException("Neither WIN nor UNX shutdownProcess commands executed properly using procid: "+ procid);
		return success;
	 }
}
