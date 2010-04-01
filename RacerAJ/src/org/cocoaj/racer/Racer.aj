/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008-2010 Danilo Ansaloni, Walter Binder, Eric Bodden
 */

package org.cocoaj.racer;

import java.util.Stack;
import org.aspectj.lang.JoinPoint.StaticPart;

/**
 * This aspect monitors field accesses, keeps track of thread-local lock collections,
 * and records lock sets per field.
 *
 * @author Eric Bodden
 */
public aspect Racer {
//    protected static class ThreadAndPeriod {
//        final Thread t;
//        final int period;
//        public ThreadAndPeriod(Thread t, int period) {
//            this.period = period;
//            this.t = t;
//        }       
//    }

//    protected ThreadLocal currentRegion = new ThreadLocal() {
//        protected Object initialValue() {
//            return new Integer(0);
//        };
//    };

    ThreadLocal<Stack<Object>> locksHeld = new ThreadLocal<Stack<Object>>() {
        protected Stack<Object> initialValue() {
            return new Stack<Object>();
        }
    };

    public final static boolean LOGGING = !System.getProperty("RACER_LOGGING","false").equals("false");

    private AdviceExecutor adviceExecutor = new AdviceExecutor();
//    private HashMap threadToStarterThreadToStarterRegion = new HashMap();

    /** restricts the scope of that aspect to avoid infinite recursion */
    pointcut scope(): !within(org.cocoaj.racer..*);

    /** matches set-joinpoints of static fields */
    pointcut staticFieldSet(): set(static * *);

    /** matches set-joinpoints of non-static fields exposing owner object of the field */
    pointcut fieldSet(Object owner): set(!static * *) && target(owner);

    /** matches get-joinpoints of static fields */
    pointcut staticFieldGet(): get(static * *);

    /** matches get-joinpoints of non-static fields exposing owner object of the field */
    pointcut fieldGet(Object owner): get(!static * *) && target(owner);

    pointcut threadStart(Thread t): call(* Thread.start()) && target(t);

    // ========= Advice to capture lock/unlock ==========

    /**
     * On every lock acquisition...
     * NOTE: since implicit locks are acquired/released in a LIFO policy,
     * we can use a stack to store the collection of currently held locks
     * @param l the acquired locks
     */
    before(Object l) : lock() && args(l) && scope() {
        locksHeld.get().push(l);
    }

    /**
     * On every lock release...
     * NOTE: since implicit locks are acquired/released in a LIFO policy,
     * we can use a stack to store the collection of currently held locks
     */
    after() : unlock() && scope() {
        locksHeld.get().pop();
    }

    // ========= Advice to capture field accesses ==========

    /**
     * On every static field access...
     */
    before(): (staticFieldSet() || staticFieldGet()) && scope() {
        adviceExecutor.onStaticFieldAccess(thisJoinPointStaticPart, locksHeld.get(), Thread.currentThread());
    }

    /**
     * On every non-static field access...
     * @param owner the owner object of the field
     */
    before(Object owner): (fieldSet(owner) || fieldGet(owner)) && scope() {
        adviceExecutor.onFieldAccess(thisJoinPointStaticPart, locksHeld.get(), Thread.currentThread(), owner);
    }

//    // ========= Advice to capture thread start ==========
//
//    before(Thread t): threadStart(t) && scope() {
//        Object currRegion = currentRegion.get();
//        Thread currThread = Thread.currentThread();
//
//        Map currThreadStarterThreadsToStarterRegion = new HashMap();
//        //started thread can "see" current region of starter thread
//        currThreadStarterThreadsToStarterRegion.put(currThread, currRegion);
//        synchronized (threadToStarterThreadToStarterRegion) {
//            Map transitiveStarterThreadToStarterRegion = (Map)threadToStarterThreadToStarterRegion.get(currThread);
//
//            if(transitiveStarterThreadToStarterRegion!=null) {
//                currThreadStarterThreadsToStarterRegion.putAll(transitiveStarterThreadToStarterRegion);
//            }       
//            threadToStarterThreadToStarterRegion.put(t, currThreadStarterThreadsToStarterRegion);           
//        }
//        
//        currentRegion.set(new Integer(((Integer)currRegion).intValue()+1));
//    }
}
