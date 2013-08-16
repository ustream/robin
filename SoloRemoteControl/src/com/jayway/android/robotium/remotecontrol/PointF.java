package com.jayway.android.robotium.remotecontrol;

import java.io.Serializable;
import java.lang.reflect.Constructor;

/** 
 * From release4.1+, Robotium begins to provide some APIs who use android.graphics.PointF as<br>
 * parameter. To support this kind of APIs, this class was created, it implements interface Serializable<br>
 * so that it can be transported as an object thru our TCP protocol.<br>
 * Example of usage:<br>
 * At controller side, user can simply call {@link PointF#PointF(float, float)} to create instance 'pointf'.<br>
 * and send this object thru the wire.<br>
 * At the device side, we receive that object 'pointf' and we call {@link PointF#toAndroidPointF()} <br>
 * to get an Android's PointF object, which can be used by Robotium's APIs.<br><br>
 * 
 * This class copy the content of android.graphics.PointF, if that class is modified, we needs<br>
 * to update this class also. The easiest implementation is to extend the android.graphics.PointF, but<br>
 * android.jar only provides the stub code which can't be used to instantiate a Class, so I have to<br>
 * copy the content of android.graphics.PointF<br>
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  Jun 21, 2013
 *
 */
public class PointF implements Serializable{
	private static final long serialVersionUID = -4211166636237039900L;
	public float x;
    public float y;
    
    public PointF() {}

    public PointF(float x, float y) { 
        this.x = x;
        this.y = y; 
    }
    
    /**
     * Set the point's x and y coordinates
     */
    public final void set(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Set the point's x and y coordinates to the coordinates of p
     */
    public final void set(PointF p) { 
        this.x = p.x;
        this.y = p.y;
    }
    
    public final void negate() { 
        x = -x;
        y = -y; 
    }
    
    public final void offset(float dx, float dy) {
        x += dx;
        y += dy;
    }
    
    /**
     * Returns true if the point's coordinates equal (x,y)
     */
    public final boolean equals(float x, float y) { 
        return this.x == x && this.y == y; 
    }

    /**
     * Return the euclidian distance from (0,0) to the point
     */
    public final float length() { 
        return length(x, y); 
    }
    
    /**
     * Returns the euclidian distance from (0,0) to (x,y)
     */
    public float length(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }
    
	public String toString(){
		return "[x="+x+", y="+y+"]";
	}
    
	/**
	 * Convert this class to android.graphics.PointF by reflection.<br>
	 * It is not suggested to call this method frequently. User should store it in<br>
	 * a local variable and use that variable.<br>
	 * 
	 * @return Object, an object of android.graphics.PointF
	 */
    public Object toAndroidPointF(){
    	try {
    		//Get a android.graphics.PointF constructor of 2 float parameters
    		Constructor<?> constructor = Class.forName("android.graphics.PointF").getConstructor(new Class[]{float.class, float.class});
			return constructor.newInstance(new Object[]{x,y});
		} catch (Throwable ignore) {}
		return null;
    }
}
