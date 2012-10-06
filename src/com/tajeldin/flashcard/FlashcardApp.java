package com.tajeldin.flashcard;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

/**
 * The main app class. In addition to handling the application (not activity)
 * lifecycle events, a singleton instance of this class will be used to maintain
 * the state of the application. While it is a bit of a global pattern, it is
 * far easier and cleaner than having to sync all application components when
 * state needs to be persisted (e.g. onPause event).
 */
public class FlashcardApp extends Application {

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[APP] onConfigurationChanged() called.");
	}

	@Override
	public void onCreate() {
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[APP] onCreate() called.");

		super.onCreate();

		setInstance(this);
		restoreState();

		// startup the controller and DB components. UI main activity will
		// be started by the system.
		new FlashcardController();
		new FlashcardDB();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[APP] onTerminate() called.");
	}

	private static FlashcardApp _instance = null;

	public static synchronized FlashcardApp getInstance() {
		return _instance;
	}

	private static synchronized void setInstance(FlashcardApp app) {
		if (_instance != null) {
			throw new IllegalStateException(
					"FlashcardApp.setInstance called twice");
		}
		_instance = app;
	}

	private Flashcard _curFlashcard = null;

	public synchronized void setCurrentFlashcard(Flashcard fc) {
		_curFlashcard = fc;
	}

	public synchronized Flashcard getCurrentFlashcard() {
		return _curFlashcard;
	}

	private boolean _showEntireCard = true;

	public boolean isShowEntireCard() {
		return _showEntireCard;
	}

	public void setShowEntireCard(boolean showEntireCard) {
		_showEntireCard = showEntireCard;
	}

	private SharedPreferences getPrefs() {
		return getSharedPreferences("flashcard", MODE_PRIVATE);
	}

	// TODO: add saveState function comment.
	public synchronized void saveState() {
		if (_curFlashcard != null) {
			if (LP.LOG_LIFECYCLE_EVENTS)
				Log.d(LP.TAG, "[APP] saving state");

			SharedPreferences.Editor spe = getPrefs().edit();
			spe.clear();
			spe.putInt("id", _curFlashcard.getID());
			spe.commit();
		}
	}

	// TODO: add restoreState func comment here.
	private void restoreState() {
		SharedPreferences sp = getPrefs();
		int id = sp.getInt("id", 0);
		if (LP.LOG_LIFECYCLE_EVENTS)
			Log.d(LP.TAG, "[APP] restore state id=" + id);
	}

	// TODO: need to add a resetState() method that is called after import or on
	// initialization (sets _showFull to false, id back to 0, level = 0, etc).
	// this func must be synchronized.
}
