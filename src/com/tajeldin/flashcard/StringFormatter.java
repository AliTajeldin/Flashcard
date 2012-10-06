package com.tajeldin.flashcard;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StringFormatter provides helper functions for transforming the raw strings
 * from the database into strings suitable for display.
 * 
 * The database strings are setup for easy entry. For example, the Spanish
 * letter N with ~ on top can be entered as "N~" in the database. For instance,
 * the word for child can be entered as "nin~o" in the database. However, it
 * will be displayed properly on the screen (with just 4 letters).
 * 
 * In addition, this class provides functions for embedding newlines where the
 * special ";" (semicolon) marker appears.
 * 
 * No need to make this synchronized as it will only be accessed by the main ui
 * thread.
 */
public class StringFormatter {

	private static Pattern _p = null;
	private static HashMap<String, String> _map = null;

	private static void init() {
		if (_p != null)
			return;

		_p = Pattern.compile("[aeiouAEIOU]'|[nN]~|;");

		_map = new HashMap<String, String>();
		_map.put("a'", "\u00E1");
		_map.put("e'", "\u00E9");
		_map.put("i'", "\u00ED");
		_map.put("o'", "\u00F3");
		_map.put("u'", "\u00FA");
		_map.put("A'", "\u00C1");
		_map.put("E'", "\u00C9");
		_map.put("I'", "\u00CD");
		_map.put("O'", "\u00D3");
		_map.put("U'", "\u00DA");
		_map.put("n~", "\u00F1");
		_map.put("N~", "\u00D1");
		_map.put(";", "\n");

	}

	/**
	 * English strings only support embedding of newline characters. No other
	 * translation is performed.
	 */
	public static String formatEnglishString(String s) {
		return s.replace(';', '\n');
	}

	/**
	 * Format a Spanish string by substituting accented letters and embed
	 * newlines instead of ";".
	 */
	public static String formatSpanishString(String s) {
		init();

		// TODO: use StringBuilder instead of StringBuffer.
		Matcher m = _p.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String ms = m.group();
			m.appendReplacement(sb, _map.get(ms));
		}
		m.appendTail(sb);

		return sb.toString();
	}
}
