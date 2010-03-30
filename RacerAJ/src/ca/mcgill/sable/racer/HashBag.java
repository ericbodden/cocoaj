/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A {@link Bag} that uses a {@link HashMap} as backing map.
 * @author Eric Bodden
 */
public class HashBag extends AbstractCollection implements Bag, Cloneable {

	/**
	 * An iterator for a {@link HashBag}.
	 * The iterator returns an object o n times if o is contained n times in the bag.
	 * <b>This iterator does not implement fast-fail semantics!</b>
	 * @author Eric Bodden
	 */
	protected class HashBagIterator implements Iterator {

		protected Iterator backingMapIterator;
		protected Map.Entry currentEntry;		
		protected int currentCount;
		protected Object lastObject;
		
		protected HashBagIterator() {
		    Set entrySet = backingMap.entrySet();
			backingMapIterator = entrySet.iterator();
		}
		
		public boolean hasNext() {
			if(currentEntry==null) {
				return init();
			} else {
				return currentCount <= ((Integer)currentEntry.getValue()).intValue() ||
				       backingMapIterator.hasNext();
			}
		}

		public Object next() {
			if(!hasNext()) throw new NoSuchElementException();

			lastObject = currentEntry.getKey();
			
			if(currentCount < ((Integer)currentEntry.getValue()).intValue()) {
				currentCount++;
				return currentEntry.getKey();
			} else {
				currentCount++;
				Object currentObject = currentEntry.getKey();
				if(backingMapIterator.hasNext()) {
					currentEntry = (Entry) backingMapIterator.next();
					currentCount = 1;
				}
				return currentObject;
			}
			
		}
		
		protected boolean init() {
			if(currentEntry==null) {
				if(!backingMapIterator.hasNext()) return false;
				currentEntry = (Entry) backingMapIterator.next();
				currentCount = 1;
			}
			return true;
		}

		public void remove() {
			if(HashBag.this.remove(lastObject)) {
				currentCount--;
			}
		}

	}

	protected Map backingMap;
	
	protected int size;
	
	/**
	 * Constructs a new, empty hash bag.
	 */
	public HashBag() {
		backingMap = new HashMap() {

			private static final long serialVersionUID = 1L;

			public Object get(Object key) {
				//return 0 as default
				Integer integer = (Integer) super.get(key);
				if(integer==null) integer = new Integer(0);
				return integer;
			}
		};
		size = 0;
	}
	
    /**
     * Constructs a new hash bag where each element in c is contained once.
     */
	public HashBag(Collection c) {
		this();
		addAll(c);
	}
	
	/** 
	 * Adds <i>o</i> to the bag.
	 * @param o any object
	 * @return true 
	 */
	public boolean add(Object o) {
		int count = ((Integer)backingMap.get(o)).intValue();
		int newCount = count+1;
		backingMap.put(o, new Integer(newCount));
		size++;
		return true;
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public boolean remove(Object o) {
		int count = ((Integer)backingMap.get(o)).intValue();
		if(count==0) {
			return false;
		} else {
			//..anyway, if we get here we know that o must be of type E because it's contained in the map 
			int newCount = count-1;
			if(newCount==0) {
		        backingMap.remove(o);
			} else {
				backingMap.put(o, new Integer(newCount));
			}
			size--;
			return true;
		}		
	}
	
	
	/** 
	 * Returns an iterator for this bag.
	 * The iterator returns an object o n times if o is contained n times in the bag.
	 */
	public Iterator iterator() {
		return new HashBagIterator();
	}

	/** 
	 * Returns the size, i.e. the number of all objects help in this bag
	 * where multiple instances of the same object are counted multiple times.
	 */
	public int size() {
		return size;
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public boolean isEmpty() {
		return backingMap.isEmpty();
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public void clear() {
		backingMap.clear();
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return backingMap.hashCode();
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj) {
		return backingMap.equals(obj);
	}
	
	/** 
	 * {@inheritDoc}
	 */
	public String toString() {
		return backingMap.toString();
	}
	
	/** 
	 * {@inheritDoc}
	 */
	protected Object clone() {
		try {
			HashBag clone = (HashBag) super.clone();
            clone.backingMap = (HashMap) ((HashMap)backingMap).clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			//cannot occur
			throw new RuntimeException();
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	public int countOf(Object o) {
		Integer val = (Integer)backingMap.get(o);
		if(val==null) {
			return 0;
		} else {
			return val.intValue();
		}
	}

	public Iterator kindIterator() {
		return backingMap.keySet().iterator();
	}	
	
}
