package uk.co.mauvesoft.communicator;

public abstract class Message {
	protected String room;
	protected String sender;
	
	public Message(String room, String sender) {
		this.room = room;
		this.sender = sender;
	}

	public abstract String toHTML();
}
