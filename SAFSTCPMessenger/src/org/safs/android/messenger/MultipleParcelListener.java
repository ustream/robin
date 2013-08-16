package org.safs.android.messenger;

public interface MultipleParcelListener {

	public void onAllParcelsHaveBeenHandled(String messageID);
	public void onParcelHasBeenHandled(String messageID, int index);
}
