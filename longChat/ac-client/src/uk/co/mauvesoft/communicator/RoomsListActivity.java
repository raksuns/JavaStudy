package uk.co.mauvesoft.communicator;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class RoomsListActivity extends Activity implements ServiceConnection {


	public class RoomListAdapter extends ArrayAdapter<Room> {
		public RoomListAdapter(RoomsListActivity c) {
			super(c, R.layout.roomlist_room, R.id.roomname);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService
				      (Context.LAYOUT_INFLATER_SERVICE);
			
			convertView = inflater.inflate(R.layout.roomlist_room, null);
			
			final RoomsListActivity activity = (RoomsListActivity) getContext();
			final Room r = getItem(position);
			
			Button button = (Button) convertView.findViewById(R.id.roomname);
			button.setText(r.name);
			
			TextView unread = (TextView) convertView.findViewById(R.id.unread);
			if (r.unread > 0) {
				unread.setText(Integer.valueOf(r.unread).toString());
			} else {
				unread.setVisibility(View.INVISIBLE);
			}

	        button.setOnClickListener(new View.OnClickListener() {
	             public void onClick(View v) {
	            	 activity.enterRoom(r);
	             }
	        });
			
			return convertView;
		}
	}
	
	protected RoomListAdapter rooms;
	protected UserAccount account;
	
	public int GET_CREDENTIALS = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rooms_list);
		
		rooms = new RoomListAdapter(this);
		ListView roomslist = (ListView) findViewById(R.id.roomslist);
		roomslist.setAdapter(rooms);
		
		try {
			account = UserAccount.fromPreferences(this);
			
		} catch (NoStoredPreferences e) {
			showLoginActivity();
			return;
		}
		
		bindService(new Intent(this, ChatService.class), this, BIND_AUTO_CREATE);
	}
	
	protected void showLoginActivity() {
		startActivityForResult(new Intent(this, LoginActivity.class), GET_CREDENTIALS);
	}
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    if (chatservice != null)
	    	chatservice.removeRoomsListListener(roomslist_listener);
	    unbindService(this);
	}
	
	ChatService.ChatServiceBinder chatservice = null;
	RoomsListListener roomslist_listener = new RoomsListListener() {
		@Override
		public void onRoomJoined(Room r) {
			rooms.add(r);
		}

		@Override
		public void onRoomLeft(Room r) {
			rooms.remove(r);
		}
	};
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		System.out.println("Connected to chat service...");
		chatservice = (ChatService.ChatServiceBinder) binder;
		rooms.addAll(chatservice.getRooms());
		chatservice.addRoomsListListener(roomslist_listener);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		System.out.println("Disconnected from chat service...");
		chatservice = null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try {
			account = UserAccount.fromPreferences(this);
		} catch (NoStoredPreferences e) {
			finish();
			return;
		}
		if (chatservice != null) {
			chatservice.setUserAccount(account);
		} else {
			bindService(new Intent(this, ChatService.class), this, BIND_AUTO_CREATE);
		}
	}

	public void enterRoom(Room r) {
		Intent i = new Intent(this, MainActivity.class);
		i.putExtra("room", r.name);
		startActivity(i);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_rooms_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_logout:
	            UserAccount.clearPreferences(this);
	            finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}
