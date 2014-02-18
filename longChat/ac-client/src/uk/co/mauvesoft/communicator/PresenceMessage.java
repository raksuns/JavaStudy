package uk.co.mauvesoft.communicator;

public class PresenceMessage extends Message {
	protected String presence;
	
	public PresenceMessage(String room, String sender, String presence) {
		super(room, sender);
		this.presence = presence;
	}
	
	public String toHTML() {
		return toString();
	}
	
	@Override
	public String toString() {
		if (presence.equals("online")) {
			return sender + " has joined the room.";
		}
		return sender + " has left the room.";
	}
}
