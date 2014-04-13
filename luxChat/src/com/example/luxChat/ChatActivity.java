package com.example.luxChat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatActivity extends Activity implements MessageReceiver {

	class MessageListAdapter extends ArrayAdapter<Message> {
		public MessageListAdapter(Context c) {
			super(c, R.layout.message_layout);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Message m = getItem(position);

			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.message_layout, null);

			((TextView) convertView).setText(m.toMessageString(ChatActivity.this));

			return convertView;
		}
	}

	String sender;
	String receiver;
	int roomid;
	TextView input;
	ArrayAdapter<Message> messages;
	ChatService.ChatServiceBinder chatService = null;
	ListView messagepane;

	ServiceConnection chatServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			chatService = (ChatService.ChatServiceBinder) binder;
			messages = new MessageListAdapter(ChatActivity.this);
			messages.addAll(chatService.getMessages(roomid));

			messagepane.setAdapter(messages);
			messagepane.post(new Runnable() {
				@Override
				public void run() {
					messagepane.setSelection(messagepane.getCount() - 1);
				}
			});

			chatService.addMessageReceiver(ChatActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		messagepane = (ListView) findViewById(R.id.messagepane);
		input = (TextView) findViewById(R.id.input);

		Button deleteBtn = (Button)findViewById(R.id.deleteBtn);
		Button sendBtn = (Button)findViewById(R.id.sendChat);

		deleteBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				chatService.sendDelete(roomid, deleteHandler);
			}
		});

		sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				sendMessage();
			}
		});

		roomid = getIntent().getIntExtra("room", 0);
		sender = getIntent().getStringExtra("sender");
		receiver = getIntent().getStringExtra("receiver");
		setTitle(Integer.toString(roomid));

		bindService(new Intent(this, ChatService.class), chatServiceConnection, BIND_AUTO_CREATE);

		updateReadStatus();
	}

	// 채팅방 삭제 응답 핸들러.
	JsonHttpResponseHandler deleteHandler = new JsonHttpResponseHandler() {
		@Override
		public void onSuccess(JSONObject result) {
			Log.d(StaticData.LOG_TAG, "Login response : " + result);

			try {

				int status = result.getInt("status");

				if(status == 0) {
					finish();
				}
			}
			catch(JSONException e) {
				e.printStackTrace();
			}
			Log.d(StaticData.LOG_TAG, "Chat Room Delete error.");

		}

		@Override
		public void onFailure(Throwable e, org.json.JSONObject result) {
			Log.d(StaticData.LOG_TAG, "Chat Room Delete error.");
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (chatService != null)
			chatService.removeMessageReceiver(ChatActivity.this);
		unbindService(chatServiceConnection);
	}

	public void messageReceived(final Message m) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (m.room == roomid) {
					Log.d(StaticData.LOG_TAG, "ChatActivity.. messageReceived...");
					messages.add(m);
				}
			}
		});
	}

	public boolean sendMessage() {
		if (chatService == null) return false;

		String msg = input.getText().toString();
		if(msg != null && !"".equals(msg)) {
			try {
				chatService.sendMessage(roomid, receiver, input.getText().toString());
			} catch (NoStoredPreferences e) {
				return false;
			}

			input.setText("");
		}

		return true;
	}

	public void updateReadStatus() {
		AsyncHttpClient client = SessionMgr.getClient(getApplicationContext());

		RequestParams params = new RequestParams();
		params.put("talk_room_id", Integer.toString(roomid));
		client.get(this, StaticData.URL_UPDATE_READ_STATUS, params, updateReadStatusHandler);
	}

	// 채팅방 읽은 시간 업데이트 핸들러.
	JsonHttpResponseHandler updateReadStatusHandler = new JsonHttpResponseHandler() {
		@Override
		public void onSuccess(JSONObject result) {
			Log.d(StaticData.LOG_TAG, "updateReadStatusHandler response : " + result);

			try {

				int status = result.getInt("status");

				if(status == 0) {
					String latest = result.getString("latest");
					UserAccount.setLatestPreferences(ChatActivity.this.getApplicationContext(), roomid, latest);
				}
				else {
					Log.d(StaticData.LOG_TAG, "Chat room update error.");
					finish();
				}
			}
			catch(JSONException e) {
				e.printStackTrace();
				Log.d(StaticData.LOG_TAG, "Chat room update error.");
				finish();
			}
		}

		@Override
		public void onFailure(Throwable e, org.json.JSONObject result) {
			Log.d(StaticData.LOG_TAG, "Chat room update error.");
			finish();
		}
	};
}
