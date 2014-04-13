package com.example.luxChat;

import android.content.Context;

public abstract class Message {
	protected int room;
	protected String sender;

	public Message() {}

	public Message(int room, String sender) {
		this.room = room;
		this.sender = sender;
	}
	public abstract String toMessageString(Context c);
}

