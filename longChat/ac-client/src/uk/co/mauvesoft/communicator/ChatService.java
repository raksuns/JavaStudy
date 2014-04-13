package uk.co.mauvesoft.communicator;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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
	public class ChatServiceBinder extends Binder {
		public List<Room> getRooms() {
			return new ArrayList<Room>(rooms.values());
		}

		public List<Message> getMessages(String roomname) {
			List<Message> list = messages.get(roomname);
			if (list == null)
				return new ArrayList<Message>();
			return new ArrayList<Message>(list);
		}

		public void sendMessage(String room, String message) throws NoStoredPreferences {
			ChatService.this.sendMessage(room, message);
		}

		public void setUserAccount(UserAccount u) {
			account = u;
			try {
				startChat();
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

	// amount of history to keep for each room
	public final int MAX_MESSAGES = 2048;
	protected String username;
	protected long latest_message = 0;
	protected HashMap<String, Room> rooms;
	protected HashMap<String, List<Message>> messages;
	protected UserAccount account = null;
	protected MessagePoller poller;
	protected List<RoomsListListener> rooms_list_listeners = new ArrayList<RoomsListListener>();
	protected List<MessageReceiver> message_receivers = new ArrayList<MessageReceiver>();
	protected IBinder binder = new ChatServiceBinder();

	protected void dispatchRoomJoined(Room r) {
		for (RoomsListListener l : rooms_list_listeners) {
			l.onRoomJoined(r);
		}
	}

	protected void dispatchRoomLeft(Room r) {
		for (RoomsListListener l : rooms_list_listeners) {
			l.onRoomLeft(r);
		}
	}

	protected void dispatchMessageReceived(Message m) {
		for (MessageReceiver r : message_receivers) {
			r.messageReceived(m);
		}
	}

	@Override
	public IBinder onBind(Intent i) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			account = getUserAccount();
			startChat();
		} catch (NoStoredPreferences e) {}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopPoller();
	}

	protected void receiveMessage(Message m) {
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

	protected void sendMessage(String room, String message) throws NoStoredPreferences {
		String url = "https://webchat.vertulabs.co.uk/rooms/" + room + "/";
		AsyncHttpClient client = getClient();

		RequestParams params = new RequestParams();
		params.put("message", message);
		client.post(this, url, params, new AsyncHttpResponseHandler() {});
	}

	protected UserAccount getUserAccount() throws NoStoredPreferences {
		if (account != null)
			return account;

		try {
			account = UserAccount.fromPreferences(this);
		} catch (NoStoredPreferences e) {
			throw new NoStoredPreferences();
		}

		return account;
	}

	protected AsyncHttpClient getClient() throws NoStoredPreferences {
		UserAccount account = getUserAccount();
		AsyncHttpClient client = new AsyncHttpClient();
		client.setBasicAuth(account.getUsername(), account.getPassword());
		return client;
	}

	protected void startChat() throws NoStoredPreferences {
		AsyncHttpClient client = getClient();
		rooms = new HashMap<String, Room>();
		messages = new HashMap<String, List<Message>>();
		stopPoller();

		client.get(this, "https://webchat.vertulabs.co.uk/user/catchup", new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject resp) {
				try {
					latest_message = resp.getLong("latest");
				} catch (JSONException e) {
					return;  // don't continue; this could cause a massive blob to be downloaded
				}
				try {
					username = resp.getString("username");
				} catch (JSONException e) {}

				try {
					rooms = roomsFromJSON(resp.getJSONArray("rooms"));
				} catch (JSONException e) {}
				for (Room r : rooms.values()) {
					dispatchRoomJoined(r);
				}

				JSONArray messages;
				try {
					messages = resp.getJSONArray("messages");
					receiveMessages(messages);
				} catch (JSONException e) {}

				startPoller();
			}
		});
	}

	protected void startPoller() {
		System.out.println("Starting message poller");
		poller = new MessagePoller(account, new MessageReceiver() {
			public void messageReceived(Message m) {
				receiveMessage(m);
			}

		}, latest_message);
		poller.start();
	}

	protected void stopPoller() {
		if (poller != null)
			poller.pleaseStop();
	}

	protected void receiveMessages(JSONArray messages) {
		for (int i = 0; i < messages.length(); i++) {
			JSONObject m;
			String sender;
			String channel;
			try {
				m = (JSONObject) messages.get(i);
				sender = m.getString("sender");
				channel = m.getString("channel");
				long timestamp = m.getLong("timestamp");
				latest_message = timestamp;

			} catch (ClassCastException e) {continue;}
			catch (JSONException e) {continue;}

			try {
				String message = m.getString("message");
				receiveMessage(new ChatMessage(channel, sender, message));
			} catch (JSONException e) {
				try {
					String presence = m.getString("presence");
					receiveMessage(new PresenceMessage(channel, sender, presence));
				} catch (JSONException f) { continue; }
			}
		}
	}

	protected HashMap<String, Room> roomsFromJSON(JSONArray roomslist) {
		HashMap<String, Room> rooms = new HashMap<String, Room>();
		for (int i = 0; i < roomslist.length(); i++) {
			try {
				Room r = new Room(roomslist.getString(i));
				rooms.put(r.name, r);
			} catch (JSONException e) {
				e.printStackTrace();
				continue;
			}
		}
		return rooms;
	}
}
