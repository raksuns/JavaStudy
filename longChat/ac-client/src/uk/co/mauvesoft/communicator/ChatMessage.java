package uk.co.mauvesoft.communicator;

public class ChatMessage extends Message {
	protected String message;
	
	public ChatMessage(String room, String sender, String message) {
		super(room, sender);
		this.message = message;
	}
	
	public String toHTML() {
		return "<font color=\"#008CB2\">" + sender + ":</font> " + message;
	}
	
	@Override
	public String toString() {
		return sender + ": " + message;
	}
}
