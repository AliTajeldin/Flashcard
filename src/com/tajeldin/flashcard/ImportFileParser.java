package com.tajeldin.flashcard;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import android.os.Environment;

// TODO: add import file parser testcase.

/**
 * an ImportFileParser is used for parsing the file of phrases to import into
 * the application. The file consists of multiple entries delimited by "--" or
 * "==".
 * 
 * An example file: <code>
 * # comment line here
 * e=English phrase 2
 * s=Spanish phrase 1
 * --
 * i=5  // optional id attribute
 * l=3  // optional level attribute
 * e=ep2 // must always be specified
 * s=sp2 // must always be specified
 * --
 * ...
 * --
 * </code>
 * <ul>
 * <li>the entry attributes may appear in any order within an entry.
 * <li>A single entry may not have the same attribute specified multiple times.
 * <li>the "e" and "s" attributes are required. All others are optional
 * <li>an empty element is legal. two "--" lines in a row for example.
 * <li>a ";" in the text will be translated to a newline.
 * <li>"n~", "a'", "e'", "i'" in Spanish text will translate to accented letter.
 * </ul>
 */
public class ImportFileParser {
	private File _curFile;
	private boolean _gotEOF = false;
	private LineNumberReader _reader = null;
	private long _totalBytesReadEstimate = 0;
	private long _fileLength = 0;

	public ImportFileParser() {
		_curFile = new File(Environment.getExternalStorageDirectory(),
				"AliFlashcard/spanish.txt");
		if (!_curFile.exists()) {
			throwError("File does not exist", null);
		}
		_fileLength = _curFile.length();
		try {
			_reader = new LineNumberReader(new FileReader(_curFile));
		} catch (Exception e) {
			throwError("Unable to open file", e);
		}
	}

	/**
	 * throw exception with filename and current line number added to the error
	 * message.
	 */
	private void throwError(String errMsg, Throwable cause) {
		String lineInfo = " [" + _curFile.toString() + ":";
		if (_reader == null)
			lineInfo += "<unknown>]";
		else
			lineInfo += _reader.getLineNumber() + "]";
		throw new ParserError(errMsg + lineInfo, cause);
	}

	/**
	 * Reads next line from the reader. Keeps track of the estimated bytes read
	 * so far. It is only an estimate because we have no idea what the real line
	 * terminator was in the file (\n or \r\n). This function will return null
	 * and set _gotEOF on end of file.
	 */
	private String getNextLine() {
		String line = null;

		try {
			line = _reader.readLine();
		} catch (IOException e) {
			throwError("Error reading import file", e);
		}

		if (line == null) {
			_gotEOF = true;
			try {
				_reader.close();
				_reader = null;
			} catch (Exception e) {
				throwError("Unable to close file", e);
			}
		} else {
			_totalBytesReadEstimate += line.length() + 1;
		}

		return line;
	}

	/**
	 * parse the next entry in the import file and return the representative
	 * Flashcard object that corresponds to it. Returns null is there are no
	 * further entries in the file.
	 */
	public Flashcard getNextEntry() {
		boolean isEmptyEntry = true;
		if (_gotEOF)
			return null;

		Flashcard fc = Flashcard.acquire();

		while (true) {
			// -- Read the next line of input
			String curLine = getNextLine();

			// -- handle special lines (EOF, end of entry, comment)
			if (_gotEOF) {
				// if we reach EOF with an empty entry, discard empty entry.
				if (isEmptyEntry)
					return null;
				break;
			} else if (curLine.startsWith("--") || curLine.startsWith("==")) {
				// skip empty entries.
				if (isEmptyEntry)
					continue;
				break;
			} else if (curLine.startsWith("#")) {
				// skip comments
				continue;
			}

			// -- handle data lines.
			isEmptyEntry = false;
			if (curLine.startsWith("s=")) {
				fc.setLang2Str(curLine.substring(2));
			} else if (curLine.startsWith("e=")) {
				fc.setLang1Str(curLine.substring(2));
			} else {
				throwError("Unknown line format", null);
			}
		}

		// -- validate data entry (should be separate function).
		if ((fc.getLang1Str() == null) || (fc.getLang2Str() == null)) {
			throwError("Missing phrase in entry", null);
		}

		return fc;
	}

	/**
	 * read a batch of flashcard entries rather than a single entry. Up to count
	 * entries will be read and returned as a linked list.
	 */
	public Flashcard getNextBatch(int count) {
		Flashcard head = null;

		for (int i = 0; i < count; ++i) {
			Flashcard fc = getNextEntry();
			if (fc == null) {
				return head;
			}
			head = Flashcard.Chain.prepend(head, fc);
		}

		return head;
	}

	/**
	 * return an estimate of how much progress has been made reading the file.
	 * 
	 * @return a value in the range 0-100 percent.
	 */
	public int getCompletedPercentEstimate() {
		return (int) ((_totalBytesReadEstimate * 100) / _fileLength);
	}

	/**
	 * The exception class thrown by ImportFileParser.
	 */
	public static class ParserError extends RuntimeException {
		private static final long serialVersionUID = -6950531613013713353L;

		public ParserError(String errMsg, Throwable cause) {
			super(errMsg, cause);
		}
	}

}
