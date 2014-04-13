package com.example.luxChat;

import android.content.Context;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;

// 채팅 메시지 수신을 위한 long polling thread
public class MessagePoller extends Thread {

	protected Credentials creds;

	MessageReceiver receiver;
	UserAccount user;
	AsyncHttpClient httpClient;

	protected boolean keeprunning = true;

	public MessagePoller(Context c, UserAccount u, MessageReceiver receiver) {
		this.user = u;
		this.receiver = receiver;
		this.httpClient = SessionMgr.getClient(c);
	}

	public void pleaseStop() {
		keeprunning = false;
		receiver = null;
	}

	@Override
	public void run() {
		polling();
	}

	public void polling() {
		try {
			while(keeprunning) {
				Log.d(StaticData.LOG_TAG, "Message Poller run...");
				receiveMessage();
			}
		}
		catch(ConnectionException e) {
			System.out.println("Failed to connect.  cause : " + e.getMessage());
			polling();
		}
	}


	// 채팅 서버 접속을 유지하며 메시지를 수신한다.
	public void receiveMessage() throws ConnectionException {
		HttpGet req;
		HttpResponse res;

		req = new HttpGet(StaticData.URL_CHAT_ON);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);

		try {
			httpClient.setTimeout(60 * 1000 * 1000);
			res = httpClient.getHttpClient().execute(req, SessionMgr.getContext());
			int statusCode = res.getStatusLine().getStatusCode();

			if(statusCode != 200) {
				throw new ConnectionException("Request return code " + statusCode);
			}

			res.getEntity().writeTo(bytes);
		}
		catch(SocketTimeoutException e) {
			throw new ConnectionException("Socket Timeout.");
		}
		catch(IOException e) {
			e.printStackTrace();
			throw new ConnectionException("Connection failed", e);
		}

		JSONObject message;

		try {
			message = new JSONObject(bytes.toString());
			message = message.getJSONObject("chat_message"); // 상대방으로부터 메시지를 수신했다.
			Log.d(StaticData.LOG_TAG, "recv message : \n" + message.toString());
		}
		catch(JSONException e) {
			return;
		}

		String buyerEmail;
		String content;
		String msgType;
		int productSeq;
		String regDt;
		String sellerEmail;
		int talkRoomId;
		int talkSeq;
		String writer;
		String brandKorName;
		String sellerNickname;
		String buyerNickname;

		try {
			buyerEmail = message.getString("buyer_email");
			content = message.getString("content");
			msgType = message.getString("msg_type");
			productSeq = message.getInt("product_seq");
			regDt = message.getString("reg_dt");
			sellerEmail = message.getString("seller_email");
			talkRoomId = message.getInt("talk_room_id");
			talkSeq = message.getInt("talk_seq");
			writer = message.getString("writer");
			brandKorName = message.getString("brand_kor_name");
			sellerNickname = message.getString("seller_nickname");
			buyerNickname = message.getString("buyer_nickname");

			if(receiver != null) {
				String sender = "sender";
				if(buyerEmail.equals(user.getUsername())) {
					sender = sellerEmail;
				}
				else {
					sender = buyerEmail;
				}

				receiver.messageReceived(
					new RecvMessage(buyerEmail, content, msgType, productSeq, regDt, sellerEmail, talkRoomId, talkSeq, writer, sender, brandKorName,
						sellerNickname, buyerNickname));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void finalize() {
		httpClient = null;
	}
}
