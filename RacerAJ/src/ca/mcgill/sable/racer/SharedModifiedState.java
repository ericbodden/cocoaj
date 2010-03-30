/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

import java.util.HashSet;
import java.util.Set;

import org.aspectj.lang.reflect.SourceLocation;

/**
 * The exclusive-modified state from the racer algorithm.
 * A field is in this state when it has been written to by at least one thread
 * but generally accessed by multiple threads.
 *
 * @author Eric Bodden
 */
public class SharedModifiedState extends State {

	/**
	 * The thread that accessed the field to which this state belongs. 
	 * Shared and protected by itself.
	 */
	protected static Set reportedRaces = new HashSet();
	
	/**
	 * @param predecessorState the predecessor state
	 */
	public SharedModifiedState(State predecessorState) {
		super(predecessorState);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void processRead(Thread t, Object owner, String id, SourceLocation loc) {
		reportRace();
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void processWrite(Thread t, Object owner, String id, SourceLocation loc) {
		reportRace();
	}
	
	private void reportRace() {
		if(locks.isEmpty()) {
			Race race = new Race(accessHistory,fieldSignature);
			synchronized (reportedRaces) {
				if(!reportedRaces.contains(race)) {
					race.report(fieldOwnerRef.get());
					reportedRaces.add(race);
				}
			}
		} 
	}	
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return "sharedModified";
	}
	
}
