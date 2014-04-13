package com.example.luxChat;

import android.content.Context;
import android.content.SharedPreferences;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

/**
 * 로그인 정보를 저장하여 계속사용하기 위한 클래스.
 */
public class UserAccount {
	protected static UserAccount instance;
	
	protected String username = null;
	protected String password = null;
	
	protected UserAccount(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public Credentials getCredentials() {
		return new UsernamePasswordCredentials(username, password);
	}
	
	public static final String USERNAME_PREFERENCE = "username";
	public static final String PASSWORD_PREFERENCE = "password";
	public static final String ACCOUNT_PREFERENCES = "account";
	public static final String LATEST_PREFERENCES = "latest";
	
	public void storePreferences(Context c) {
		SharedPreferences prefs = c.getSharedPreferences(ACCOUNT_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor e = prefs.edit();
		
		e.putString(USERNAME_PREFERENCE, username);
		e.putString(PASSWORD_PREFERENCE, password);
		
		e.commit();
	}

	public static void setLatestPreferences(Context c, int talk_room_id, String latest) {
		SharedPreferences prefs = c.getSharedPreferences(LATEST_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor e = prefs.edit();

		e.putString("tid_" + Integer.toString(talk_room_id), latest);

		e.commit();
	}

	public static String getLatestPreferences(Context c, int talk_room_id) {
		SharedPreferences prefs = c.getSharedPreferences(LATEST_PREFERENCES, Context.MODE_PRIVATE);
		String latest = prefs.getString("tid_" + Integer.toString(talk_room_id), null);

		return latest;
	}
	
	public static void clearPreferences(Context c) {
		SharedPreferences prefs = c.getSharedPreferences(ACCOUNT_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor e = prefs.edit();
		
		e.remove(USERNAME_PREFERENCE);
		e.remove(PASSWORD_PREFERENCE);
		
		e.commit();
	}
	
	public static UserAccount fromPreferences(Context c) throws NoStoredPreferences {
		SharedPreferences prefs = c.getSharedPreferences(ACCOUNT_PREFERENCES, Context.MODE_PRIVATE);
		String username = prefs.getString(USERNAME_PREFERENCE, null);
		String password = prefs.getString(PASSWORD_PREFERENCE, null);
		
		if (username == null || password == null) {
			throw new NoStoredPreferences();
		}
		
		return new UserAccount(username, password);
	}
}
