/** Copyright (C) SAS Institute. All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/

package com.jayway.android.robotium.remotecontrol.solo;

/**
 * <br><em>Purpose:</em> our user defined application exception used with this package.
 * <p>
 * @author  Lei Wang
 *
 *   <br>   APR 01, 2012    (SBJLWA) Original Release
 **/
public class RemoteSoloException extends Exception {

  /** <br><em>Purpose:</em> constructor
   * @param                     msg, String, the string to pass along to our 'super'
   **/
  public RemoteSoloException (String msg) {
    super(msg);
  }

  /** <br><em>Purpose:</em> constructor
   * @param  msg, String, the string to pass along to our 'super'
   * @param  cause, Throwable 'cause' to pass along to our 'super'.
   **/
  public RemoteSoloException (Throwable cause) {
    super(cause);
  }

  /** <br><em>Purpose:</em> constructor
   * @param  msg, String, the string to pass along to our 'super'
   * @param  cause, Throwable 'cause' to pass along to our 'super'.
   **/
  public RemoteSoloException (String msg, Throwable cause) {
    super(msg, cause);
  }

  /** <br><em>Purpose:</em> constructor
   * @param  cause, Throwable 'cause' to pass along to our 'super'.
   * @param  msg, String, the string to pass along to our 'super'
   **/
  public RemoteSoloException ( Throwable cause, String msg) {
    super(msg, cause);
  }
}
