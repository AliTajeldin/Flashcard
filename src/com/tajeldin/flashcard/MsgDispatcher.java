package com.tajeldin.flashcard;

import android.os.Handler;
import android.os.Message;

/**
 * Message dispatcher for sending messages to the three main components of the
 * application (UI, controller, db). The setXHandler/sendMessageToX methods are
 * synchronized due to java memory model constraints rather than a real need for
 * mutual exclusion. This should not prove to be a problem in practice.
 */
public class MsgDispatcher {

	// the three message handler to send messages to.
	private static Handler _uiHandler;
	private static Handler _controllerHandler;
	private static Handler _dbHandler;

	public static synchronized void setUiHandler(Handler h) {
		_uiHandler = h;
	}

	public static synchronized void setControllerHandler(Handler h) {
		_controllerHandler = h;
	}

	public static synchronized void setDbHandler(Handler h) {
		_dbHandler = h;
	}

	public static synchronized boolean sendMessageToUI(int w, int a1, int a2,
			Object o) {
		return sendMessageToHandler(_uiHandler, w, a1, a2, o);
	}

	public static synchronized boolean sendMessageToController(int w, int a1,
			int a2, Object o) {
		return sendMessageToHandler(_controllerHandler, w, a1, a2, o);
	}

	public static synchronized boolean sendMessageToDB(int w, int a1, int a2,
			Object o) {
		return sendMessageToHandler(_dbHandler, w, a1, a2, o);
	}

	private static boolean sendMessageToHandler(Handler h, int w, int a1,
			int a2, Object o) {
		if (h != null) {
			return h.sendMessage(Message.obtain(h, w, a1, a2, o));
		} else {
			return false;
		}
	}
}
