package uk.co.mauvesoft.communicator;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import android.content.Context;
import android.content.SharedPreferences;

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
	
	public void storePreferences(Context c) {
		SharedPreferences prefs = c.getSharedPreferences(ACCOUNT_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor e = prefs.edit();
		
		e.putString(USERNAME_PREFERENCE, username);
		e.putString(PASSWORD_PREFERENCE, password);
		
		e.commit();
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
