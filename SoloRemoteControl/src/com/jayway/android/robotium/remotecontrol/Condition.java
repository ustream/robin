package com.jayway.android.robotium.remotecontrol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 
 * From release4.0, Robotium begins to support some APIs who use com.jayway.android.robotium.solo.Condition as<br>
 * parameter. To support this kind of APIs, this class was created. This class implements interface Serializable<br>
 * so that it can be transported as an object thru our TCP protocol. This class also implements interface<br>
 * com.jayway.android.robotium.solo.Condition so that it can be used directly by Robotium's APIs.<br>
 * 
 * Example of usage:<br>
 * At controller side, user can create a concrete class of Condition like following:<br>
 * <pre>
	public class MyCondition extends Condition{
		public boolean isSatisfied() {
			boolean satisfied = true;
			try{
				List&lt;Object&gt; views = this.getObjects();
				//The first object is reserved by TestRunner object
				//RobotiumTestRunner runner = (RobotiumTestRunner) views.get(0);
				//Solo solo = runner.getSolo();
				
				//From 1, they are View objects
				for(int i=1;i&lt;views.size();i++){
					satisfied = satisfied && ((View) views.get(i)).isShown();
				}
			}catch(Exception e){
				e.printStackTrace();
				satisfied = false;
			}
			return satisfied;
		}
	}
 * </pre>
 * The class MyCondition needs to be put into the TestRunner project. The TestRunner project<br>
 * should be rebuilt<br>
 * Then, user can simply create an instance 'condition' of class MyCondition and call it's method<br>
 * {@link #addObjects(List)}/{@link #addObject(Object)} to add the view's UID.<br>
 * For example:<br>
 * <pre>
 * 	private void testWaitForCondition(){
		try {
			Condition condition = new MyCondition();
			List listviews = solo.getCurrentListViews();
			condition.addObjects(listviews);
			boolean isSatisfied = solo.waitForCondition(condition, 10);
			System.out.println("waitForCondition  isSatisfied: " + isSatisfied);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
 * </pre>
 * 
 * At the device side, we receive that object 'condition', which can be used by Robotium's APIs.<br><br>
 * 
 * This class implements com.jayway.android.robotium.solo.Condition, if that interface is modified, we needs<br>
 * to update this class also.<br>
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  May 14, 2013
 *
 */
public abstract class Condition implements com.jayway.android.robotium.solo.Condition, Serializable{
	private static final long serialVersionUID = 5322165220611572454L;

	/**
	 * Should do the necessary work needed to check a condition and then return whether this condition is satisfied or not.
	 * @return {@code true} if condition is satisfied and {@code false} if it is not satisfied
	 */
	public abstract boolean isSatisfied();
	
	/**
	 * This field is also used to transport view's UID from controller to device.<br>
	 * At the controller side, we put the views' UID into the list {@link #objects}<br>
	 * At the device side, we get the real instance of View from cache and replace the UID<br>
	 * so that the list {@link #objects} will contain instances of View.<br>
	 * 
	 * The first object is an instance of class AbstractTestRunner.
	 * 
	 * @see com.jayway.android.robotium.remotecontrol.client.AbstractTestRunner
	 * @see android.view.View
	 */
	List<Object> objects = new ArrayList<Object>();
	
    public List<Object> getObjects(){
    	return this.objects;
    }
    public void setObjects(List<Object> objects){
    	this.objects = objects;
    }
    
    /**
     * Append all objects to the existing list {@link #objects}<br>
     */
    public void addObjects(List<Object> objects){
    	if(objects==null){
    		objects = new ArrayList<Object>();
    	}
    	this.objects.addAll(objects);
    }
    
    /**
     * Append an object to the list {@link #objects}.<br>
     * 
     * @see #addObject(Object, boolean)
     */
    public void addObject(Object object){
    	this.addObject(object, false);
    }
    
    /**
     * Add an object to the list {@link #objects}.<br>
     * 
     * @param object		Object, to be added to the list {@link #objects}
     * @param asFirstItem	boolean, if we add the object as the first item of list.
     */
    public void addObject(Object object, boolean asFirstItem){
    	if(objects==null){
    		objects = new ArrayList<Object>();
    	}
    	if(asFirstItem){
    		objects.add(0, object);
    	}else{
    		objects.add(object);
    	}
    }

    /**
     * Replace the existing object in the list {@link #objects}<br>
     * 
     * @param object	Object, to set in the list {@link #objects}.
     * @param index		int,	the position in list {@link #objects} to put the new object.
     *                          if it is bigger than the size of the list {@link #objects}, 
     *                          nothing will happen.
     */
    public void setObject(Object object, int index){
    	if(objects==null || objects.size()<=index) return;
    	objects.set(index, object);
    }
}
