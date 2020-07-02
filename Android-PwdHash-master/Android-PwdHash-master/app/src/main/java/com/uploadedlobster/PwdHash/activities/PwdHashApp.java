/**
 * PwdHash, PwdHashApp.java
 * A password hash implementation for Android.
 *
 * Copyright (c) 2010 - 2013 Philipp Wolfer
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the RBrainz project nor the names of the
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * @author Philipp Wolfer <ph.wolfer@gmail.com>
 */

package com.uploadedlobster.PwdHash.activities;

import java.util.ArrayList;
import java.util.stream.*;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.uploadedlobster.PwdHash.R;
import com.uploadedlobster.PwdHash.algorithm.DomainExtractor;
import com.uploadedlobster.PwdHash.algorithm.HashedPassword;
import com.uploadedlobster.PwdHash.storage.HistoryDataSource;
import com.uploadedlobster.PwdHash.storage.HistoryOpenHelper;
import com.uploadedlobster.PwdHash.storage.UpdateHistoryTask;
import com.uploadedlobster.PwdHash.util.Preferences;
import com.uploadedlobster.PwdHash.activities.monitor;
import org.bouncycastle.Monitor;
import java.security.Provider;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.text.DecimalFormat;
import android.Manifest;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.os.Build;


/**
 * @author Philipp Wolfer <ph.wolfer@gmail.com>
 *
 */
public class PwdHashApp extends Activity {


	private Preferences mPreferences;
	private HistoryDataSource mHistory;
	
	private AutoCompleteTextView mSiteAddress;
	private EditText mPassword;
	private TextView mHashedPassword;
	private Button mCopyBtn;
	private String[] tempos;
	private float sum;
	private long startMemory;
    private long endMemory;

	private boolean mSaveStateOnExit = true;

	private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 1;
	private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 2;


	public void requestPermissionForReadExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void requestPermissionForWriteExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

	public boolean checkPermissionForReadExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public boolean checkPermissionForWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

	public static void setupBouncyCastle() {

        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
		}

		setContentView(R.layout.main);

		mSiteAddress = (AutoCompleteTextView) findViewById(R.id.siteAddress);
		mPassword = (EditText) findViewById(R.id.password);
		mHashedPassword = (TextView) findViewById(R.id.hashedPassword);
		mCopyBtn = (Button) findViewById(R.id.copyBtn);

		mPreferences = new Preferences(this);
		mHistory = new HistoryDataSource(this);
		
		setWindowGeometry();
		restoreSavedState();
		handleIntents();
		registerEventListeners();
		initAutoComplete();

		if (checkPermissionForWriteExternalStorage() == false || checkPermissionForReadExternalStorage() == false) {
			try {
				requestPermissionForWriteExternalStorage();
				requestPermissionForReadExternalStorage();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		setupBouncyCastle();
		startMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		if (mSaveStateOnExit) {
			mPreferences.setSavedSiteAddress(getDomain());
		}
		else {
			mPreferences.setSavedSiteAddress("");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		long statusMemory;
		sum = Monitor.writeTimes("", "SUM");
		endMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        statusMemory = endMemory - startMemory;
     

        if (sum > 15000000) {
            System.out.println("[M] Tempo de execução: O sistema preparou a build...");
        }
        else {
        sum = sum/1000000;
        statusMemory = statusMemory/1000;
        DecimalFormat df = new DecimalFormat("#.####");
        DecimalFormat df2 = new DecimalFormat("#.####");
        String iString = df.format(sum);
        String iString2 = df2.format(statusMemory);
        System.out.println("[M] Tempo de execução: " + iString + " ms\n");
        System.out.println("[M] Memória: " + iString2 + " KB\n");
        }

		Monitor.writeTimes("", "CLEAN");
	}

	private void setWindowGeometry() {
		Window window = getWindow();
		window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		int maxWidth = getResources().getDimensionPixelSize(
				R.dimen.maxWindowWidth);

		if (metrics.widthPixels > maxWidth) {
			window.setLayout(maxWidth, LayoutParams.WRAP_CONTENT);
		}
	}

	private void restoreSavedState() {
		String savedSiteAddress = mPreferences.getSavedSiteAddress();
		
		if (!savedSiteAddress.equals("")) {
			mSiteAddress.setText(savedSiteAddress);
		}
	}

	private void handleIntents() {
		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();
			if (action != null && action.equals(Intent.ACTION_SEND)) {
				String siteAddress = intent.getStringExtra(Intent.EXTRA_TEXT);
				if (siteAddress != null && !siteAddress.equals("")) {
					siteAddress = DomainExtractor.extractDomain(siteAddress);
					mSiteAddress.setText(siteAddress);
					mPassword.requestFocus();
				}
			}
		}
	}

	private void initAutoComplete() {
		mHistory.open();
		String[] from = new String[] { HistoryOpenHelper.COLUMN_REALM };
		int[] to = new int[] { android.R.id.text1 };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_dropdown_item_1line, null, from, to, 0);

		// Set the CursorToStringConverter, to provide the labels for the
		// choices to be displayed in the AutoCompleteTextView.
		adapter.setCursorToStringConverter(new CursorToStringConverter() {
			public String convertToString(android.database.Cursor cursor) {
				final int columnIndex = cursor
						.getColumnIndexOrThrow(HistoryOpenHelper.COLUMN_REALM);
				return cursor.getString(columnIndex);
			}
		});

		// Set the FilterQueryProvider, to run queries for choices
		// that match the specified input.
		adapter.setFilterQueryProvider(new FilterQueryProvider() {
			public Cursor runQuery(CharSequence constraint) {
				String partialInput = (constraint != null ? constraint
						.toString() : "");
				return mHistory.getHistoryCursor(partialInput);
			}
		});

		mSiteAddress.setAdapter(adapter);
	}

	private void registerEventListeners() {
		TextWatcher updatePasswordTextWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String realm = getDomain();
				String password = mPassword.getText().toString();
				
				updateHashedPassword(realm, password);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {}
		};
		mSiteAddress.addTextChangedListener(updatePasswordTextWatcher);
		mPassword.addTextChangedListener(updatePasswordTextWatcher);

		mCopyBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String realm = getDomain();
				String password = mPassword.getText().toString();

				if (realm.equals("")) {
					mSiteAddress.requestFocus();
				} else if (password.equals("")) {
					mPassword.requestFocus();
				} else {
					String hashedPassword = updateHashedPassword(realm, password);
	
					if (!hashedPassword.equals("")) {
						new UpdateHistoryTask(mHistory).execute(realm);
						copyToClipboard(hashedPassword);
						CharSequence clipboardNotification = getString(R.string.copiedToClipboardNotification);
						showNotification(clipboardNotification);
						mSaveStateOnExit = false;
						finish();
					}
				}
			}
		});
	}
	
	private String getDomain() {
		return DomainExtractor.extractDomain(
			mSiteAddress.getText().toString());
	}

	private String updateHashedPassword(String realm, String password) {
		String result = "";
		
		if (!realm.equals("") && !password.equals("")) {
			HashedPassword hashedPassword = HashedPassword.create(password, realm);
			result = hashedPassword.toString();
		}
		
		if (result.equals(""))
			mCopyBtn.setEnabled(false);
		else
			mCopyBtn.setEnabled(true);
		
		mHashedPassword.setText(result);
		return result;
	}

	private void showNotification(CharSequence text) {
		int duration = Toast.LENGTH_LONG;
		Toast toast = Toast.makeText(this, text, duration);
		toast.show();
	}

	@SuppressWarnings("deprecation")
	private void copyToClipboard(String hashedPassword) {
		try {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("", hashedPassword);
				clipboard.setPrimaryClip(clip);
			}
			else {
				// android.text.ClipboardManager is deprecated since API level 11, but we need it in order to be backward compatible.
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				clipboard.setText(hashedPassword);
			}
		}
		catch (IllegalStateException e) {
			// Workaround for some Android 4.3 devices, where writing to the clipboard manager raises an exception
			// if there is an active clipboard listener.
			Log.w("PwdHashApp", "IllegalStateException raised when accessing clipboard.");
		}
	}
}
