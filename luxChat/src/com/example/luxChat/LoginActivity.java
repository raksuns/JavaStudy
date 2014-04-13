package com.example.luxChat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends Activity {

	private boolean loginSuccess = false;

	private String mUsername;
	private String mPassword;

	// UI references.
	private EditText mUsernameView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);
		
		mUsernameView = (EditText) findViewById(R.id.username);
		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}

	public void attemptLogin() {

		mUsernameView.setError(null);
		mPasswordView.setError(null);

		mUsername = mUsernameView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
		showProgress(true);
		login();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
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
		UserAccount account = new UserAccount(mUsername, mPassword);

		try {
			ChatClient.validateCredentials(account, getApplicationContext(), responseHandler);
		}
		catch(ConnectionException e) {
			loginSuccess = false;
		}
	}

	public void loginSuccess() {
		UserAccount account = new UserAccount(mUsername, mPassword);
		account.storePreferences(LoginActivity.this);

		showProgress(false);

		if (loginSuccess) {
			finish();
		} else {
			mPasswordView.setError("Login Error");
			mPasswordView.requestFocus();
		}

	}
}
