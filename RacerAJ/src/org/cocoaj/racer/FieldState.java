/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008-2010 Danilo Ansaloni, Walter Binder, Eric Bodden
 */

package org.cocoaj.racer;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import org.aspectj.lang.Signature;
import org.aspectj.lang.JoinPoint.StaticPart;

/**
 * This class implements a finite-state machine (FSM). Each accessed field
 * will be associated with a specific instance of this class.
 * When the state is SHARED_MODIFIED and the intersection of the collections
 * of locks hold by the threads accessing the corresponding field is
 * null, a potential data-race is reported.
 * 
 * @author Eric Bodden
 * @author Danilo Ansaloni
 * 
 */
public final class FieldState {
    /**
     * This type defines the set of states in which the FSM could be
     */
    private enum FState {
        VIRGIN,
        EXCLUSIVE,
        MODIFIED,
        SHARED,
        SHARED_MODIFIED,
        REPORTED_RACE
    }

    /**
     * Contains all the potential data-races that have already been reported 
     */
    private static Set<String> reportedRaces = new HashSet<String>();

    /**
     * The signature of the field associated to this FSM
     */
    private final Signature fieldSignature;

    /**
     * Data structures used to record an history of the accesses that could
     * represent a data-race
     */
    private LinkedList<StaticPart> readList = new LinkedList<StaticPart>();
    private LinkedList<StaticPart> writeList = new LinkedList<StaticPart>();

    /**
     * The collection of locks that the threads hold when accessing the associated
     * field
     */
    private MyList<Object> locksList;

    /**
     * The current state of the FSM
     */
    private FState currentState;

    /**
     * A reference to the first thread that accessed the associated field
     */
    private Thread t;

    /**
     * True if the field has never been accessed by any thread.
     */
    private boolean virgin;

    /**
     * Creates a new FSM in VIRGIN state
     * 
     * @param fieldSignature the signature of the corresponding field
     */
    public FieldState(Signature fieldSignature) {
        this.fieldSignature = fieldSignature;
        currentState = FState.VIRGIN;
        virgin = true;
    }

    /**
     * Synchronized method to perform the state transitions, update the history,
     * and intersect the set of locks in case of read access to the field.
     * 
     * @param t the accessing thread
     * @param jpsp the JoinPoint.Static part relative to the intercepted joinpoint
     * @param stack the collection of locks hold by the accessing thread
     */
    public synchronized void onRead(Thread t, StaticPart jpsp, Stack<Object> stack) {
        boolean reportRace = false;
        switch(currentState) {
            case VIRGIN:
                currentState = FState.EXCLUSIVE;
                this.t = t;
                readList.add(jpsp);
                updateLocks(stack);
                return;
            case EXCLUSIVE:
                if(this.t != t) {
                    currentState = FState.SHARED;
                    readList.add(jpsp);
                }
                updateLocks(stack);
                return;
            case MODIFIED:
                if(this.t != t) {
                    readList.add(jpsp);
                    updateLocks(stack);
                    if(reportRace = needToReportRace()) {
                        currentState = FState.REPORTED_RACE;
                        break;
                    }
                    else {
                        currentState = FState.SHARED_MODIFIED;
                        return;
                    }
                }
                else {
                    updateLocks(stack);
                }
                return;
            case SHARED:
                updateLocks(stack);
                return;
            case SHARED_MODIFIED:
                updateLocks(stack);
                if(reportRace = needToReportRace()) {
                    currentState = FState.REPORTED_RACE;
                    break;
                }
                return;
            case REPORTED_RACE:
                return;
        }
        if(reportRace) {
            reportRace();
        }
    }

    /**
     * Synchronized method to perform the state transitions, update the history,
     * and intersect the set of locks in case of write access to the field.
     * 
     * @param t the accessing thread
     * @param jpsp the JoinPoint.Static part relative to the intercepted joinpoint
     * @param stack the collection of locks hold by the accessing thread
     */
    public synchronized void onWrite(Thread t, StaticPart jpsp, Stack<Object> stack) {
        boolean reportRace = false;
        switch(currentState) {
            case VIRGIN:
                currentState = FState.MODIFIED;
                this.t = t;
                writeList.add(jpsp);
                updateLocks(stack);
                return;
            case EXCLUSIVE:
                if(this.t == t) {
                    currentState = FState.MODIFIED;
                    writeList.add(jpsp);
                    updateLocks(stack);
                    return;
                }
                else {
                    writeList.add(jpsp);
                    updateLocks(stack);
                    if(reportRace = needToReportRace()) {
                        currentState = FState.REPORTED_RACE;
                        break;
                    }
                    else {
                        currentState = FState.SHARED_MODIFIED;
                    }
                }
                return;
            case MODIFIED:
                if(this.t != t) {
                    writeList.add(jpsp);
                    updateLocks(stack);
                    if(reportRace = needToReportRace()) {
                        currentState = FState.REPORTED_RACE;
                        break;
                    }
                    else {
                        currentState = FState.SHARED_MODIFIED;
                        return;
                    }
                }
                else {
                    updateLocks(stack);
                }
                return;
            case SHARED:
                writeList.add(jpsp);
                updateLocks(stack);
                if(reportRace = needToReportRace()) {
                    currentState = FState.REPORTED_RACE;
                    break;
                }
                else {
                    currentState = FState.SHARED_MODIFIED;
                }
                return;
            case SHARED_MODIFIED:
                updateLocks(stack);
                if(reportRace = needToReportRace()) {
                    currentState = FState.REPORTED_RACE;
                    break;
                }
                return;
            case REPORTED_RACE:
                return;
        }
        if(reportRace) {
            reportRace();
        }
    }

    /**
     * Updates the collection of locks associated to the FSM by performing the
     * intersection with the associated locks and the collection of locks hold
     * by the last accessing thread.
     *  
     * @param stack the collection of locks hold by the accessing thread
     */
    private void updateLocks(Stack<Object> stack) {
        if(virgin) {
            virgin = false;
            int index;
            if((index = stack.size()) != 0) {
                locksList = new MyList<Object>(stack.toArray(), index);
            }
        }
        else {
            if(locksList != null) {
                locksList.retainAll(stack.toArray(), stack.size());
            }
        }
    }

    /**
     * @return true if no potential data-race has already been reported for the
     * associated field
     */
    private boolean needToReportRace() {
        if((locksList == null) || (locksList.isEmpty())) {
            synchronized(reportedRaces) {
                return reportedRaces.add(fieldSignature.toShortString());
            }
        }
        return false;
    }

    /**
     * Reports a potential data-race.
     */
    private void reportRace() {
        System.err.print("==========================\nRace condition found!\nUnprotected access to field: " + fieldSignature.toLongString());
        String accessHistory = new String();
        while(!readList.isEmpty()) {
            accessHistory += "\nREAD: " + readList.removeFirst().getSourceLocation().toString();
        }
        while(!writeList.isEmpty()) {
            accessHistory += "\nWRITE: " + writeList.removeFirst().getSourceLocation().toString();
        }
        System.err.println(accessHistory + "\n==========================\n");
    }
}
