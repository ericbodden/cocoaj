/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008-2010 Danilo Ansaloni, Walter Binder, Eric Bodden
 */

package org.cocoaj.racer;

/**
 * Custom class similar to a standard List, explicitly tuned for Racer.
 * 
 * @author Danilo Ansaloni
 */
public final class MyList<E> {
    static class Entry<E> {
        final E e;
        Entry<E> next;

        Entry(E e) {
           this.e = e; 
        }
    }

    private Entry<E> firstEntry;

    /**
     * Allocates an instance of MyList not containing any element
     */
    public MyList() { }

    /**
     * Allocates an instance of MyList containing only the specified element
     * 
     * @param element the element to be inserted
     */
    public MyList(E element) {
        append(element);
    }

    /**
     * Allocates an instance of MyList containing the specified elements.
     * Only the first "index" elements will be inserted.
     * 
     * @param elements array of elements to be inserted
     * @param index the number of elements that will be inserted
     */
    public MyList(E[] elements, int index) {
        if((elements != null) && (index >= 0)) {
            for(int i = 0; i < index; i++) {
                append(elements[i]);
            }
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * If the specified element is not already part of the list, it will
     * be appended.
     * 
     * @param element the element to be appended
     */
    public void append(E element) {
        if(element != null) {
            Entry<E> currentEntry = firstEntry;
            Entry<E> prevEntry = null;
            for(;;) {
                if(currentEntry == null) {
                    if(prevEntry == null) {
                        firstEntry = new Entry<E>(element);
                    }
                    else {
                        prevEntry.next = new Entry<E>(element);
                    }
                    return;
                }
                if(currentEntry.e == element) {
                    return;
                }
                prevEntry = currentEntry;
                currentEntry = currentEntry.next;
            }
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Performs the intersection between the elements that are part of the list
     * and the first "index" elements that are part of the "elements" array.
     * 
     * @param elements array of elements to be intersected
     * @param index the number of elements that will be intersected
     */
    public void retainAll(E[] elements, int index) {
        Entry<E> currentEntry = firstEntry;
        Entry<E> prevEntry = null;
        while(currentEntry != null) {
            boolean found = false;
            E element = currentEntry.e;
            for(int i = 0; i < index; i++) {
                if(element == elements[i]) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                if(prevEntry != null) {
                    prevEntry.next = (currentEntry = currentEntry.next);
                }
                else {
                    firstEntry = (currentEntry = currentEntry.next);
                }
            }
            else {
                prevEntry = currentEntry;
                currentEntry = currentEntry.next;
            }
        }
    }

    /**
     * @return true if the list is empty, false otherwise
     */
    public boolean isEmpty() {
        return firstEntry == null ? true : false;
    }
}

