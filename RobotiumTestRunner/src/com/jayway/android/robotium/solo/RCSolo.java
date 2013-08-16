package com.jayway.android.robotium.solo;

import java.util.List;

import android.app.Activity;
import android.app.Instrumentation;

/** 
 * From release4.0, Robotium has removed the API getAllOpenedActivities(). We want to keep supporting this API in
 * Robotium RemoteControl. As in class {@link Solo}, there is a protected field activityUtils,
 * which provides the API getAllOpenedActivities(), so {@link RCSolo} is created as subclass of {@link Solo}.
 * But {@link ActivityUtils} is only visible within package {@link com.jayway.android.robotium.solo}, so the
 * class {@link RCSolo} is put in the same package.<br><br>
 * 
 * There are also some other protected fields, which are useful, such as:<br>
 * {@link Solo#checker}<br>
 * {@link Solo#asserter}<br>
 * {@link Solo#clicker}<br>
 * {@link Solo#dialogUtils}<br>
 * {@link Solo#getter}<br>
 * {@link Solo#presser}<br>
 * {@link Solo#screenshotTaker}<br>
 * {@link Solo#scroller}<br>
 * {@link Solo#searcher}<br>
 * {@link Solo#sender}<br>
 * {@link Solo#setter}<br>
 * {@link Solo#sleeper}<br>
 * {@link Solo#textEnterer}<br>
 * {@link Solo#viewFetcher}<br>
 * {@link Solo#waiter}<br>
 * {@link Solo#webUrl}<br>
 * {@link Solo#webUtils}<br>
 * <br>
 * But these fields can ONLY be used in this class or its subclass within package {@link com.jayway.android.robotium.solo}.
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  May 21, 2013
 * <br>    May 17, 2013		(SBJLWA)	Update to add removed method finishInactiveActivities() in Robotium 4.1<br>
 */

public class RCSolo extends Solo{

	public RCSolo(Instrumentation instrumentation) {
		super(instrumentation);
	}

	public RCSolo(Instrumentation instrumentation, Activity activity){
		super(instrumentation, activity);
	}
	
	/**
	 * This method is removed from {@link Solo} from Robotium4.1 release.<br>
	 * Expose it in {@link RCSolo} to keep the backward compatibility.<br> 
	 */
	public List<Activity> getAllOpenedActivities(){
		if(activityUtils==null) return null;
		return activityUtils.getAllOpenedActivities();
	}

	/**
	 * This method is removed from {@link Solo} from Robotium4.1 release.<br>
	 * <p>
	 * The Activity handling has changed since that method was introduced. 
	 * Only weak references of Activities are now stored and Activities are 
	 * now also removed as soon as new ones are opened. Due to these changes 
	 * finishInactiveActivities has lost its purpose.
	 * The old implementation introduced crashes as keeping references to 
	 * Activities resulted in memory not being freed.
	 * <p>
	 * Expose it in {@link RCSolo} as a do-nothing to keep the backward compatibility.<br> 
	 * @deprecated
	 */
	public void finishInactiveActivities() {
	}

}
