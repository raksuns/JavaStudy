package uk.co.mauvesoft.communicator;

import android.net.http.AndroidHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClient implements Runnable {

	protected Credentials creds;
	
	// Timestamp of latest message
	long latest_message;
	Authenticator authentication;
	MessageReceiver receiver;
	AndroidHttpClient httpclient;
	LinkedBlockingQueue<Message> outgoing;
	
	Thread send_thread;
	
	protected boolean keeprunning = true;
	
	public ChatClient(UserAccount u, MessageReceiver receiver) {
		this.creds = u.getCredentials();
		this.receiver = receiver;
		this.latest_message = 0;
		httpclient = AndroidHttpClient.newInstance("WebchatClient/0.1 (Android)");
		outgoing = new LinkedBlockingQueue<Message>();
		
		send_thread = new Thread(new Runnable() {
			public void run() {
				Message message;
				while (keeprunning) {
					try {
						message = outgoing.take();
						dispatchMessage(message);
					} catch(InterruptedException e) {
						break;
					} catch (ConnectionException e) {}
				}
			}
		});
		send_thread.start();
	}
	
	public void pleaseStop() {
		keeprunning = false;
		receiver = null;
		send_thread.interrupt();
	}
	
	@Override
	public void run() {
		try {
			while (keeprunning) {
				receiveMessages();
			}
		} catch (ConnectionException e) {
			System.out.println("Failed to connect");
		}
	}
	
	public void receiveMessages() throws ConnectionException {
		HttpGet req;
		HttpResponse resp;
		
		if (latest_message > 0) {
			req = new HttpGet("https://webchat.vertulabs.co.uk/user/messages/?since=" + latest_message);
		} else {
			req = new HttpGet("https://webchat.vertulabs.co.uk/user/messages/today");
		}
		req.addHeader(org.apache.http.impl.auth.BasicScheme.authenticate(creds, "UTF-8", false));
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);
		
		try {
			resp = httpclient.execute(req);
			int status_code = resp.getStatusLine().getStatusCode(); 
			if (status_code != 200) {
				throw new ConnectionException("Request return code " + status_code);
			}
			resp.getEntity().writeTo(bytes);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ConnectionException("Connection failed", e);
		}
		
		JSONArray messages;
		try {
			messages = new JSONArray(bytes.toString());
		} catch (JSONException e) {
			return;
		}
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
				if (receiver != null)
					receiver.messageReceived(new ChatMessage(channel, sender, message));
			} catch (JSONException e) {
				try {
					String presence = m.getString("presence");
					if (receiver != null)
						receiver.messageReceived(new PresenceMessage(channel, sender, presence));
				} catch (JSONException f) { continue; }
			}
			

		}
	}

	public void sendMessage(String room, String message) {
		outgoing.add(new ChatMessage(room, "", message));
	}
		
	protected void dispatchMessage(Message m) throws ConnectionException {
		HttpPost req;
		HttpResponse resp;
		
		req = new HttpPost("https://webchat.vertulabs.co.uk/rooms/" + m.room + "/");
		req.addHeader(org.apache.http.impl.auth.BasicScheme.authenticate(creds, "UTF-8", false));
		req.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair("message", ((ChatMessage) m).message));
		try {
			req.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		} catch (UnsupportedEncodingException e) {return;}
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);
		
		try {
			resp = httpclient.execute(req);
			int status_code = resp.getStatusLine().getStatusCode(); 
			if (status_code != 200) {
				throw new ConnectionException("Request return code " + status_code);
			}
			resp.getEntity().writeTo(bytes);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ConnectionException("Connection failed", e);
		}
	}
	
	public void finalize() {
		httpclient.close();
	}
	
	public static boolean validateCredentials(UserAccount u) throws ConnectionException {
		AndroidHttpClient httpclient = AndroidHttpClient.newInstance("ChatClient Android");
		HttpGet req = new HttpGet(StaticData.URL_LOGIN);
		
//		Credentials creds = u.getCredentials();
//		req.addHeader(org.apache.http.impl.auth.BasicScheme.authenticate(creds, "UTF-8", false));
		
		try {
			HttpResponse resp = httpclient.execute(req);
			int status_code = resp.getStatusLine().getStatusCode(); 
			if (status_code != 200)
				return false;
		} catch (IOException e) {
			throw new ConnectionException("Connection failed", e);
		} finally {
			httpclient.close();
		}
		return true;
	}
}