package com.tajeldin.flashcard;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

//TODO: fix Force Closes on screen orientation changes. (send continue import to controller).
//TODO: capture "Back" button to do same as cancel.

/**
 * An activity to show the progress of the import operation (for now).
 */
public class ProgressActivity extends Activity {

	private TextView _progressText;
	private ProgressBar _progressBar;
	private Button _progressCancelButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.progress);

		_progressText = (TextView) findViewById(R.id.text_progress);
		_progressBar = (ProgressBar) findViewById(R.id.pb_progress);

		_progressText.setText(ProgressActivityState.progressTitle);

		// TODO: implement cancel functionality.
		// button press on the "Done" button will just discard this activity.
		_progressCancelButton = (Button) findViewById(R.id.button_progress_cancel);
		_progressCancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[PA] OnCreate() called.");

		// save the singleton instance. make sure to remove this instance
		// when this activity is destroyed.
		setInstance(this);

		/*
		 * Send a "progress activity started message" to the controller to kick
		 * start the actual import/export operation. This is done so that the
		 * "progress update" messages will not be received before this activity
		 * has finished initializing.
		 */
		MsgDispatcher.sendMessageToController(
				MsgType.MSG_PROGRESS_ACTIVITY_STARTED, 0, 0, null);
	}

	@Override
	protected void onDestroy() {
		// reset the singleton instance when activity is destroyed.
		setInstance(null);
		super.onDestroy();
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[PA] OnDestroy() called");
	}

	// Maintain a static instance of this class. To avoid leaks, this instance
	// must be removed when the activity is paused.
	private static ProgressActivity _instance = null;

	private static synchronized void setInstance(ProgressActivity instance) {
		// we must either be clearing an old instance value or setting a new
		// instance (that was null).
		if (instance != null && _instance != null) {
			throw new IllegalStateException(
					"setInstance() called twice.  Possible memory leak");
		}
		_instance = instance;
	}

	/**
	 * process the update progress message by update the progress bar. If the
	 * progress state is inactive, then finish (remove) this activity.
	 */
	public static synchronized void updateProgress() {
		int progress = ProgressActivityState.progress;
		boolean isActive = ProgressActivityState.isActive;
		if (_instance != null) {
			if (isActive) {
				_instance._progressBar.setProgress(progress);
			} else {
				_instance.finish();
			}
		}
	}
}
