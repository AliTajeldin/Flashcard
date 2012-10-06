package com.tajeldin.flashcard;

/**
 * An instance of PrefechQueue exists for each supported level. This queue will
 * store prefetched flashcards from the database in an internal queue. The
 * decision to replenish the internal queue must be done by the controller and
 * not the queue itself.
 * 
 * This class is not MT-safe, so it should only be used by a single thread.
 */
public class PrefetchQueue {

	private final int _level;
	private Flashcard _queue;
	private int _queueSize;
	private int _maxId;

	public PrefetchQueue(int level) {
		_level = level;
		_queue = null;
		_queueSize = 0;
		_maxId = 0;
	}

	/**
	 * returns the element at the head of the queue. If the queue is empty, a
	 * null is returned.
	 */
	public Flashcard dequeue() {
		Flashcard fc = _queue;
		_queue = Flashcard.Chain.removeFirst(_queue);
		return fc;
	}

	/**
	 * enqueue the flashcard set from a database query into this prefetch queue.
	 */
	public void enqueueSet(QueryResult qr) {
		_queue = Flashcard.Chain.append(_queue, qr.head);
		_queueSize += qr.count;
		_maxId = qr.maxId;
		// TODO: mark this queue as "partial" if qr.count < desired size.
	}

	/**
	 * Replenish the data in the queue by querying the database for more
	 * flashcards.
	 */
	public void replenishQueue() {
		MsgDispatcher.sendMessageToDB(MsgType.MSG_QUERY_FC_SET, _level,
				_maxId + 1, null);
	}

}
