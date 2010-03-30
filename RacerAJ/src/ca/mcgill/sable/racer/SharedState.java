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
 * A field is in this state when it has been read by multiple threads.
 *
 * @author Eric Bodden
 */
public class SharedState extends State {

	/**
	 * @param predecessorState the predecessor state
	 */
	public SharedState(State predecessorState) {
		super(predecessorState);
	}
	
	protected State newStateOnWrite(Thread t,int accessRegion) {
		return new SharedModifiedState(this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return "shared";
	}
	
}
