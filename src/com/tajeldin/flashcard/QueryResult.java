package com.tajeldin.flashcard;

import android.util.Log;

/**
 * simple class to hold the result of the db query for a set of flashcards. The
 * result chain contained starting at {@link #head} is not necessarily in the
 * same order as the results returned from the database.
 */
class QueryResult {
	int count = 0;
	Flashcard head = null;
	int maxId = 0;
	int level = 0;

	void log(int num) {
		String msg = "[qr" + num + "] level=" + level + " count=" + count
				+ " maxId=" + maxId;
		Log.d(LP.TAG, msg);
	}
}
