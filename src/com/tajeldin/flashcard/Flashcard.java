package com.tajeldin.flashcard;

// TODO: create a screen to show the flashcard cache hit rate.

/**
 * Flashcard holds one flashcard entry. This class is not MT safe but that
 * should be OK in practice as we never simultaneously access the same flashcard
 * entry from multiple threads.
 * 
 * This class also implements a pool of flashcard objects. Users of this class
 * should never call the constructor directly, but rather use the
 * {@link #acquire()} method to get a Flashcard instance. Once the user is done
 * with the flashcard instance, {@link #release()} should be called.
 */
public class Flashcard implements Cloneable {
	private int _id;
	private String _lang1Str;
	private String _lang2Str;
	private int _level;
	private int _rightGuessCount;

	// internal data for maintaining linked lists of flashcards and caches.
	private boolean _inUse;
	private Flashcard _next;

	// a pool of flashcard objects.
	private final static Object _poolSync = new Object();
	private static int _poolSize = 0;
	private static Flashcard _pool = null;

	// pool use statistics. public for easy access from test framework.
	public static int _poolAcquireCount = 0;
	public static int _poolAcquireHit = 0;
	public static int _poolReleaseCount = 0;
	public static int _poolReleaseHit = 0;

	// private constructor so caller must use acquire() method
	private Flashcard() {
		init();
	}

	private void init() {
		_inUse = false;
		_id = -1;
		_lang1Str = null;
		_lang2Str = null;
		_level = 0;
		_rightGuessCount = 0;
	}

	// -----------------------------------------------------------------------
	// cache pool of previously used flashcards.
	// -----------------------------------------------------------------------

	/**
	 * get an instance of Flashcard from the pool. If no instances already exist
	 * in the pool, a new Flashcard is created.
	 */
	public static Flashcard acquire() {
		Flashcard fc;
		synchronized (_poolSync) {
			++_poolAcquireCount;
			if (_poolSize == 0) {
				fc = new Flashcard();
			} else {
				++_poolAcquireHit;
				fc = _pool;
				_pool = fc._next;
				--_poolSize;
			}
		}
		if (fc._inUse || fc._lang1Str != null || fc._lang2Str != null
				|| fc._level != 0 || fc._rightGuessCount != 0) {
			throw new IllegalStateException("Flashcard in pool was in use");
		}
		fc._next = null;
		fc._inUse = true;
		return fc;
	}
	
	/**
	 * Override default shallow clone method with a method that acquires the
	 * new instance from the FC pool.
	 */
	@Override
	public Flashcard clone() {
		Flashcard fc = acquire();
		fc._id = this._id;
		fc._lang1Str = this._lang1Str;
		fc._lang2Str = this._lang2Str;
		fc._level = this._level;
		fc._rightGuessCount = this._rightGuessCount;
		return fc;
	}

	/**
	 * release this Flashcard back into the pool. If the pool is already at
	 * capacity, then this method is a no-op (this flashcard will eventually be
	 * cleared up by the GC).
	 * 
	 * Caller must not use this instance once it has been released.
	 */
	public void release() {
		if (!_inUse) {
			throw new IllegalStateException("releasing Flashcard not in use");
		}

		init();

		synchronized (_poolSync) {
			++_poolReleaseCount;
			if (_poolSize < AppConfig._fcPoolSize) {
				++_poolReleaseHit;
				_next = _pool;
				_pool = this;
				++_poolSize;
			}
		}
	}

	public static int getPoolSize() {
		synchronized (_poolSync) {
			return _poolSize;
		}
	}

	// -----------------------------------------------------------------------
	// generated getters/setters
	// -----------------------------------------------------------------------

	public String getLang1Str() {
		return _lang1Str;
	}

	public void setLang1Str(String lang1Str) {
		_lang1Str = lang1Str;
	}

	public String getLang2Str() {
		return _lang2Str;
	}

	public void setLang2Str(String lang2Str) {
		_lang2Str = lang2Str;
	}

	public int getID() {
		return _id;
	}

	public void setID(int id) {
		_id = id;
	}

	public int getLevel() {
		return _level;
	}

	public void setLevel(int level) {
		_level = level;
	}

	public int getRightGuessCount() {
		return _rightGuessCount;
	}

	public void setRightGuessCount(int count) {
		_rightGuessCount = count;
	}

	// -----------------------------------------------------------------------
	// chain related functions. These functions are enclosed in the "Chain"
	// name space to make it obvious to caller that we are not dealing with
	// a single fc, but a chain of fc's.
	// -----------------------------------------------------------------------

	public static class Chain {

		/**
		 * release a chain of flashcards to the pool.
		 */
		public static void releaseChain(Flashcard head) {
			while (head != null) {
				Flashcard next = head._next;
				head.release();
				head = next;
			}
		}

		/**
		 * find last flashcard in chain.
		 * 
		 * WARNING: this is an O(n) operation.
		 */
		public static Flashcard tail(Flashcard head) {
			if (head == null)
				return null;
			Flashcard tail = head;
			while (tail._next != null) {
				tail = tail._next;
			}
			return tail;
		}

		/**
		 * returns number of elements in fc chain.
		 * 
		 * WARNING: this is an O(n) operation.
		 */
		public static int length(Flashcard head) {
			int len = 0;
			while (head != null) {
				head = head._next;
				++len;
			}
			return len;
		}

		/**
		 * prepends the given node to the given chain. Returns the new head of
		 * the chain. This allows this function to be used as follows:
		 * <p>
		 * <code>
		 * head = Flashcard.Chain.prepend(head, node);
		 * </code>
		 * 
		 * @param head
		 *            head of chain where node will be prepended. Can be null.
		 * @param node
		 *            node to prepend to chain. Must not be null.
		 */
		public static Flashcard prepend(Flashcard head, Flashcard node) {
			node._next = head;
			return node;
		}

		/**
		 * appends one flashcard chain to another. Either chain can be null. The
		 * resulting chain is returned.
		 * 
		 * WARNING: this is an O(n) operation.
		 */
		public static Flashcard append(Flashcard head1, Flashcard head2) {
			if (head1 == null)
				return head2;
			if (head2 == null)
				return head1;
			Flashcard tail1 = tail(head1);
			tail1._next = head2;
			return head1;
		}

		/**
		 * return next element of given node. Implemented as static to make it
		 * obvious we are using given fc as a node in a chain.
		 */
		public static Flashcard getNext(Flashcard node) {
			return node._next;
		}
		
		/**
		 * removes the first element in the chain and returns the new chain.
		 * The given chain head can be null.
		 * <p>
		 * The previous head flashcard node is detached from the chain by setting
		 * its next pointer to null.  If the caller still needs the old head node,
		 * it must keep a pointer to it before calling this function. 
		 */
		public static Flashcard removeFirst(Flashcard head) {
			if ( head == null)
				return null;
			Flashcard origHead = head;
			head = origHead._next;
			origHead._next = null;
			return head;
		}
	}
}
