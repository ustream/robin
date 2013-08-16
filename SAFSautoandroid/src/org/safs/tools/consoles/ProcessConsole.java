/** Copyright (C) SAS Institute All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.tools.consoles;

import java.lang.Process;

/**
 * Simply overrides the GenericProcessConsole debug() method to use the SAFS Debug Log.
 * @see GenericProcessConsole#debug(String)
 * @see #debug(String)
 */
public class ProcessConsole extends GenericProcessConsole{

	/**
	 * Constructor for ProcessConsole
	 * @see #setShowOutStream(boolean)
	 * @see #setShowErrStream(boolean)
	 */
	public ProcessConsole(Process process) {
		super(process);
	}
}

