/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008 Eric Bodden
 */

package ca.mcgill.sable.racer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aspectbench.tm.runtime.internal.WeakKeyIdentityHashMap;
import org.aspectj.lang.JoinPoint.StaticPart;
import org.aspectj.lang.reflect.SourceLocation;

/**
 * Main monitoring aspect. This aspect monitors field accesses and records lock sets per field.
 *
 * @author Eric Bodden
 */
public aspect Racer {
	
	protected static class ThreadAndPeriod {
		final Thread t;
		final int period;
		public ThreadAndPeriod(Thread t, int period) {
			this.period = period;
			this.t = t;
		}		
	}
	
	protected ThreadLocal currentRegion = new ThreadLocal() {
		protected Object initialValue() {
			return new Integer(0);
		};
	};
		
	public final static boolean LOGGING = !System.getProperty("RACER_LOGGING","false").equals("false");
	
	/**
	 * A mapping from a field owner to a field to a state.
	 * This field is accessed by multiple threads but they all lock on
	 * this field.
	 */
	private Map ownerToFieldToState = new WeakKeyIdentityHashMap();		

	private HashMap threadToStarterThreadToStarterRegion = new HashMap();
	
	/**	restricts the scope of that aspect to avoid infinite recursion */
	pointcut scope(): !within(ca.mcgill.sable..*) &&
	  !cflow(within(ca.mcgill.sable..*));

	/** matches set-joinpoints of static fields */
    pointcut staticFieldSet(): set(static * *) /*&& maybeShared()*/;

	/** matches set-joinpoints of non-static fields exposing owner object of the field */
    pointcut fieldSet(Object owner): set(!static * *) && target(owner)  /*&& maybeShared() */;
		
	/** matches get-joinpoints of static fields */
    pointcut staticFieldGet(): get(static * *)  /*&& maybeShared()*/;

	/** matches get-joinpoints of non-static fields exposing owner object of the field */
    pointcut fieldGet(Object owner): get(!static * *) && target(owner) /* && maybeShared()*/;
    
    pointcut threadStart(Thread t): call(* Thread.start()) && target(t);

	// ========= Advice to capture field accesses ==========

	/**
	 * On every non-static field set...
	 * @param owner the owner object of the field
	 */
	before(Object owner): fieldSet(owner) && scope() {
		String id = getId(thisJoinPointStaticPart);
		SourceLocation loc = location(thisJoinPointStaticPart);
		fieldSet(owner,id,loc);
	}

	/**
	 * On every static field set...
	 */
	before(): staticFieldSet() && scope() {
		String id = getId(thisJoinPointStaticPart);
		Class owner = thisJoinPointStaticPart.getSignature().getDeclaringType();		
		SourceLocation loc = location(thisJoinPointStaticPart);
		fieldSet(owner,id,loc);
	}

	/**
	 * On every non-static field get...
	 * @param owner the owner object of the field
	 */
	before(Object owner): fieldGet(owner) && scope() {
		String id = getId(thisJoinPointStaticPart);
		SourceLocation loc = location(thisJoinPointStaticPart);
		fieldGet(owner,id,loc);		
	}

	/**
	 * On every static field get...
	 */
	before(): staticFieldGet() && scope() {
		String id = getId(thisJoinPointStaticPart);
		Class owner = thisJoinPointStaticPart.getSignature().getDeclaringType();		
		SourceLocation loc = location(thisJoinPointStaticPart);
		fieldGet(owner,id,loc);
	}

	before(Thread t): threadStart(t) && scope() {
		Object currRegion = currentRegion.get();
		Thread currThread = Thread.currentThread();

		Map currThreadStarterThreadsToStarterRegion = new HashMap();
		//started thread can "see" current region of starter thread
		currThreadStarterThreadsToStarterRegion.put(currThread, currRegion);
		synchronized (threadToStarterThreadToStarterRegion) {
			Map transitiveStarterThreadToStarterRegion = (Map)threadToStarterThreadToStarterRegion.get(currThread);

			if(transitiveStarterThreadToStarterRegion!=null) {
				currThreadStarterThreadsToStarterRegion.putAll(transitiveStarterThreadToStarterRegion);
			}		
			threadToStarterThreadToStarterRegion.put(t, currThreadStarterThreadsToStarterRegion);			
		}
		
		currentRegion.set(new Integer(((Integer)currRegion).intValue()+1));
	}
	
	private String getId(StaticPart sp) {
		return sp.getSignature().toLongString().intern();
	}

    private SourceLocation location(StaticPart sp){
	    return sp.getSourceLocation();
	}

	private void fieldSet(Object owner, String id, SourceLocation loc) {
		int currRegion = ((Integer)currentRegion.get()).intValue();
		synchronized (owner) {
			State currentState = getState(Thread.currentThread(),owner, id);
			State newState = currentState.onWrite(Thread.currentThread(),owner,id,loc,currRegion);
			putState(owner, id, newState);
			if(LOGGING) {
				System.err.println("WRITE: Moved state for field '"+id+
						"' of object '"+owner+"' to from '"+currentState+
						"' to '"+newState+"' ("+Thread.currentThread().getName()+")");
			}
		}
	}

	private void fieldGet(Object owner, String id, SourceLocation loc) {
		int currRegion = ((Integer)currentRegion.get()).intValue();
		synchronized (owner) {
			State currentState = getState(Thread.currentThread(),owner, id);
			State newState = currentState.onRead(Thread.currentThread(),owner,id,loc,currRegion);
			putState(owner, id, newState);
			if(LOGGING) {
				System.err.println("READ:  Moved state for field '"+id+
						"' of object '"+owner+"' to from '"+currentState+
						"' to '"+newState+"' ("+Thread.currentThread().getName()+")");
			}
		}
	}
	

	private State getState(Thread t, Object owner, String id) {
		synchronized (ownerToFieldToState) {
			Map fieldToState = (Map)ownerToFieldToState.get(owner);		
			if(fieldToState==null) {
				fieldToState = new HashMap();
				ownerToFieldToState.put(owner,fieldToState);
			}
			
			State state = (State)fieldToState.get(id);
			if(state==null) {
				state = new VirginState(owner, id);
				fieldToState.put(id,state);
			}
			return state;
		}
	}

	private void putState(Object owner, String id, State newState) {
		synchronized (ownerToFieldToState) {
			Map fieldToState = (Map)ownerToFieldToState.get(owner);		
			if(fieldToState==null) {
				fieldToState = new HashMap();
				ownerToFieldToState.put(owner,fieldToState);
			}
			
			fieldToState.put(id,newState);
		}
	}
	
	public boolean canSee(Thread t, int tRegion, Thread canSee, int canSeeRegion) {
		if(t==canSee) return true;
		
		synchronized(threadToStarterThreadToStarterRegion) {
			Map s = (Map)threadToStarterThreadToStarterRegion.get(t);
			if(s==null) return false;
			Integer i = (Integer)s.get(canSee);
			if(i==null) return false;
			
			boolean ret = canSeeRegion<=i.intValue();
			return ret;
		}
	}

}
