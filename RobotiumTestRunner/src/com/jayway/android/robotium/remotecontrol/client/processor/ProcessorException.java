/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package com.jayway.android.robotium.remotecontrol.client.processor;


/**
 * This class is used to wrap the errors during processing the messages<br>
 * from remote robotium solo in {@link SoloProcessor}
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  Feb 21, 2012
 *
 * @see SoloProcessor
 */
public class ProcessorException extends Exception {

	private static final long serialVersionUID = 1L;

	public ProcessorException() {
		super();
	}

	public ProcessorException(final String detailMessage, final Throwable throwable) {
		super(detailMessage, throwable);
	}

	public ProcessorException(final String detailMessage) {
		super(detailMessage);
	}

	public ProcessorException(final Throwable throwable) {
		super(throwable);
	}

}
