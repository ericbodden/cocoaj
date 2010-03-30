/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

import org.aspectj.lang.reflect.SourceLocation;

/**
 * A record of a field access.
 * 
 * @author Eric Bodden
 */
public abstract class AbstractAccessRecord {
		
	/**
	 * The source location at which the access took place.
	 */
	protected SourceLocation accessLocation;

	public AbstractAccessRecord(SourceLocation accessLocation) {
		this.accessLocation = accessLocation;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessLocation == null) ? 0 : accessLocation.toString().hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AbstractAccessRecord other = (AbstractAccessRecord) obj;
		if (accessLocation == null) {
			if (other.accessLocation != null)
				return false;
		} else if (!accessLocation.toString().equals(other.accessLocation.toString()))
			return false;
		return true;
	}
	
	public String toString() {
		return getClass().getName() + " at "+accessLocation;
	}
	
}
