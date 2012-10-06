package com.tajeldin.flashcard;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * UiHandler handles messages sent to the UI component of the application. The
 * UiHandler instance must be created from an Activity running in the main
 * thread. That will ensure that this handler will bind to the main thread
 * looper. The {@link #handleMessage(Message)} method will therefore be called
 * in the context of the main thread and can directly or indirectly perform UI
 * operations.
 */
public class UiHandler extends Handler {

	@Override
	public void handleMessage(Message msg) {
		if (LP.LOG_MESSAGE_EVENTS) {
			Log.d(LP.TAG, "[ui] received message: "
					+ MsgType.getMsgName(msg.what));
		}

		switch (msg.what) {
		case MsgType.MSG_LAUNCH_PROGRESS_ACTIVITY:
			MainActivity.launchProgressActivity();
			break;
		case MsgType.MSG_DISPLAY_FC:
			MainActivity.displayNewFlashcard((Flashcard) msg.obj);
			break;
		case MsgType.MSG_UPDATE_PROGRESS:
			ProgressActivity.updateProgress();
			break;
		case MsgType.MSG_SHOW_ERROR_MSG:
			Log.e(LP.TAG, (String) msg.obj);
			// TODO: show error message in dialog.
			break;
		default:
			Log.e(LP.TAG, "[ui] unknown message type: " + msg.what);
		}
	}

}
