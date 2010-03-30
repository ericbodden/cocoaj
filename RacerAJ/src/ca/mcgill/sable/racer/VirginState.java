/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;


/**
 * VirginState
 *
 * @author Eric Bodden
 */
public class VirginState extends State {

	public VirginState(Object fieldOwner, String fieldSignature) {
		super(fieldOwner, fieldSignature, new LinkedHashSet(), new HashSet(Collections.singleton(State.FULLSET_MARKER)));
	}

	protected State newStateOnRead(Thread t,int accessRegion) {
		return new ExclusiveState(t,accessRegion,this,false);
	}
	
	protected State newStateOnWrite(Thread t,int accessRegion) {
		return new ModifiedState(t,accessRegion,this,false);
	}
	
	public String toString() {
		return "virgin";
	}

}
