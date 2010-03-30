/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Record of a data race.
 *
 * @author Eric Bodden
 */
public class Race {
	
	protected LinkedHashSet accessHistory;
	
	protected String fieldSignature;

	public Race(LinkedHashSet accessHistory, String fieldSignature) {
		this.accessHistory = accessHistory;
		this.fieldSignature = fieldSignature;
	}
	
	public void report(Object fieldOwner) {
		System.err.println("==========================");
		System.err.println("Race condition found!");
		System.err.println("Field '"+fieldSignature+"' is accessed unprotected.");
		System.err.println("Owner object: "+System.identityHashCode(fieldOwner));
		System.err.println("==========================\n");
		for (Iterator iter = accessHistory.iterator(); iter.hasNext();) {
			AbstractAccessRecord record = (AbstractAccessRecord) iter.next();
			System.err.println(record);
		}
		System.err.println("\n--------------------------\n");
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessHistory == null) ? 0 : accessHistory.hashCode());
		result = prime * result
				+ ((fieldSignature == null) ? 0 : fieldSignature.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Race other = (Race) obj;
		if (accessHistory == null) {
			if (other.accessHistory != null)
				return false;
		} else if (!accessHistory.equals(other.accessHistory))
			return false;
		if (fieldSignature == null) {
			if (other.fieldSignature != null)
				return false;
		} else if (!fieldSignature.equals(other.fieldSignature))
			return false;
		return true;
	}

}
