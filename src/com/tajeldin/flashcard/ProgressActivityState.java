package com.tajeldin.flashcard;

/**
 * Maintain the state of the progress activity external to the activity itself.
 * This will help with onCreate/onDestory calls on the activity due to
 * configuration changes.
 * 
 * No need to persist this state on application startup as we never start in
 * the middle of a progress requiring operation (e.g. import or export).
 */
public class ProgressActivityState {

	// the progress activity state.  the fields are volatile to allow for
	// modification/read from two different threads without an sync boundary.
	public static volatile String progressTitle = "";
	public static volatile int progress = 0;
	public static volatile boolean isActive = false;

	/**
	 * helper routine for initializing progress state.
	 */
	public static void startProgress(String title) {
		progressTitle = title;
		progress = 0;
		isActive = true;
	}

	/**
	 * helper routine for resetting progress state.
	 */
	public static void finishProgress() {
		progressTitle = "";
		progress = 0;
		isActive = false;
	}
}
