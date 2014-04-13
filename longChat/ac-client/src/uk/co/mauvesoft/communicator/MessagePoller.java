package uk.co.mauvesoft.communicator;

import android.net.http.AndroidHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MessagePoller extends Thread {
	protected Credentials creds;

	// Timestamp of latest message
	long latest_message;
	MessageReceiver receiver;
	AndroidHttpClient httpclient;

	protected boolean keeprunning = true;

	public MessagePoller(UserAccount u, MessageReceiver receiver, long since) {
		this.creds = u.getCredentials();
		this.receiver = receiver;
		this.latest_message = since;
		httpclient = AndroidHttpClient.newInstance("WebchatClient/0.1 (Android)");

	}

	public void pleaseStop() {
		keeprunning = false;
		receiver = null;
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
			try {
				JSONObject m = (JSONObject) messages.get(i);
				String sender = m.getString("sender");
				String channel = m.getString("channel");
				String message = m.getString("message");
				long timestamp = m.getLong("timestamp");
				latest_message = timestamp;
				if (receiver != null)
					receiver.messageReceived(new ChatMessage(channel, sender, message));
			} catch (ClassCastException e) {}
			catch (JSONException e) {}
		}
	}

	public void finalize() {
		httpclient.close();
	}
}
