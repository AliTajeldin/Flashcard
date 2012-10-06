package com.tajeldin.flashcard;

/**
 * class to encapsulate all known message types being communicated between
 * application components.
 */
public class MsgType {

	/**
	 * message sent from UI to controller to tell it to send back the next FC.
	 * <ul>
	 * <li>NO_ARGS.
	 */
	public static final int MSG_GET_NEXT_FC = 100;

	/**
	 * message sent from UI to controller to update results of FC.
	 * <ul>
	 * <li>arg1 = 1 if correct guess, 0 for wrong guess.
	 * <li>obj = flashcard object to update (ownership passed to controller).
	 */
	public static final int MSG_FC_GUESS_RESULT = 101;

	/**
	 * message sent from UI to controller to handle import menu item.
	 * <ul>
	 * <li>NO_ARGS.
	 */
	public static final int MSG_MENU_IMPORT = 200;

	/**
	 * message sent from db to controller to telling it to continue with current
	 * import operation (i.e. send more flashcard sets to db).
	 * <ul>
	 * <li>NO_ARGS.
	 */
	public static final int MSG_CONTINUE_IMPORT = 201;

	/**
	 * message sent from controller to db to tell it to prepare for impending
	 * import operation.
	 * <ul>
	 * <li>NO_ARGS.
	 */
	public static final int MSG_DB_START_IMPORT = 300;

	/**
	 * message sent from controller to db to insert the given FC chain into
	 * database.
	 * <ul>
	 * <li>obj = flashcard chain to insert.
	 */
	public static final int MSG_INSERT_FC_SET = 301;

	/**
	 * message sent from controller (PrefetchQueue) to db to query for the next
	 * set of flashcards.
	 * <ul>
	 * <li>arg1 = level to query for.
	 * <li>arg2 = next id to query for.
	 */
	public static final int MSG_QUERY_FC_SET = 302;

	/**
	 * message sent from db to controller in response to a query request.
	 * <ul>
	 * <li>obj = QueryResult instance containing the result of the query.
	 */
	public static final int MSG_RESULT_FC_SET = 303;

	/**
	 * message sent from controller to db to update the given FC in the
	 * database.
	 * <ul>
	 * <li>obj = FC to be updated. ownership of FC is transfered to DB.
	 */
	public static final int MSG_UPDATE_FC = 304;

	/**
	 * message sent from controller to db to notify db that the import operation
	 * has completed.
	 * <ul>
	 * <li>NO_ARGS.
	 */
	public static final int MSG_DB_FINISH_IMPORT = 305;

	/**
	 * message sent from controller to UI to inform it to start the progress
	 * activity.
	 * <ul>
	 * <li>NO_ARGS.
	 */
	public static final int MSG_LAUNCH_PROGRESS_ACTIVITY = 400;

	/**
	 * message sent from controller to UI to update the progress activity. The
	 * actual progress value is stored in the static ProgressActiviyState class.
	 * <ul>
	 * <li>NO_ARGS.
	 */
	public static final int MSG_UPDATE_PROGRESS = 401;

	/**
	 * message sent from UI to controller when the progress activity has
	 * finished initializing.
	 * <ul>
	 * <li>NO_ARGS.
	 */
	public static final int MSG_PROGRESS_ACTIVITY_STARTED = 402;

	/**
	 * message sent from controller to ui to tell it to display the given FC to
	 * the user.
	 * <ul>
	 * <li>obj = FC to be displayed.
	 */
	public static final int MSG_DISPLAY_FC = 403;

	/**
	 * message sent from anyone to ui to display an error message and log it.
	 * <ul>
	 * <li>obj = string error message to be displayed and logged.
	 */
	public static final int MSG_SHOW_ERROR_MSG = 404;

	/**
	 * convert internal message number to message name. Mostly used for
	 * logging/debugging purposes.
	 */
	public static String getMsgName(int msgType) {
		switch (msgType) {

		case MSG_GET_NEXT_FC:
			return "MSG_GET_NEXT_FC";
		case MSG_FC_GUESS_RESULT:
			return "MSG_FC_GUESS_RESULT";

		case MSG_MENU_IMPORT:
			return "MSG_MENU_IMPORT";
		case MSG_CONTINUE_IMPORT:
			return "MSG_CONTINUE_IMPORT";

		case MSG_DB_START_IMPORT:
			return "MSG_DB_START_IMPORT";
		case MSG_INSERT_FC_SET:
			return "MSG_INSERT_FC_SET";
		case MSG_QUERY_FC_SET:
			return "MSG_QUERY_FC_SET";
		case MSG_RESULT_FC_SET:
			return "MSG_RESULT_FC_SET";
		case MSG_UPDATE_FC:
			return "MSG_UPDATE_FC";
		case MSG_DB_FINISH_IMPORT:
			return "MSG_DB_FINISH_IMPORT";

		case MSG_LAUNCH_PROGRESS_ACTIVITY:
			return "MSG_LAUNCH_PROGRESS_ACTIVITY";
		case MSG_UPDATE_PROGRESS:
			return "MSG_UPDATE_PROGRESS";
		case MSG_PROGRESS_ACTIVITY_STARTED:
			return "MSG_PROGRESS_ACTIVITY_STARTED";
		case MSG_DISPLAY_FC:
			return "MSG_DISPLAY_FC";
		case MSG_SHOW_ERROR_MSG:
			return "MSG_SHOW_ERROR_MSG";

		default:
			return "[unknown]";
		}
	}
}
