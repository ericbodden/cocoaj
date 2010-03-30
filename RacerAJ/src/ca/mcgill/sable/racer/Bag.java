/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

import java.util.Collection;
import java.util.Iterator;

/**
 * A bag is a collection that is similar to a set but can hold the same object multiple times.
 * 
 * @author Eric Bodden
 */
public interface Bag extends Collection {

	/** 
	 * Adds <i>o</i> to the bag.
	 * @param o any object
	 * @return true 
	 */
	public boolean add(Object o);
	
	/** 
	 * Returns an iterator for this bag.
	 * The iterator returns an object o n times if o is contained n times in the bag.
	 */
	public Iterator iterator();
	
	/** 
	 * Returns an iterator for this bag.
	 * The iterator returns each object only 1 time.
	 */
	public Iterator kindIterator();
	
	/**
	 * Returns how often o is contained in this bag.
	 */
	public int countOf(Object o);
	
}
