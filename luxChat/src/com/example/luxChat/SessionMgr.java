package com.example.luxChat;

import android.content.Context;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;
import org.apache.http.protocol.HttpContext;

/**
 * 로그인 성공 후 HttpContext를 이용하여 세션을 유지하고,
 * loopj에 쿠키를 설정해서 세션을 지속적으로 사용하기 위한 클래스.
 */
public class SessionMgr {

	public static int LONG_POLLING_TIMEOUT = 60 * 1000;

	static public AsyncHttpClient client;
	static public PersistentCookieStore cookieStore;

	public static AsyncHttpClient getClient(Context c) {
		if (client == null) {
			client = new AsyncHttpClient();
			setCookieStore(new PersistentCookieStore(c));
		}

		return client;
	}

	public static void setHttpClient(AsyncHttpClient httpClient) {
		SessionMgr.client = httpClient;
	}

	public static void setCookieStore(PersistentCookieStore cookieStore1) {
		cookieStore = cookieStore1;
		client.setCookieStore(cookieStore);
	}

	public static PersistentCookieStore getCookieStore() {
		return cookieStore;
	}

	public static HttpContext getContext() {
		return client.getHttpContext();
	}
}
