package com.example.luxChat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;

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
			button.setText("상품번호: " + r.getProductSeq() + " - 채팅방 번호 : " + Integer.toString(r.getRoomId()));

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

	protected EditText productNumber; // 채팅 신청할 판매자의 제품 번호.
	protected EditText chatUserEmail; // 채팅신청할 사용자의 이메일 주소.
	protected Button reqChat;
	String sellerEmail = null;
	String buyerEmail = null;
	private boolean loginSuccess = false;

	public int GET_CREDENTIALS = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rooms_list);
		
		rooms = new RoomListAdapter(this);
		productNumber = (EditText) findViewById(R.id.productNumber);
		chatUserEmail = (EditText) findViewById(R.id.email);
		reqChat = (Button) findViewById(R.id.reqChat);

		reqChat.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// 채팅 요청
				requestChat();
			}
		});

		ListView roomslist = (ListView) findViewById(R.id.roomslist);
		roomslist.setAdapter(rooms);
		
		try {
			account = UserAccount.fromPreferences(this);
			login();
		} catch (NoStoredPreferences e) {
			showLoginActivity();
			return;
		}
	}

	public void callBind() {
		bindService(new Intent(this, ChatService.class), this, BIND_AUTO_CREATE);
	}

	JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
		@Override
		public void onSuccess(JSONObject result) {
			// 0 이 아니면 로그인 실패임...
			Log.d(StaticData.LOG_TAG, "Login response : " + result);

			try {

				int status = result.getInt("status");

				if(status == 0) {
					loginSuccess = true;
					Log.d(StaticData.LOG_TAG, "Login Ok.");
				}
				else if(status == 600) {
					loginSuccess = false;
					Log.d(StaticData.LOG_TAG, "Session Error.");
				}
			}
			catch(JSONException e) {
				e.printStackTrace();
				loginSuccess = false;
			}

			loginSuccess();
		}

		@Override
		public void onFailure(Throwable e, org.json.JSONObject result) {
			loginSuccess = false;
			loginSuccess();
		}
	};

	public void login() {
		try {
			ChatClient.validateCredentials(account, RoomsListActivity.this, responseHandler);
		}
		catch(ConnectionException e) {
			loginSuccess = false;
		}
	}

	public void loginSuccess() {
		if (loginSuccess) {
			account.storePreferences(RoomsListActivity.this);
			callBind();
		}
		else {
			account.clearPreferences(RoomsListActivity.this);
			showLoginActivity();
			return;

		}
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
		Intent i = new Intent(this, ChatActivity.class);
		String sender;
		String receiver;

		if(account.getUsername().equals(r.getBuyerEmail())) {
			sender = r.getBuyerEmail();
			receiver = r.getSellerEmail();
		}
		else {
			sender = r.getSellerEmail();
			receiver = r.getBuyerEmail();
		}

		i.putExtra("room", r.getRoomId());
		i.putExtra("sender", sender);
		i.putExtra("receiver", receiver);
		startActivity(i);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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

	public void requestChat() {

		AsyncHttpClient client = SessionMgr.getClient(RoomsListActivity.this);
		Log.d(StaticData.LOG_TAG, "request chat");

		// 특정 사용자에게 채팅 요청.
		try {
			String userInputEmail = chatUserEmail.getText().toString();

			if(userInputEmail != null && !account.getUsername().equals(userInputEmail)) {
				sellerEmail = userInputEmail;  // 구매자가 채팅 신청할 판매자 이메일.
				buyerEmail = account.getUsername(); // 구매자 이메일, 현재 접속 사용자다.
			}

			client.get(this, StaticData.URL_CREATE_ROOM + "?sellerEmail=" + sellerEmail + "&buyerEmail=" + buyerEmail + "&productSeq=" + productNumber.getText().toString(), new JsonHttpResponseHandler() {
				@Override
				public void onSuccess(JSONObject resObj) {

					// 0 이 아니면 실패임.
					Log.d(StaticData.LOG_TAG, "response : " + resObj);
					try {
						int status = resObj.getInt("status");

						if(status == 0) {
							Room r = new Room();
							r.setRoomId(resObj.getInt("talk_room_id"));
							r.setBuyerEmail(resObj.getString("buyer_email"));
							r.setBuyerName(resObj.getString("buyer_nickname"));
							r.setSellerName(resObj.getString("seller_nickname"));
							r.setSellerEmail(resObj.getString("seller_email"));
							r.setBrandName(resObj.getString("brand_kor_name"));
							r.setProductSeq(resObj.getInt("product_seq"));
							r.setSellerReadDate(resObj.getString("seller_read_dt"));
							r.setBuyerReadDate(resObj.getString("byuer_read_dt"));

							enterRoom(r);
						}
						else if(status == 602) {
							// 이미 존재하는 채팅방..
						}
						else if(status == 606) {
							// 채팅방 만들기 서버 에러.
						}
						else {
							// 접속 오류.
						}
					}
					catch(JSONException e) {
						Log.d(StaticData.LOG_TAG, "Connection failed", e);
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
	}
}
