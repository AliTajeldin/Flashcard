package com.tajeldin.flashcard;

// TODO: implement onRetainNonConfigurationInstance for quick rotation (not worth it!)
// actually, we need to do that to preserve currentFC and prevFC.
// TODO: implement res/layout instead of hard coded names in main.xml

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * The main application activity (window). This class is responsible for
 * displaying the English/Spanish words and a state bar at the bottom.
 */
public class MainActivity extends Activity {

	private TextView _topText;
	private TextView _bottomText;
	private TextView _statusText;

	private GestureDetector _gestureDetector;

	// -----------------------------------------------------------------------
	// state of currently displayed flashcard.
	// -----------------------------------------------------------------------
	// TODO: move app state to FlashcardApp class.
	private boolean _waitingForNextCard = false;
	private Flashcard _prevFlashcard = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[MA] OnCreate() called.");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// create the UI handler here so it will be associated with the main
		// UI thread.
		MsgDispatcher.setUiHandler(new UiHandler());

		_topText = (TextView) findViewById(R.id.text_top);
		_bottomText = (TextView) findViewById(R.id.text_bottom);
		_statusText = (TextView) findViewById(R.id.text_status);

		_gestureDetector = new GestureDetector(this, new AxialFlingListener());

		// save the singleton instance. make sure to remove this instance
		// when this activity is destroyed.
		setInstance(this);

		// TODO: send out request for next flashcard to prime the pump.
	}

	// Maintain a static instance of this class. To avoid leaks, this instance
	// must be removed when the activity is paused.
	private static MainActivity _instance = null;

	private static synchronized void setInstance(MainActivity ma) {
		// we must either be clearing an old instance value or setting a new
		// instance (that was null).
		if (ma != null && _instance != null) {
			throw new IllegalStateException(
					"setInstance() called twice.  Possible memory leak");
		}

		_instance = ma;
	}

	/**
	 * statically available method that allows the caller to start the progress
	 * activity in the context of this activity. Must be called from the main UI
	 * thread.
	 */
	static synchronized void launchProgressActivity() {
		if (_instance != null) {
			Intent explicitIntent = new Intent(_instance,
					ProgressActivity.class);
			_instance.startActivity(explicitIntent);
		}
	}

	/**
	 * display the next flashcard to the screen. Called from the
	 * {@link UiHandler}.
	 */
	static synchronized void displayNewFlashcard(Flashcard fc) {
		FlashcardApp.getInstance().setCurrentFlashcard(fc);
		// TODO: do not reset showEntireCard. keep last state from app.
		FlashcardApp.getInstance().setShowEntireCard(false);
		if (_instance != null) {
			_instance._waitingForNextCard = false;
			_instance.updateDisplay();
		}
	}

	/**
	 * update the display to show the current flashcard. Either the entire
	 * flashcard is shown or just the first half depending on the value of the
	 * {@link #_showEntireCard} variable.
	 */
	private void updateDisplay() {
		Flashcard fc = FlashcardApp.getInstance().getCurrentFlashcard();
		if (fc == null) {
			return;
		}

		String lang1Text = fc.getLang1Str();
		String lang2Text = "";
		if (FlashcardApp.getInstance().isShowEntireCard()) {
			lang2Text = fc.getLang2Str();
		}

		_topText.setText(StringFormatter.formatEnglishString(lang1Text));
		_bottomText.setText(StringFormatter.formatSpanishString(lang2Text));
		_statusText.setText("id: " + fc.getID() + "   level: " + fc.getLevel()
				+ "   count: " + fc.getRightGuessCount());
	}

	/**
	 * handle user pressing standard menu button. This will inflate the menu
	 * defined in res/menu/menu.xml.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[MA] OnCreateOptionsMenu() called.");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * handle selection of a menu item from menu.xml.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_import:
			MsgDispatcher.sendMessageToController(MsgType.MSG_MENU_IMPORT, 0,
					0, null);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// TODO: delete onX extra methods.
	// @Override
	// protected void onRestart() {
	// super.onRestart();
	// if (LP.LOG_LIFECYCLE_EVENTS)
	// Log.d(LP.TAG, "[MA] OnRestart() called");
	// }
	//
	// @Override
	// protected void onStart() {
	// super.onStart();
	// if (LP.LOG_LIFECYCLE_EVENTS)
	// Log.d(LP.TAG, "[MA] OnStart() called");
	// }
	//
	// @Override
	// protected void onStop() {
	// super.onStop();
	// if (LP.LOG_LIFECYCLE_EVENTS)
	// Log.d(LP.TAG, "[MA] OnStop() called");
	// }

	@Override
	protected void onPause() {
		super.onPause();
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[MA] OnPause() called");

		FlashcardApp.getInstance().saveState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[MA] OnResume() called");

		_waitingForNextCard = false;
		updateDisplay();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[MA] OnDestroy() called");

		// reset the singleton instance when activity is destroyed.
		setInstance(null);
	}

	/**
	 * handle back button press by showing previous flashcard. If no previous
	 * flashcard is present, then destroy (finish) the activity.
	 */
	@Override
	public void onBackPressed() {
		Log.v(LP.TAG, "[MA] onBackPressed called. "
				+ ((_prevFlashcard == null) ? "p=null" : "notnull"));
		if (_prevFlashcard == null) {
			super.onBackPressed();
		} else {
			FlashcardApp.getInstance().setCurrentFlashcard(_prevFlashcard);
			FlashcardApp.getInstance().setShowEntireCard(true);
			_prevFlashcard = null;
			_waitingForNextCard = false;
			updateDisplay();
		}

	}

	/**
	 * handle a vertical or horizontal fling from within this Activity. If we
	 * have only shown the first half of the current card so far, then the
	 * second half is shown to the user. Otherwise, a new card is displayed.
	 * 
	 * Called by {@link AxialFlingListener} when it detects a vertical or
	 * horizontal fling.
	 */
	private void handleFling(boolean isCorrectGuess) {
		if (FlashcardApp.getInstance().isShowEntireCard()) {
			// if we are already waiting for a card, ignore extra fling.
			if (_waitingForNextCard) {
				return;
			}
			_waitingForNextCard = true;

			MsgDispatcher.sendMessageToController(MsgType.MSG_GET_NEXT_FC, 0,
					0, null);

			Flashcard fc = FlashcardApp.getInstance().getCurrentFlashcard();
			if (fc != null) {
				// save current fc as the previous fc. must clone it as the one
				// we send to update guess count may be released soon.
				if (_prevFlashcard != null)
					_prevFlashcard.release();
				_prevFlashcard = fc.clone();

				int arg1 = isCorrectGuess ? 1 : 0;
				MsgDispatcher.sendMessageToController(
						MsgType.MSG_FC_GUESS_RESULT, arg1, 0, fc);
			}
		} else {
			FlashcardApp.getInstance().setShowEntireCard(true);
			updateDisplay();
		}
	}

	/**
	 * Need to override the OnTouchEvent to forward the MotionEvent to the
	 * gesture detector associated with this activity.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (_gestureDetector.onTouchEvent(event)) {
			return true;
		}
		// handle our own touch even here if needed.
		return false;
	}

	/**
	 * flashcard fling gestures listener. This gesture listener is only
	 * interested in Fling motions. Only horizontal and vertical flings are
	 * detected by ensuring that flings in the primary direction are at least
	 * FLING_RATIO (default twice) as much as the secondary direction. Flings
	 * along the diagonal line are ignored.
	 * 
	 */
	private class AxialFlingListener extends
			GestureDetector.SimpleOnGestureListener {
		private final float FLING_RATIO = 2.0f;

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			float absX = Math.abs(velocityX);
			float absY = Math.abs(velocityY);
			if (absX > absY && absX / absY >= FLING_RATIO) {
				MainActivity.this.handleFling(false);
				return true;
			} else if (absY > absX && absY / absX >= FLING_RATIO) {
				MainActivity.this.handleFling(true);
				return true;
			}
			return false;
		}
	}
}
