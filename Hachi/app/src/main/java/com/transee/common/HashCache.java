package com.transee.common;

import java.util.HashMap;

// map K to Item<K, V>
// all items are also in a linked-list for traversal
abstract public class HashCache<K, V, O> {

	abstract public int getStartIndex();

	abstract public int getEndIndex(); // inclusive

	abstract public K getItemKey(int index);

	abstract public O requestValue(int index, K key); // returns item owner

	abstract public void itemReleased(Item<K, V, O> item);

	public static class Item<K, V, O> {
		protected Item<K, V, O> prev;
		protected Item<K, V, O> next;
		protected boolean using;
		protected K key;
		protected O owner;

		public V value;
		public Object tag; // save user data

		private Item(K key) {
			this.key = key;
		}

		private final void reset() {
			prev = this;
			next = this;
		}

		// insert 'next' as my next
		private final void append(Item<K, V, O> next) {
			next.prev = this;
			next.next = this.next;
			this.next.prev = next;
			this.next = next;
		}

		// remove me from the list
		private final void removeSelf() {
			prev.next = next;
			next.prev = prev;
			prev = null;
			next = null;
		}
	}

	private final Item<K, V, O> mHead;
	private final HashMap<K, Item<K, V, O>> mCache;

	public HashCache() {
		mHead = new Item<K, V, O>(null);
		mHead.reset();
		mCache = new HashMap<K, Item<K, V, O>>();
	}

	// API
	public void clear() {
		releaseAll(true);
		mHead.reset();
		mCache.clear();
	}

	// API
	public void update() {

		// mark all as not using
		Item<K, V, O> item;
		for (item = mHead.next; item != mHead; item = item.next) {
			item.using = false;
		}

		// update range: start-1 .. end+1
		int indexStart = getStartIndex();
		if (indexStart > 0) {
			indexStart--;
		}
		int indexEnd = getEndIndex() + 1;

		// traverse the list
		for (int i = indexStart; i <= indexEnd; i++) {
			K key = getItemKey(i);
			if (key != null) {
				item = mCache.get(key);
				if (item == null) {
					O owner = requestValue(i, key);
					item = new Item<K, V, O>(key);
					item.owner = owner;
					mCache.put(key, item);
					mHead.append(item);
				}
				item.using = true;
			}
		}

		// remove those not using
		releaseAll(false);
	}

	// API
	public boolean changeKey(K oldKey, K newKey) {
		Item<K, V, O> item = mCache.get(oldKey);
		if (item != null) {
			mCache.remove(item.key);
			item.key = newKey;
			mCache.put(newKey, item);
			return true;
		}
		return false;
	}

	private final void releaseAll(boolean bForce) {
		Item<K, V, O> item = mHead.next;
		while (item != mHead) {
			Item<K, V, O> next = item.next;
			if (bForce || !item.using) {
				mCache.remove(item.key);
				item.removeSelf();
				itemReleased(item);
			}
			item = next;
		}
	}

	// API
	public final Item<K, V, O> getItem(K key) {
		return mCache.get(key);
	}

	// API
	public final V getValue(K key) {
		Item<K, V, O> item = mCache.get(key);
		return item == null ? null : item.value;
	}

	// API
	public final boolean setValue(K key, V value) {
		Item<K, V, O> item = mCache.get(key);
		if (item == null) {
			return false;
		} else {
			item.value = value;
			return true;
		}
	}
}
