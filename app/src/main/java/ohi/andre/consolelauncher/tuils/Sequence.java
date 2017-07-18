package ohi.andre.consolelauncher.tuils;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by francescoandreuzzi on 12/07/2017.
 */

public class Sequence {

    List<Entry> sequence;

    public Sequence(int[] keys, Object[] values) {
        this.sequence = new ArrayList<>();

        for(int count = 0; count < keys.length; count++) add(keys[count], values[count]);
        Collections.sort(this.sequence);
    }

    public void add(int key, Object v) {
        add(new Entry(key, v));
    }

    public void add(Entry e) {
        int i = indexOf(e);
        if(i == -1) this.sequence.add(e);
        else {
            e.key++;
            i = indexOf(e);
            if(i == -1) this.sequence.add(e);
            else {
                Entry x = sequence.remove(i);
                sequence.add(e);
                add(x);
            }
        }
    }

    public Object get(int index) {
        return sequence.get(index).value;
    }

    public int size() {
        return sequence.size();
    }

    private int indexOf(Entry e) {
        for(int count = 0; count < sequence.size(); count++) if(e.equals(sequence.get(count))) return count;
        return -1;
    }

    private class Entry implements Comparable<Entry> {
        int key;
        Object value;

        public Entry(int key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int compareTo(@NonNull Entry o) {
            return this.key - o.key;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Entry) return key == ((Entry) obj).key;
            return false;
        }

        @Override
        public String toString() {
            return key + ": " + value.toString();
        }
    }
}
