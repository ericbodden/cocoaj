/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;


/**
 * The exclusive-modified state from the racer algorithm.
 * A field is in this state when it has been written to but only
 * been accessed by a single thread.
 *
 * @author Eric Bodden
 */
public class ModifiedState extends State {

	/**
	 * The thread that accessed the field to which this state belongs. 
	 */
	protected final Thread t;
	protected final int accessRegion;

	/**
	 * @param t the parameter thread
	 * @param accessRegion 
	 * @param predecessorState the predecessor state
	 */
	public ModifiedState(Thread t, int accessRegion, State predecessorState,boolean flushHistory) {
		super(predecessorState);
		if(flushHistory) accessHistory.clear();
		this.t = t;
		this.accessRegion = accessRegion;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected State newStateOnRead(Thread t,int accessRegion) {
		if(t==this.t) {
			return this;
		} else if (Racer.aspectOf().canSee(t,accessRegion,this.t,this.accessRegion)) {
			State s = new ExclusiveState(t,accessRegion,this,true);
			s.locks.clear();
			s.locks.add(FULLSET_MARKER);
			return s;
		} else {
			return new SharedModifiedState(this);
		}		
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected State newStateOnWrite(Thread t,int accessRegion) {
		if(t==this.t||Racer.aspectOf().canSee(t,accessRegion,this.t,this.accessRegion)) {
			return this;
		} else {
			return new SharedModifiedState(this);
		}		
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return "modified "+t.getName();
	}
	
}
