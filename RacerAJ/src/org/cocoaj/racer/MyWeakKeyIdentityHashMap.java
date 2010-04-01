/*
 * This code is made available under version 3 of the
 * GNU GENERAL PUBLIC LICENSE. See the file LICENSE in this
 * distribution for details.
 * 
 * Copyright 2008-2010 Danilo Ansaloni, Walter Binder, Eric Bodden
 */

package org.cocoaj.racer;

import java.lang.ref.WeakReference;

/**
 * Custom class similar to a Map, explicitly tuned for Racer.
 * This class uses WeakReferences to store the keys and identity instead
 * of equality to compare the entries.
 * Entries using as key objects that have been reclaimed by the GC will be
 * removed from the table before resizing (this avoids the necessity of an
 * external thread to remove these entries).
 * Since this class violates Map's general contract, we decided to avoid
 * implementing the Map interface.
 * 
 * @author Danilo Ansaloni
 *
 */
public final class MyWeakKeyIdentityHashMap<K, V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 4096;
    private static final int INCREMENT_FACTOR = 2;
    private transient Object[] table;
    private int size = 0;
    private transient int threshold;

    private volatile int currentSize;

    public MyWeakKeyIdentityHashMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public MyWeakKeyIdentityHashMap(int initCapacity) {
        currentSize = initCapacity;
        threshold = (currentSize * 2) / 3;
        table = new Object[currentSize * 2];
    }

    private int hash(Object obj, int size) {
        return (System.identityHashCode(obj) % size) * 2;
    }

    private static int nextKeyIndex(int prevIndex, int size) {
        return (prevIndex + 2 < (size * 2) ? prevIndex + 2 : 0);
    }

    public V get(K key) {
        Object[] tab = table;
        int index = hash(key, currentSize);
        while (true) {
            WeakReference<Object> item = (WeakReference<Object>)tab[index];
            if (item == null)
                return null;
            if (item.get() == key)
                return (V)tab[index + 1];
            index = nextKeyIndex(index, currentSize);
        }
    }

    public void put(K key, V value) {
        Object[] tab = table;
        int index = hash(key, currentSize);

        WeakReference<Object> item;
        while ((item = (WeakReference<Object>)tab[index]) != null) {
            if (item.get() == key) {
                tab[index + 1] = value;
                return;
            }
            index = nextKeyIndex(index, currentSize);
        }

        tab[index] = new WeakReference<Object>(key);
        tab[index + 1] = value;
        if (++size >= threshold)
            cleanAndResize();
    }

    private void cleanAndResize() {
        int newSize = currentSize * INCREMENT_FACTOR;

        Object[] oldTable = table;
        int oldLength = oldTable.length;

        Object[] newTable = new Object[newSize * 2];

        for (int j = 0; j < oldLength; j += 2) {
            WeakReference<Object> key = (WeakReference<Object>)oldTable[j];
            if (key != null) {
                Object obj;
                if((obj = key.get()) != null) {
                    Object value = oldTable[j+1];
                    int i = hash(obj, newSize);
                    while (newTable[i] != null)
                        i = nextKeyIndex(i, newSize);
                    newTable[i] = key;
                    newTable[i + 1] = value;
                }
                else {
                    size--;
                }
            }
        }
        table = newTable;
        currentSize = newSize;
        threshold = newSize * 2 / 3;
    }
}
