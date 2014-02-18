package uk.co.mauvesoft.communicator;

public interface RoomsListListener {
	public abstract void onRoomJoined(Room r);
	public abstract void onRoomLeft(Room r);
}
