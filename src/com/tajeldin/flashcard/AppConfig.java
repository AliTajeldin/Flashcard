package com.tajeldin.flashcard;

// TODO: use a configuration screen to set the params.

/**
 * Application configuration parameters. Encapsulate user modifiable application
 * parameters. Initially, just a bunch of globals. Eventually, this class will
 * utilize a configuration screen.
 * values below.
 */
public class AppConfig {

	/** full path of database file name */
	public static String _dbFullPath = "/sdcard/AliFlashcard/ali.db";

	/** maximum query result set size */
	public static int _maxQuerySetSize = 10;

	/** number of supported levels */
	public static int _numLevels = 3;

	/** size of flashcard pool */
	public static int _fcPoolSize = 20;
}
