/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008-2010 Danilo Ansaloni, Walter Binder, Eric Bodden
 */

package org.cocoaj.racer;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.Signature;
import org.aspectj.lang.JoinPoint.StaticPart;

/**
 * The methods of this class are called by the Racer aspect upon each field access.
 * The finite-state machine corresponding to the accessed field is retrieved and updated
 * accordingly to the type of access (i.e.: read or write).
 *
 * @author Eric Bodden
 * @author Danilo Ansaloni
 *
 */
public class AdviceExecutor {
    public static final int NUMBER_OF_MAPS = 100000;

    /**
     * A mapping from a field owner to a field to a state.
     * Since this map will be accessed concurrently by multiple threads, we split
     * the map into many sub-maps to take advantage from lock-striping.
     * The map is of custom type MyWeakKeyIdentityHashMap and uses weak-references
     * to store the keys and identity instead of equality to compare the entries.
     */
    private MyWeakKeyIdentityHashMap<Object, ConcurrentHashMap<String, FieldState>>[] ownerToFieldToState;

    /**
     * Initialize all the sub-maps.
     * Since we observed that on standard applications almost all the maps are used,
     * we do not rely on lazy initialization (which could save some memory but would
     * require an additional check upon each field access to see if the corresponding
     * map has been initialized).
     */
    AdviceExecutor() {
        ownerToFieldToState = new MyWeakKeyIdentityHashMap[NUMBER_OF_MAPS];

        for(int i = 0; i < ownerToFieldToState.length; i++) {
            ownerToFieldToState[i] = new MyWeakKeyIdentityHashMap<Object, ConcurrentHashMap<String, FieldState>>(8);
        }
    }

    /**
     * This method retrieves and updates the finite-state machine corresponding to
     * the accessed static field.
     * @param jpsp the JoinPoint.StaticPart corresponding to the intercepted joinpoint
     * @param stack the collection of locks hold by the accessing thread
     * @param t the accessing thread 
     */
    void onStaticFieldAccess(StaticPart jpsp, Stack<Object> stack, Thread t) {
        //if the accessed field is static we use jpsp.getSignature().getDeclaringType()
        //as owner of the field 
        onFieldAccess(jpsp, stack, t, jpsp.getSignature().getDeclaringType());
    }

    /**
     * This method retrieves and updates the finite-state machine corresponding to
     * the accessed field.
     * @param jpsp the JoinPoint.StaticPart corresponding to the intercepted joinpoint
     * @param stack the collection of locks hold by the accessing thread
     * @param t the accessing thread
     * @param owner the instance of the class that owns the accessed field
     */
    void onFieldAccess(StaticPart jpsp, Stack<Object> stack, Thread t, Object owner) {
        ConcurrentHashMap<String, FieldState> fieldToState;
        MyWeakKeyIdentityHashMap<Object, ConcurrentHashMap<String, FieldState>> localOwnerToFieldToState
            = ownerToFieldToState[System.identityHashCode(owner) % NUMBER_OF_MAPS];
        //acquire the lock of the specific sub-map
        synchronized(localOwnerToFieldToState) {
            if((fieldToState = localOwnerToFieldToState.get(owner)) == null) {
                //if there is no map of fields associated to that specific class instance, create a new one
                localOwnerToFieldToState.put(owner, fieldToState = new ConcurrentHashMap<String, FieldState>());
            }
        }

        Signature signature;
        String signatureShortString;
        FieldState currentState;
        //this operation can be performed without holding any lock because we are using a ConcurrentHashMap
        if((currentState = fieldToState.get(signatureShortString = (signature = jpsp.getSignature()).toShortString())) == null) {
            //if no finite-state machine has been associated to the accessed field, acquire the lock of the map...
            synchronized (fieldToState) {
                //...and check again if the condition still holds (to avoid overriding an entry inserted by another thread)
                if((currentState = fieldToState.get(signatureShortString = (signature = jpsp.getSignature()).toShortString())) == null) {
                    //if no finite-state machine has been associated to the accessed field, create a new one
                    fieldToState.put(signatureShortString, currentState = new FieldState(signature));
                }
            }
        }

        //perform the proper operation depending on the type of access (i.e.: read or write)
        //TODO: avoid string comparison to perform this check
        if(jpsp.getKind().equals("field-get")) {
            currentState.onRead(t, jpsp, stack);
        }
        else {
            currentState.onWrite(t, jpsp, stack);
        }
    }
}
