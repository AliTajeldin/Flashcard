package com.tajeldin.flashcard;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * This controller class contains the business logic for the flashcard app.
 * Users of this class (e.g. UI and DB components) should generally not call
 * methods in this call directly. Rather, messages to this components must
 * be dispatched through the {@link MsgDispatcher}.
 */
public class FlashcardController extends HandlerThread implements
		Handler.Callback {

	private ImportFileParser _importFileParser = null;
	private PrefetchQueue _prefetchQueue[] = null;
	private boolean _waitingForLevelData = false;
	private int _curLevel = 0;

	public FlashcardController() {
		super("controller");
		this.start();

		_prefetchQueue = new PrefetchQueue[AppConfig._numLevels];
		for (int i = 0; i < AppConfig._numLevels; ++i) {
			_prefetchQueue[i] = new PrefetchQueue(i);
		}

		Handler h = new Handler(this.getLooper(), this);
		MsgDispatcher.setControllerHandler(h);
	}

	public boolean handleMessage(Message msg) {
		if (LP.LOG_MESSAGE_EVENTS) {
			Log.d(LP.TAG, "[controller] received message: "
					+ MsgType.getMsgName(msg.what));
		}

		switch (msg.what) {
		case MsgType.MSG_MENU_IMPORT:
			startImport();
			break;
		case MsgType.MSG_CONTINUE_IMPORT:
			continueImport();
			break;
		case MsgType.MSG_PROGRESS_ACTIVITY_STARTED:
			continueImport();
			break;
		case MsgType.MSG_GET_NEXT_FC:
			getNextFlashcardToDisplay();
			break;
		case MsgType.MSG_RESULT_FC_SET:
			handleFlashcardSetResponse((QueryResult) msg.obj);
			break;
		case MsgType.MSG_FC_GUESS_RESULT:
			handleFlashcardGuessResult((Flashcard) msg.obj, msg.arg1 != 0);
			break;
		default:
			Log.e(LP.TAG, "[controller] unknown message type: " + msg.what);
			return false;
		}

		return true;
	}

	/**
	 * handle case where user has indicated a correct guess for a flashcard.
	 * Increment the "right guess count" register and promote if needed.
	 */
	private void handleFlashcardGuessResult(Flashcard fc, boolean isCorrectGuess) {
		if (isCorrectGuess) {
			// right guess.
			int rightCount = fc.getRightGuessCount() + 1;
			// TODO: promote if needed.
			fc.setRightGuessCount(rightCount);
		} else {
			// wrong guess: demote fc back to initial level.
			fc.setLevel(0);
			fc.setRightGuessCount(0);
		}

		// send message to db to update flashcard.
		MsgDispatcher.sendMessageToDB(MsgType.MSG_UPDATE_FC, 0, 0, fc);
	}

	/**
	 * Gets the next appropriate flashcard to be displayed. This function will
	 * attempt to get the card from the right prefetch queue. If a flashcard was
	 * already available, it will be immediately sent to the UI for display.
	 * <p>
	 * On the other hand, if the prefetch queue was empty, then it will be
	 * replenished and the {@link #_waitingForLevelData} flag will be set to
	 * indicate we are waiting for data at this level.
	 */
	private void getNextFlashcardToDisplay() {
		PrefetchQueue q = _prefetchQueue[_curLevel];
		Flashcard fc = q.dequeue();

		if (fc == null) {
			Log.v(LP.TAG, "Need to replenish queue");
			q.replenishQueue();
			_waitingForLevelData = true;
			return;
		}

		Log.v(LP.TAG, "Got fc from prefetch queue: " + fc.getID());
		MsgDispatcher.sendMessageToUI(MsgType.MSG_DISPLAY_FC, 0, 0, fc);
	}

	/**
	 * handle a response of a set of flashcards sent by the db component in
	 * response to an earlier query.
	 */
	private void handleFlashcardSetResponse(QueryResult qr) {
		Log.v(LP.TAG, "[controller] got fc set: c=" + qr.count + " l="
				+ qr.level + " m=" + qr.maxId);
		PrefetchQueue q = _prefetchQueue[qr.level];
		q.enqueueSet(qr);

		// if we were waiting for data at this level, display the flashcard.
		if (_waitingForLevelData && _curLevel == qr.level) {
			_waitingForLevelData = false;
			getNextFlashcardToDisplay();
		}
	}

	/**
	 * start the import process. The actual import takes place in
	 * {@link #continueImport()}. However, that will not happen until we get the
	 * {@link MsgType#MSG_PROGRESS_ACTIVITY_STARTED} from the ProgressActivity
	 * once it has finished initializing itself.
	 */
	private void startImport() {
		// TODO: need to clear all queue caches on import (id's may be different
		// after import)

		ProgressActivityState.startProgress("Import Progress");

		// notify UI to start the import status activity.
		MsgDispatcher.sendMessageToUI(MsgType.MSG_LAUNCH_PROGRESS_ACTIVITY, 0,
				0, null);

		// notify db to prepare to start import.
		MsgDispatcher.sendMessageToDB(MsgType.MSG_DB_START_IMPORT, 0, 0, null);

		_importFileParser = new ImportFileParser();
	}

	/**
	 * send import progress message to the UI. The progress percent is extracted
	 * from the import file parser. If the parser was already removed, the
	 * progress is assumed to be at %100.
	 */
	private void sendImportProgressMsgToUI() {
		if (_importFileParser != null) {
			ProgressActivityState.progress = _importFileParser
					.getCompletedPercentEstimate();
		}
		MsgDispatcher.sendMessageToUI(MsgType.MSG_UPDATE_PROGRESS, 0, 0, null);
	}

	/**
	 * Called to import the next "chunk" from the import file into the database.
	 */
	private void continueImport() {
		if (_importFileParser == null) {
			// handle spurious continue import messages by bailing out on an
			// already finished import.
			return;
		}

		Flashcard fc;
		fc = _importFileParser.getNextBatch(AppConfig._fcPoolSize);
		if (fc == null) {
			// no more flashcard to import. update progress activity and notify
			// db.
			ProgressActivityState.finishProgress();
			_importFileParser = null;
			MsgDispatcher.sendMessageToDB(MsgType.MSG_DB_FINISH_IMPORT, 0, 0,
					null);
		} else {
			MsgDispatcher.sendMessageToDB(MsgType.MSG_INSERT_FC_SET, 0, 0, fc);
		}
		sendImportProgressMsgToUI();
	}
}
