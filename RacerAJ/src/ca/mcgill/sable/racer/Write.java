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
 * A record of a writing field access.
 * 
 * @author Eric Bodden
 */
public class Write extends AbstractAccessRecord {

	public Write(SourceLocation accessLocation) {
		super(accessLocation);
	}

}
