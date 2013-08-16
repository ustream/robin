package org.safs.android.messenger;

import android.os.Messenger;

public interface MessengerListener extends MultipleParcelListener{

	public void onMessengerDebug(String message);
	public void onEngineDebug(String message);
	public void onEngineException(String message);
	public void onEngineMessage(String message);
	public void onEngineResult(int statuscode, String statusinfo);
	public void onEngineResultProps(char[] props);
	public void onEngineShutdown(int cause);
	public void onEngineReady();
	public void onEngineRegistered(Messenger messenger);
	public void onEngineUnRegistered();
	public void onEngineRunning();
}
