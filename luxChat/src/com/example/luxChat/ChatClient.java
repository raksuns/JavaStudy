package com.example.luxChat;

import android.content.Context;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.apache.http.client.methods.HttpPost;

public class ChatClient {

	public ChatClient() {
	}

	public static void validateCredentials(UserAccount u, Context c, JsonHttpResponseHandler responseHandler) throws ConnectionException {

		HttpPost req = new HttpPost(StaticData.URL_LOGON);

		try {
			AsyncHttpClient client = SessionMgr.getClient(c);

			RequestParams params = new RequestParams();
			params.put("email", u.getUsername());
			params.put("passwd", u.getPassword());

			client.post(StaticData.URL_LOGON, params, responseHandler);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}
}
