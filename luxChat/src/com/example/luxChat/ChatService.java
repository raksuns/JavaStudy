package com.example.luxChat;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ChatService extends Service {

	public final int MAX_MESSAGES = 2048;
	protected String username;
	protected HashMap<Integer, Room> rooms;
	protected HashMap<Integer, List<Message>> messages;
	protected UserAccount user = null;
	protected MessagePoller poller;
	protected List<RoomsListListener> rooms_list_listeners = new ArrayList<RoomsListListener>();
	protected List<MessageReceiver> message_receivers = new ArrayList<MessageReceiver>();
	protected IBinder binder = new ChatServiceBinder();
	protected Room newRoom;

	final Handler newRoomHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if(newRoom != null) {
				dispatchRoomJoined(newRoom);
			}
		}
	};

	public class ChatServiceBinder extends Binder {

		public List<Room> getRooms() {
			return new ArrayList<Room>(rooms.values());
		}

		public List<Message> getMessages(int room) {
			List<Message> list = messages.get(room);

			if(list == null) {
				return new ArrayList<Message>();
			}

			return new ArrayList<Message>(list);
		}

		public void sendMessage(int talk_room_id, String receiver, String message) throws NoStoredPreferences {
			ChatService.this.sendMessage(talk_room_id, receiver, message);
		}

		public void sendDelete(int talk_room_id, JsonHttpResponseHandler jsonHandler) {
			ChatService.this.sendDelete(talk_room_id, jsonHandler);
		}

		public void setUserAccount(UserAccount u) {
			user = u;
			try {
				if(user != null) {
					startChat();
				}
			} catch (NoStoredPreferences e) {}
		}

		public void addRoomsListListener(RoomsListListener l) {
			rooms_list_listeners.add(l);
		}

		public void removeRoomsListListener(RoomsListListener l) {
			rooms_list_listeners.remove(l);
		}

		public void addMessageReceiver(MessageReceiver m) {
			message_receivers.add(m);
		}

		public void removeMessageReceiver(MessageReceiver m) {
			message_receivers.remove(m);
		}
	}

	protected void dispatchRoomJoined(Room r) {
		for (RoomsListListener l : rooms_list_listeners) {
			l.onRoomJoined(r);
		}
	}

	protected void dispatchMessageReceived(Message m) {
		Log.d(StaticData.LOG_TAG, "dispatchMessageReceived...");
		for(MessageReceiver r : message_receivers) {
			r.messageReceived(m);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			user = getUserAccount();
			if(user != null) {
				startChat();
			}
		} catch (NoStoredPreferences e) {}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopPoller();
	}

	protected void receiveMessage(Message m) {

		if(rooms.get(m.room) != null) {

			List<Message> list;
			list = messages.get(m.room);

			if (list == null) {
				list = new LinkedList<Message>();
				messages.put(m.room, list);
			}

			list.add(m);
			while (list.size() > MAX_MESSAGES) {
				list.remove(0);
			}
			Room r = rooms.get(m.room);
			if (r != null) {
				r.unread += 1;
			}

			dispatchMessageReceived(m);
		}
		else {
			// 단말에 방정보가 없다면, 신규 채팅 요청으로 판단하여 서버로 부터 채팅방 정보를 가져온다.
			requestChatRoom(m.room, m);
		}
	}

	protected void requestChatRoom(int talk_room_id, final Message msg) {
		AsyncHttpClient client = SessionMgr.getClient(getApplicationContext());

		RequestParams params = new RequestParams();
		params.put("talk_room_id", Integer.toString(talk_room_id));
		client.post(this, StaticData.URL_GET_CHAT_ROOM, params, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject result) {
				Log.d(StaticData.LOG_TAG, "getChatRoomHandler response : " + result);

				try {
					int status = result.getInt("status");
					if (status == 0) {
						JSONObject roomObj = result.getJSONObject("room");

						Room r = new Room();
						r.setRoomId(roomObj.getInt("talk_room_id"));
						r.setSellerEmail(roomObj.getString("seller_email"));
						r.setBuyerEmail(roomObj.getString("buyer_email"));
						r.setProductSeq(roomObj.getInt("product_seq"));
						r.setBrandName(roomObj.getString("brand_kor_name"));
						r.setSellerName(roomObj.getString("seller_nickname"));
						r.setBuyerName(roomObj.getString("buyer_nickname"));
						r.setSellerReadDate(roomObj.getString("seller_read_dt"));
						r.setBuyerReadDate(roomObj.getString("byuer_read_dt"));

						rooms.put(r.getRoomId(), r);

						dispatchRoomJoined(r);

						receiveMessage(msg);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				Log.d(StaticData.LOG_TAG, "getChatRoomHandler error.");

			}

			@Override
			public void onFailure(Throwable e, org.json.JSONObject result) {
				Log.d(StaticData.LOG_TAG, "getChatRoomHandler error.");
			}
		});
	}

	protected void sendMessage(int talk_room_id, String receiver, String message) throws NoStoredPreferences {
		AsyncHttpClient client = SessionMgr.getClient(getApplicationContext());

		RequestParams params = new RequestParams();
		params.put("talk_room_id", Integer.toString(talk_room_id));
		params.put("receiver", receiver);
		params.put("message", message);
		client.post(this, StaticData.URL_SEND_CHAT, params, new AsyncHttpResponseHandler() {});

		// 보낸 메시지를 화면에 뿌리기 위해 메시지 리스트에 넣어줌.
		receiveMessage(new SendMessage(talk_room_id, receiver, user.getUsername(), message));
	}

	protected void sendDelete(int talk_room_id, JsonHttpResponseHandler responseHandler) {
		AsyncHttpClient client = SessionMgr.getClient(getApplicationContext());

		RequestParams params = new RequestParams();
		params.put("talk_room_id", Integer.toString(talk_room_id));
		client.post(this, StaticData.URL_CHAT_ROOM_DELETE, params, responseHandler);
	}

	protected UserAccount getUserAccount() throws NoStoredPreferences {
		if (user != null)
			return user;

		try {
			user = UserAccount.fromPreferences(this);
		} catch (NoStoredPreferences e) {
			throw new NoStoredPreferences();
		}

		return user;
	}

	protected void startChat() throws NoStoredPreferences {
		AsyncHttpClient client = SessionMgr.getClient(getApplicationContext());
		rooms = new HashMap<Integer, Room>();
		messages = new HashMap<Integer, List<Message>>();
		stopPoller();

		Log.d(StaticData.LOG_TAG, "start chat");
		// 처음 시작하는 경우.. 현재 사용자의 모든 채팅방 목록과 채팅 메시지를 가져온다.
		try {
			client.get(this, StaticData.URL_ALL_LIST, new JsonHttpResponseHandler() {
				@Override
				public void onSuccess(JSONObject resp) {
					Log.d(StaticData.LOG_TAG, resp.toString());

					allMsgFromJSON(resp);
					for (Room r : rooms.values()) {
						dispatchRoomJoined(r);
					}
				}

				@Override
				public void onFailure(Throwable e, JSONObject result) {
					Log.d(StaticData.LOG_TAG, result.toString());
					e.printStackTrace();
				}

				@Override
				public void onStart() {
					Log.d(StaticData.LOG_TAG, "loopj start...");
				}
			});
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		startPoller();
	}

	protected void startPoller() {
		System.out.println("Starting message poller");
		poller = new MessagePoller(getApplicationContext(), user, new MessageReceiver() {
			public void messageReceived(Message m) {
				receiveMessage(m);
			}

		});
		poller.start();
	}

	protected void stopPoller() {
		if (poller != null)
			poller.pleaseStop();
	}

	protected HashMap<Integer, Room> roomsFromJSON(JSONArray roomslist) {
		HashMap<Integer, Room> rooms = new HashMap<Integer, Room>();
		JSONObject roomObj = null;
		for (int i = 0; i < roomslist.length(); i++) {
			try {
				roomObj = roomslist.getJSONObject(i);

				Room r = new Room();
				r.setRoomId(roomObj.getInt("talk_room_id"));
				r.setSellerEmail(roomObj.getString("seller_email"));
				r.setBuyerEmail(roomObj.getString("buyer_email"));
				r.setProductSeq(roomObj.getInt("product_seq"));
				r.setBrandName(roomObj.getString("brand_kor_name"));
				r.setSellerName(roomObj.getString("seller_nickname"));
				r.setBuyerName(roomObj.getString("buyer_nickname"));


				rooms.put(r.getRoomId(), r);
			} catch (JSONException e) {
				e.printStackTrace();
				continue;
			}
		}
		return rooms;
	}

	protected void allMsgFromJSON(JSONObject allMsg) {

		try {

			int count = allMsg.getInt("count"); // 채팅방 갯수

			if(count > 0) {
				JSONArray roomMessageList = allMsg.getJSONArray("list");


				for(int i = 0 ; i < roomMessageList.length(); i++) {
					JSONObject roomsMessage = roomMessageList.getJSONObject(i);
					int talkRoomId = roomsMessage.getInt("tid"); // 채팅방 번호
					int msgCount = roomsMessage.getInt("count");

					JSONObject roomObj = roomsMessage.getJSONObject("room");

					Room r = new Room();
					r.setRoomId(roomObj.getInt("talk_room_id"));
					r.setSellerEmail(roomObj.getString("seller_email"));
					r.setBuyerEmail(roomObj.getString("buyer_email"));
					r.setProductSeq(roomObj.getInt("product_seq"));
					r.setBrandName(roomObj.getString("brand_kor_name"));
					r.setSellerName(roomObj.getString("seller_nickname"));
					r.setBuyerName(roomObj.getString("buyer_nickname"));
					r.setSellerReadDate(roomObj.getString("seller_read_dt"));
					r.setBuyerReadDate(roomObj.getString("byuer_read_dt"));

					rooms.put(r.getRoomId(), r);

					if(msgCount > 0) {
						JSONArray recv_messages = roomsMessage.getJSONArray("messages");

						ArrayList<Message> roomMessages = new ArrayList<Message>();

						for(int j = 0; j < recv_messages.length(); j++) {
							JSONObject message = recv_messages.getJSONObject(j);

							RecvMessage recvMsg = new RecvMessage();

							recvMsg.buyer_email = message.getString("buyer_email");
							recvMsg.content = message.getString("content");
							recvMsg.msg_type = message.getString("msg_type");
							recvMsg.product_seq = message.getInt("product_seq");
							recvMsg.reg_dt = message.getString("reg_dt");
							recvMsg.seller_email = message.getString("seller_email");
							recvMsg.talk_room_id = message.getInt("talk_room_id");
							recvMsg.talk_seq = message.getInt("talk_seq");
							recvMsg.writer = message.getString("writer");
							recvMsg.buyerNickname = r.getBuyerName();
							recvMsg.sellerNickname = r.getSellerName();
							recvMsg.brandKorName = r.getBrandName();

							if("S".equals(recvMsg.writer)) {
								recvMsg.sender = recvMsg.seller_email;
							}
							else {
								recvMsg.sender = recvMsg.buyer_email;
							}

							roomMessages.add(recvMsg);
						}

						messages.put(r.getRoomId(), roomMessages);
					}
				}
			}
		}
		catch(JSONException e) {
			e.printStackTrace();
		}
	}
}
