package com.jayway.android.robotium.remotecontrol.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import android.graphics.PointF;
import android.util.Log;

import com.jayway.android.robotium.remotecontrol.client.processor.ProcessorException;
import com.jayway.android.robotium.remotecontrol.solo.Message;
import com.jayway.android.robotium.solo.By;
/**
 * This class is just a wrapper to com.jayway.android.robotium.remotecontrol.solo.Message
 * As we have org.safs.sockets.Message, who has the same.
 * To avoid using the full qualified name, I create this class.
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  Feb 16, 2012
 *
 */
public class SoloMessage extends Message {

	public static final String RESULT_INFO_GENERAL_SUCCESS		= " success. ";
	public static final String RESULT_INFO_GENERAL_FAIL	 		= " fail. ";

	public static final String RESULT_INFO_COMMAND_ISNULL		= " The command from remote control is null. ";
	public static final String RESULT_INFO_COMMAND_UNKNOWN 		= " The command from remote control is unknown. ";
	
	public static final String RESULT_INFO_PARAM_WRONG	 		= " The parameter from remote control is wrong. ";
	
	public static final String RESULT_INFO_PROCESSOR_EXCEPTION	= " Processor Exception. ";
	
	public static final String RESULT_INFO_COMPARAISON_FAIL		= " Comparison Failure. ";

	public static final String RESULT_INFO_ASSERTION_FAIL 		= " Assertion Failed Error. ";
	
	public static final String RESULT_INFO_EXCEPTION	 		= " General Exception. ";
	
	public static final String RESULT_INFO_SOLO_ISNULL	 		= " The application has not been launched yet. ";
	
	public static final String RESULT_INFO_MEMORY_ISLOW 		= " The memory is low. ";
	
	public static final String RESULT_INFO_ACTIVITYMONITOR_NULL	= " The activity monitor is null. ";
	
	public static final String RESULT_INFO_GENERATE_UID_NULL	= " The generated UID is null. ";

	/**
	 * Return the optional parameter as a boolean value<br>
	 * 
	 */
	public static boolean getBoolean(Properties props, String key){		
		return Boolean.parseBoolean(props.getProperty(key));
	}
	
	/**
	 * Return the required parameter as a String value<br>
	 * If this parameter is optional, you should catch the ProcessorException<br>
	 * @throws ProcessException if the value is not found in the properties.
	 */
	public static String getString(Properties props, String key) throws ProcessorException{
		String value = props.getProperty(key);
		
		if(value==null){
			throw new ProcessorException("can't get value for key '"+key+"' in properties object.");
		}
		
		return value;
	}
	
	/**
	 * Return the required parameter as an int value<br>
	 * If this parameter is optional, you should catch the ProcessorException<br>
	 */
	public static int getInteger(Properties props, String key) throws ProcessorException{
		return getNumber(props, key, Integer.class).intValue();
	}
	
	/**
	 * Return the required parameter as a float value<br>
	 * If this parameter is optional, you should catch the ProcessorException<br>
	 */
	public static float getFloat(Properties props, String key) throws ProcessorException{		
		return getNumber(props, key, Float.class).floatValue();
	}
	
	/**
	 * Return the required parameter as a double value<br>
	 * If this parameter is optional, you should catch the ProcessorException<br>
	 */
	public static double getDouble(Properties props, String key) throws ProcessorException{		
		return getNumber(props, key, Double.class).doubleValue();
	}
	
	/**
	 * Return the required parameter as a long value<br>
	 * If this parameter is optional, you should catch the ProcessorException<br>
	 */
	public static long getLong(Properties props, String key) throws ProcessorException{		
		return getNumber(props, key, Long.class).longValue();
	}
	
	/**
	 * Warn: If the key can't be found in the properties object, a ProcessorException will be thrown out
	 * 
	 */
	public static Number getNumber(Properties props, String key, Class numberClass) throws ProcessorException{
		String value = props.getProperty(key);
		Number number = null;
		String numberClassName = Integer.class.getSimpleName();
		
		try{
			if(value==null){
				throw new NumberFormatException("can't get value for key '"+key+"' in properties object.");
			}

			if(numberClass!=null){
				numberClassName = numberClass.getSimpleName();
			}

			if(numberClassName.equals(Integer.class.getSimpleName())){
				number = Integer.decode(value);
			}else if(numberClassName.equals(Float.class.getSimpleName())){
				number = Float.valueOf(value);
			}else if(numberClassName.equals(Double.class.getSimpleName())){
				number = Double.valueOf(value);
			}else if(numberClassName.equals(Long.class.getSimpleName())){
				number = Long.valueOf(value);
			}else{
				//Add by yourself
			}
		}catch(NumberFormatException nfe){
			throw new ProcessorException(value+" can't be converted to '"+numberClassName+"'!", nfe);
		}
		
		return number;
	}
	
	/**
	 * get the 'simple class name' from a 'full qualified class name'
	 */
	public static String getSimpleClassName(String fullQulifiedClassName){
		String simpleClassName = null;
		
		if(fullQulifiedClassName!=null){
			fullQulifiedClassName = fullQulifiedClassName.trim();

			//fullQulifiedClassName should not begin or end with "."
			if(!(fullQulifiedClassName.startsWith(".") && fullQulifiedClassName.endsWith(".")) ){
				int dotIndex = fullQulifiedClassName.lastIndexOf(".");
				if(dotIndex > -1){
					simpleClassName = fullQulifiedClassName.substring(dotIndex+1);
				}else{
					simpleClassName = fullQulifiedClassName;
				}
			}
		}
		
		return simpleClassName;
	}
	
	private static String __last_unique_key = "";
	/**
	 * Routine is used to create a unique ID String key.
	 * 
	 * @return unique String
	 */
	public static String makeUniqueCacheKey(){
		
		String uniquekey = "";
		synchronized(__last_unique_key){
			do{	uniquekey = UUID.randomUUID().toString();
			}while(uniquekey.equals(__last_unique_key));
			__last_unique_key = uniquekey;
		}
		return uniquekey;
	}
	
	/**
	 * Convert a list of com.jayway.android.robotium.remotecontrol.PointF to a list of android.graphics.PointF.<br>
	 * 
	 * @see #getAndroidPoint(com.jayway.android.robotium.remotecontrol.PointF)
	 */
	public static List<PointF> getAndroidPointFList(List<com.jayway.android.robotium.remotecontrol.PointF> points){
		List<PointF> pointfs = new ArrayList<PointF>(points.size());

		for(int i=0;i<points.size();i++){
			pointfs.add(getAndroidPoint(points.get(i)));
		}
		
		return pointfs;
	}
	
	/**
	 * Convert a com.jayway.android.robotium.remotecontrol.PointF to a android.graphics.PointF.<br>
	 * 
	 * @see #getAndroidPointFList(List)
	 */
	public static PointF getAndroidPoint(com.jayway.android.robotium.remotecontrol.PointF point){
		Object tempObj = null;
		PointF androidPointF = null;
		
		tempObj = point.toAndroidPointF();
		if(tempObj instanceof PointF){
			androidPointF = (PointF) tempObj;
		}else{
			//In case that the method point.toAndroidPointF() can't return the correct android.graphics.PointF object
			androidPointF = new PointF(point.x, point.y);
		}
		
		return androidPointF;
	}
	
	/**
	 * Convert a com.jayway.android.robotium.remotecontrol.By to a com.jayway.android.robotium.solo.By.<br>
	 * 
	 */
	public static By getSoloBy(com.jayway.android.robotium.remotecontrol.By by){
		Object tempObj = null;
		By soloBy = null;
		
		tempObj = by.toSoloBy();
		if(tempObj instanceof By){
			soloBy = (By) tempObj;
		}else{
			//In case that the method by.toSoloBy() can't return the correct com.jayway.android.robotium.solo.By object
			if(SoloMessage.method_by_className.equals(by.getStaticMethodName())){
				soloBy = By.className(by.getValue());
			}else if(SoloMessage.method_by_cssSelector.equals(by.getStaticMethodName())){
				soloBy = By.cssSelector(by.getValue());
			}else if(SoloMessage.method_by_id.equals(by.getStaticMethodName())){
				soloBy = By.id(by.getValue());
			}else if(SoloMessage.method_by_name.equals(by.getStaticMethodName())){
				soloBy = By.name(by.getValue());
			}else if(SoloMessage.method_by_tagName.equals(by.getStaticMethodName())){
				soloBy = By.tagName(by.getValue());
			}else if(SoloMessage.method_by_textContent.equals(by.getStaticMethodName())){
				soloBy = By.textContent(by.getValue());
			}else if(SoloMessage.method_by_xpath.equals(by.getStaticMethodName())){
				soloBy = By.xpath(by.getValue());
			}else{
				Log.d("SoloMessage", "Need update to create instance of "+by.getStaticMethodName());
			}
		}
		
		return soloBy;
	}
	
	public static void main(String[] args) throws ProcessorException{
		Properties props = new Properties();
		
		props.put("Int", "135");
		props.put("Dbl", "135.25");
		props.put("Flt", " 123.2 ");
		props.put("Lng", "12785");
		
		int a = SoloMessage.getInteger(props, "Int");
		double b = SoloMessage.getDouble(props, "Dbl");
		float c = SoloMessage.getFloat(props, "Flt");
		long d = SoloMessage.getLong(props, "Lng");
		
		System.out.println("int = "+a);
		System.out.println("double = "+b);
		System.out.println("float = "+c);
		System.out.println("long = "+d);
	}
}
