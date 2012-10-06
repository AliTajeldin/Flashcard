package com.tajeldin.flashcard;

// TODO: handle case where sdcard is not present.

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * The database component handler. It is a HandlerThread so it has its own
 * thread, looper and handler. All database related operations should be done by
 * this class.
 */
public class FlashcardDB extends HandlerThread implements Handler.Callback {

	private static final String TBL_FC = "FC";
	private static final String TBL_FCS = "FCS";
	private static final String COL_ID = "ID";
	private static final String COL_LANG1 = "LANG1";
	private static final String COL_LANG2 = "LANG2";
	private static final String COL_LEVEL = "LEVEL";
	private static final String COL_COUNT = "COUNT";
	private static final String COL_FC_ID = TBL_FC + "." + COL_ID;
	private static final String COL_FC_LANG1 = TBL_FC + "." + COL_LANG1;
	private static final String COL_FC_LANG2 = TBL_FC + "." + COL_LANG2;
	private static final String COL_FCS_LANG1 = TBL_FCS + "." + COL_LANG1;
	private static final String COL_FCS_LEVEL = TBL_FCS + "." + COL_LEVEL;
	private static final String COL_FCS_COUNT = TBL_FCS + "." + COL_COUNT;

	private static final String SQL_CREATE_FC_TBL = "CREATE TABLE " + TBL_FC
			+ " ( " + COL_ID + " integer primary key," + COL_LANG1 + " text, "
			+ COL_LANG2 + " text)";

	private static final String SQL_CREATE_FCS_TBL = "CREATE TABLE " + TBL_FCS
			+ " ( " + COL_LANG1 + " text primary key, " + COL_LEVEL
			+ " integer, " + COL_COUNT + " integer )";

	private static final String SQL_INSERT_FC = "INSERT INTO " + TBL_FC
			+ " VALUES (?,?,?)";

	private static final String SQL_INSERT_FCS = "INSERT INTO " + TBL_FCS
			+ " VALUES (?,?,?)";

	private static final String SQL_DELETE_FCS = "DELETE FROM " + TBL_FCS
			+ "  WHERE " + COL_LANG1 + "=?";

	private static final String SQL_QUERY2 = "SELECT " + COL_FC_ID + ","
			+ COL_FC_LANG1 + "," + COL_FC_LANG2 + "," + COL_FCS_LEVEL + ","
			+ COL_FCS_COUNT + " FROM " + TBL_FC + " LEFT JOIN " + TBL_FCS
			+ " ON " + COL_FC_LANG1 + "=" + COL_FCS_LANG1 + " WHERE "
			+ COL_FC_ID + " >= ? LIMIT " + AppConfig._maxQuerySetSize;

	private SQLiteDatabase _curDB = null;
	private HashMap<String, SQLiteStatement> _compiledStatements = null;
	private ImportState _importState = null;

	public FlashcardDB() {
		super("db");
		this.start();

		Handler h = new Handler(this.getLooper(), this);
		MsgDispatcher.setDbHandler(h);
	}

	public boolean handleMessage(Message msg) {
		if (LP.LOG_MESSAGE_EVENTS) {
			Log.d(LP.TAG, "[db] received message: "
					+ MsgType.getMsgName(msg.what));
		}

		switch (msg.what) {
		case MsgType.MSG_DB_START_IMPORT:
			startImport();
			break;
		case MsgType.MSG_DB_FINISH_IMPORT:
			finishImport();
			break;
		case MsgType.MSG_INSERT_FC_SET:
			insertFlashcardSet((Flashcard) msg.obj);
			break;
		case MsgType.MSG_QUERY_FC_SET:
			queryFlashcardSet(msg.arg1, msg.arg2);
			break;
		case MsgType.MSG_UPDATE_FC:
			updateFlashcard((Flashcard) msg.obj);
			break;
		default:
			Log.e(LP.TAG, "[db] unknown message type: " + msg.what);
			return false;
		}

		return true;
	}

	/**
	 * reset the database by removing all data in the flashcard table. The state
	 * table is left intact as we want to keep our current levels and counts.
	 */
	private void startImport() {
		openDB();
		_curDB.delete(TBL_FC, null, null);
		_importState = new ImportState();
	}

	/**
	 * finish up import by cleaning up import state.
	 */
	private void finishImport() {
		_importState = null;
	}

	/**
	 * update the level and count for the given flashcard in the database.
	 */
	private void updateFlashcard(Flashcard fc) {
		openDB();

		_curDB.beginTransaction();
		try {
			// Delete old entry if any.
			SQLiteStatement dStmt = getCompiledStatement(SQL_DELETE_FCS);
			dStmt.bindString(1, fc.getLang1Str());
			dStmt.execute();

			// insert new state entry.
			SQLiteStatement iStmt = getCompiledStatement(SQL_INSERT_FCS);
			iStmt.bindString(1, fc.getLang1Str());
			iStmt.bindLong(2, fc.getLevel());
			iStmt.bindLong(3, fc.getRightGuessCount());
			iStmt.execute();
			_curDB.setTransactionSuccessful();
		} catch (SQLException e) {
			MsgDispatcher.sendMessageToUI(MsgType.MSG_SHOW_ERROR_MSG, 0, 0,
					"unable to update id=" + fc.getID());
		} finally {
			_curDB.endTransaction();
		}

		// this flashcard is no longer used by anyone so release it.
		fc.release();
	}

	/**
	 * Convert the current cursor row to a flashcard object.
	 */
	private Flashcard cursorToFlashcard(Cursor c) {
		Flashcard fc = Flashcard.acquire();
		fc.setID(c.getInt(0));
		fc.setLang1Str(c.getString(1));
		fc.setLang2Str(c.getString(2));
		fc.setLevel(c.getInt(3));
		fc.setRightGuessCount(c.getInt(4));
		return fc;
	}

	/**
	 * the actual implementation of query. This just does a straight attempt to
	 * query for up to {@link #requiredCount} flashcards from the table starting
	 * at the given id and level. This function will not attempt to wrap to the
	 * beginning of the table if insufficient results were found.
	 * 
	 * The results of this query will be added to the passed in
	 * {@link QueryResult} object.
	 */
	private void queryNoWrap(QueryResult qr, int level, int minId,
			int requiredCount) {
		Cursor c = _curDB.rawQuery(SQL_QUERY2, new String[] { "" + minId });
		if (c.moveToFirst()) {
			do {
				// extract fc from cursor and prepend to result chain.
				Flashcard fc = cursorToFlashcard(c);
				qr.head = Flashcard.Chain.append(qr.head, fc);

				if (fc.getID() > qr.maxId)
					qr.maxId = fc.getID();
				++qr.count;
			} while (c.moveToNext() && qr.count < requiredCount);
		}
		c.close();
	}

	/**
	 * perform a query of the database for the next set of flashcards at the
	 * given level and starting at given id. At first, a query from the current
	 * minId is performed. If insufficient results are returned, then a query
	 * from the beginning of the table is tried.
	 */
	private void queryFlashcardSet(int level, int minId) {
		openDB();
		int requiredCount = AppConfig._maxQuerySetSize;
		QueryResult qr = new QueryResult();
		qr.level = level;

		// perform the first query. If insufficient data was returned,
		// perform a second query from the beginning of the table.
		queryNoWrap(qr, level, minId, requiredCount);
		qr.log(0);
		if (qr.count < requiredCount && minId > 1) {
			qr.maxId = 0;
			queryNoWrap(qr, level, 0, requiredCount);
			qr.log(1);
		}

		// send the query result (potentially partial] to controller.
		MsgDispatcher.sendMessageToController(MsgType.MSG_RESULT_FC_SET, 0, 0,
				qr);
	}

	/**
	 * Insert a set of flashcards (linked list) in a single transaction.
	 */
	private void insertFlashcardSet(Flashcard head) {
		openDB();
		SQLiteStatement stmt = getCompiledStatement(SQL_INSERT_FC);
		boolean gotError = false;

		_curDB.beginTransaction();
		try {
			for (Flashcard fc = head; fc != null; fc = Flashcard.Chain
					.getNext(fc)) {

				stmt.bindLong(1, _importState.getRandomId());
				stmt.bindString(2, fc.getLang1Str());
				stmt.bindString(3, fc.getLang2Str());
				stmt.execute();
			}
			_curDB.setTransactionSuccessful();
		} catch (SQLException e) {
			gotError = true;
			MsgDispatcher.sendMessageToUI(MsgType.MSG_SHOW_ERROR_MSG, 0, 0,
					"unable to insert fc");
		} finally {
			_curDB.endTransaction();
		}

		Flashcard.Chain.releaseChain(head);

		// tell controller to send us more data if insert went fine.
		if (!gotError) {
			MsgDispatcher.sendMessageToController(MsgType.MSG_CONTINUE_IMPORT,
					0, 0, null);
		}
	}

	/**
	 * Determines if the database file already exists.
	 */
	private boolean dbFileExists() {
		return new File(AppConfig._dbFullPath).exists();
	}

	/**
	 * create the database schema (tables). Assumes database is already open.
	 */
	private void createSchema() {
		try {
			_curDB.execSQL(SQL_CREATE_FC_TBL);
			_curDB.execSQL(SQL_CREATE_FCS_TBL);
		} catch (SQLException e) {
			MsgDispatcher.sendMessageToUI(MsgType.MSG_SHOW_ERROR_MSG, 0, 0,
					"unable to create schema");
		}
	}

	/**
	 * open the database. The database and schema will be created if they are
	 * not there already.
	 */
	private void openDB() {
		if (_curDB == null) {
			boolean dbExists = dbFileExists();
			_curDB = SQLiteDatabase.openDatabase(AppConfig._dbFullPath, null,
					SQLiteDatabase.CREATE_IF_NECESSARY);
			if (!dbExists) {
				createSchema();
			}
		}
	}

	/**
	 * Maintain a cache of pre-compiled SQL statements. The cached statements
	 * should be destroyed once the database is closed.
	 */
	private SQLiteStatement getCompiledStatement(String stmtStr) {
		if (_compiledStatements == null) {
			_compiledStatements = new HashMap<String, SQLiteStatement>();
		}

		SQLiteStatement stmt = _compiledStatements.get(stmtStr);
		if (stmt == null) {
			stmt = _curDB.compileStatement(stmtStr);
			_compiledStatements.put(stmtStr, stmt);
		}
		return stmt;
	}

	// TODO: need to call closeDB when app is destroyed.
	// private void closeDB() {
	// XXX: no more updateStmt/insertStmt. Need to close statements in
	// _compiledStatements values.
	// if (_updateStmt != null) {
	// _updateStmt.close();
	// }
	// _updateStmt = null;
	//
	// if (_insertStmt != null) {
	// _insertStmt.close();
	// }
	// _insertStmt = null;
	//
	// if (_curDB != null) {
	// _curDB.close();
	// }
	// _curDB = null;
	// }

	private static class ImportState {
		private HashSet<Integer> _importedIds = new HashSet<Integer>(1000);
		private Random _random = new Random();

		/**
		 * generate a random id value in the range [1,MAX_INT) that has not
		 * already been generated during this import cycle.
		 */
		public int getRandomId() {
			Integer r;
			do {
				// TODO: reset max to MAX_VALUE
//				r = _random.nextInt(Integer.MAX_VALUE - 2) + 1;
				r = _random.nextInt(99999) + 1;
			} while (!_importedIds.add(r));
			return r.intValue();
		}
	}
}
