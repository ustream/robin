package com.jayway.android.robotium.remotecontrol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.jayway.android.robotium.remotecontrol.solo.Solo;

/**
 * This class is used to transport a collection of objects between MobileDevice/Emulator and RemoteControl.<br>
 * 
 * For example:<br> 
 * From release4.1+, Robotium begins to provide some APIs who use android.graphics.PointF as<br>
 * parameter. Some APIs need more than one PointF parameter, to transport multiple PointF as <br>
 * an object thru our TCP protocol in the same time, this class is created.<br>
 * Example of usage:<br>
 * At controller side, user can simply call {@link ObjectCollection()} to create an instance, and add some<br>
 * instances of PointF and send this object thru the wire.<br>
 * At the device side, we receive that object 'ObjectCollection' and we call {@link ObjectCollection#getObjectList()} <br>
 * to get a list of Android's PointF objects, which can be used by Robotium's APIs.<br><br>
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @param <T>
 * @since  Jun 21, 2013
 *
 * @see Solo#rotateLarge(PointF, PointF)
 * @see Solo#getScreenshotSequence(int, int)
 */
public class ObjectCollection<T> implements Serializable{
	private static final long serialVersionUID = 8014238078288066150L;

	private List<T> objectList = new ArrayList<T>();
	
	public List<T> getObjectList(){
		return objectList;
	}
	
	public void addToObjectList(T p){
		objectList.add(p);
	}

	public int getSize(){
		return objectList.size();
	}
	
	public T getObject(int i){
		return objectList.get(i);
	}
}
