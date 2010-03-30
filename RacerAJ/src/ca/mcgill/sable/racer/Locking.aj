/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

/**
 * Locking aspect, keeping track of thread-local lock sets.
 * 
 * @author Eric Bodden
 */
public aspect Locking {
	
	ThreadLocal locksHeld = new ThreadLocal() {
		 protected synchronized Object initialValue() {
			 return new HashBag();
		 }
	};
	
	before(Object l): lock() && args(l) && Racer.scope() {
		Bag locks = (Bag)locksHeld.get();
		locks.add(l);
		if(Racer.LOGGING) {
			System.err.println("LOCK:   Thread "+
					Thread.currentThread().getName()+" holds locks: "+locks);
		}
	}

	after(Object l): unlock() && args(l) && Racer.scope() {
		Bag locks = (Bag)locksHeld.get();
		assert locks.contains(l);
		locks.remove(l);
		if(Racer.LOGGING) {
			System.err.println("UNLOCK: Thread "+
					Thread.currentThread().getName()+" holds locks: "+locks);
		}
	}
}
