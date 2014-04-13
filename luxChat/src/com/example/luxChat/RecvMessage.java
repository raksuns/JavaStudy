package com.example.luxChat;

import android.content.Context;

public class RecvMessage extends Message {

	protected String buyer_email;
	protected String content;
	protected String msg_type;
	protected int    product_seq;
	protected String reg_dt;
	protected String seller_email;
	protected int talk_room_id;
	protected int talk_seq;
	protected String writer;
	protected String sender;
	protected String brandKorName;
	protected String sellerNickname;
	protected String buyerNickname;

	public RecvMessage() {
		super();
	}

	public RecvMessage(String buyer_email, String content, String msg_type, int product_seq, String reg_dt, String seller_email,
	                   int talk_room_id, int talk_seq, String writer, String sender, String brandKorName, String sellerNickname, String buyerNickname) {
		super(talk_room_id, sender);
		this.sender = sender;
		this.buyer_email = buyer_email;
		this.content = content;
		this.msg_type = msg_type;
		this.product_seq = product_seq;
		this.reg_dt = reg_dt;
		this.seller_email = seller_email;
		this.talk_room_id = talk_room_id;
		this.talk_seq = talk_seq;
		this.writer = writer;
		this.brandKorName = brandKorName;
		this.sellerNickname = sellerNickname;
		this.buyerNickname = buyerNickname;
	}

	public String toMessageString(Context c) {
		return sender + " : " + content;
	}
}
