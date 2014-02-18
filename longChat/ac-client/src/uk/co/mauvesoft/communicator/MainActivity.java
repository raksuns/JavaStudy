package uk.co.mauvesoft.communicator;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements MessageReceiver, TextView.OnEditorActionListener {

	class MessageListAdapter extends ArrayAdapter<Message> {
		public MessageListAdapter(Context c) {
			super(c, R.layout.message_layout);
			
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Message m = getItem(position);

			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService
				      (Context.LAYOUT_INFLATER_SERVICE);
			
			//FIXME: try to recycle convertView
			if (m instanceof PresenceMessage) {
				convertView = inflater.inflate(R.layout.message_presence, null);
			} else {
				convertView = inflater.inflate(R.layout.message_layout, null);
			}
			
			((TextView) convertView).setText(Html.fromHtml(m.toHTML()));
			
			return convertView;
		}
	}
	
	ArrayAdapter<Message> messages;
	String room;
	ChatService.ChatServiceBinder chatservice = null;
	ListView messagepane;
	
	ServiceConnection chatserviceconnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			chatservice = (ChatService.ChatServiceBinder) binder;
	        messages = new MessageListAdapter(MainActivity.this);
	        messages.addAll(chatservice.getMessages(room));
	        messagepane.setAdapter(messages);
	        messagepane.post(new Runnable() {
	        	@Override
	        	public void run() {
	        		messagepane.setSelection(messagepane.getCount() - 1);
	        	}
	        });
			chatservice.addMessageReceiver(MainActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
		}
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        messagepane = (ListView) findViewById(R.id.messagepane);
        TextView input = (TextView) findViewById(R.id.input);
        input.setOnEditorActionListener(this);
        
        room = getIntent().getStringExtra("room");
        setTitle(room);
		
		bindService(new Intent(this, ChatService.class), chatserviceconnection, BIND_AUTO_CREATE);
    }
    
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (chatservice != null)
			chatservice.removeMessageReceiver(MainActivity.this);
		unbindService(chatserviceconnection);
	}
    
    public void messageReceived(final Message m) {
    	this.runOnUiThread(new Runnable() { public void run() {
    		if (m.room.equals(room)) {
    			messages.add(m);
    		}
    	}});
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (chatservice == null) return false;
		
		try {
			chatservice.sendMessage(room, v.getText().toString());
		} catch (NoStoredPreferences e) { return false; }
		v.setText("");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // This is called when the Home (Up) button is pressed
	            // in the Action Bar.
	            Intent parentActivityIntent = new Intent(this, RoomsListActivity.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(parentActivityIntent);
	            finish();
	            return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
}
