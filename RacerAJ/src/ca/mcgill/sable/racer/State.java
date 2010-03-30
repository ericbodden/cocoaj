/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.aspectj.lang.reflect.SourceLocation;

/**
 * An abstract automaton state.
 *
 * @author Eric Bodden
 */
public abstract class State implements Cloneable {
	
	
	/**
	 * Marker for the full set of locks.
	 */
	public final static Object FULLSET_MARKER = new Object() {
		public String toString() { return "all locks"; }
	};
	
	/**
	 * Weak reference to the owner of the field that owns this state.
	 */
	protected final WeakReference fieldOwnerRef;
	
	/**
	 * Signature of the field that owns this state. 
	 */
	protected final String fieldSignature;
	
	/**
	 * The access history for this field. Shared and protected by itself.
	 */
	protected final LinkedHashSet accessHistory;
	
	/**
	 * The lockset for this field. Shared and protected by itself.
	 */
	protected final HashSet locks;
	
	public State(State predecessorState) {
		this(
			predecessorState.fieldOwnerRef.get(),
			predecessorState.fieldSignature,
			new LinkedHashSet(predecessorState.accessHistory),
			new HashSet(predecessorState.locks)
		);
	}
	
	public State(Object fieldOwner, String fieldSignature,LinkedHashSet history, HashSet locks) {
		this.fieldOwnerRef = new WeakReference(fieldOwner);
		this.fieldSignature = fieldSignature;
		this.accessHistory = history;
		this.locks = locks;
	}

	private void registerRead(SourceLocation loc) {
		synchronized (accessHistory) {
			accessHistory.add(new Read(loc));
		}
	}

	private void registerWrite(SourceLocation loc) {
		synchronized (accessHistory) {
			accessHistory.add(new Write(loc));
		}
	}

	
	public final State onRead(Thread t, Object owner, String id, SourceLocation loc, int accessRegion) {
		State newStateOnRead = newStateOnRead(t,accessRegion);
		if(newStateOnRead!=this) {
			newStateOnRead.registerRead(loc);
		}
		newStateOnRead.updateLocks();
		newStateOnRead.processRead(t,owner,id,loc);
		return newStateOnRead;
	}
	
	protected void processRead(Thread t, Object owner, String id, SourceLocation loc) {}

	protected State newStateOnRead(Thread t,int accessRegion) {
		return this;
	}

	public final State onWrite(Thread t, Object owner, String id, SourceLocation loc, int accessRegion) {
		State newStateOnWrite = newStateOnWrite(t,accessRegion);
		if(newStateOnWrite!=this) {
			newStateOnWrite.registerWrite(loc);
		}
		newStateOnWrite.updateLocks();
		newStateOnWrite.processWrite(t,owner,id,loc);
		return newStateOnWrite;
	}
	
	protected void processWrite(Thread t, Object owner, String id, SourceLocation loc) {}

	protected State newStateOnWrite(Thread t, int accessRegion) {
		return this;
	}	
	
	protected void updateLocks() {
		Bag locksHeld = (Bag) Locking.aspectOf().locksHeld.get();
		intersectWith(locksHeld);				
	}
	
	private void intersectWith(Bag bag) {
		synchronized (locks) {
			if(locks.contains(FULLSET_MARKER)) {
				locks.clear();
				locks.addAll(bag);
			} else {
				locks.retainAll(bag);
			}
		}
	}
	
	protected Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
