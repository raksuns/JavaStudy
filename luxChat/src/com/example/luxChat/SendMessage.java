package com.example.luxChat;

import android.content.Context;

public class SendMessage extends Message {

	protected int talk_room_id;
	protected String receiver;
	protected String sender;
	protected String message;

	public SendMessage(int talk_room_id, String receiver, String sender,  String message) {
		super(talk_room_id, sender);

		this.talk_room_id = talk_room_id;
		this.receiver = receiver;
		this.sender = sender;
		this.message = message;
	}

	public String toMessageString(Context c) {
		return sender + " : " + message;
	}
}
