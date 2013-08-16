package com.jayway.android.robotium.remotecontrol;

import java.util.Properties;
import com.jayway.android.robotium.remotecontrol.solo.Message;
import com.jayway.android.robotium.remotecontrol.solo.SoloTest;

public class MyTest extends SoloTest{

  public MyTest(){ super(); }
  public MyTest(String[] args){ super(args); }
  public MyTest(String messengerApk, String testRunnerApk, String instrumentArg){
	  super(messengerApk, testRunnerApk, instrumentArg);
  }

  public static void main(String[] args){
	  SoloTest soloTest = new MyTest(args);	  
	  soloTest.process();
  }

	protected void test(){
	  try{

		  String activityID = solo.getCurrentActivity();
		  Properties props = solo._last_remote_result;
		  String activityName = props.getProperty(Message.PARAM_NAME);
		  String activityClass = props.getProperty(Message.PARAM_CLASS);

		  System.out.println("CurrentActivity   UID: "+ activityID);
		  System.out.println("CurrentActivity Class: "+ activityClass);
		  System.out.println("CurrentActivity  Name: "+ activityName);

	  }catch(Exception e){

		  e.printStackTrace();

	  }
	}
}

