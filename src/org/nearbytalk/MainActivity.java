package org.nearbytalk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

public class MainActivity extends FragmentActivity {

	private static final Logger log = LoggerFactory
			.getLogger(MainActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

	}

	public void onBackPressed() {
		// TODO deal with press when init progress dialog showing

		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
