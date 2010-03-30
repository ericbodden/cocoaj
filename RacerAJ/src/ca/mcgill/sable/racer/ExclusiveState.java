/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

/**
 * The 'exclusive' state in the racer algorithm. It is parameterized with
 * the thread t to which it is exclusive.
 *
 * @author Eric Bodden
 */
public class ExclusiveState extends State {

	/**
	 * The parameter thread t. 
	 */
	protected final Thread t;
	protected final int accessRegion;

	/**
	 * @param t the parameter thread
	 * @param accessRegion 
	 * @param predecessorState the predecessor state
	 */
	public ExclusiveState(Thread t, int accessRegion, State predecessorState, boolean flushHistory) {
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
			return new SharedState(this);
		}		
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected State newStateOnWrite(Thread t,int accessRegion) {
		if(t==this.t) {
			return new ModifiedState(t,accessRegion,this,false);
		} else if (Racer.aspectOf().canSee(t,accessRegion,this.t,this.accessRegion)) {
			return new ModifiedState(t,accessRegion,this,true);
		} else {
			return new SharedModifiedState(this);
		}		
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return "exclusive "+t.getName();
	}
	

}
