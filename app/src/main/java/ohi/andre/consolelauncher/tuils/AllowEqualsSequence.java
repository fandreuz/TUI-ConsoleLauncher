package ohi.andre.consolelauncher.tuils;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by francescoandreuzzi on 28/08/2017.
 */

public class AllowEqualsSequence {

    List<Entry> sequence;

    public AllowEqualsSequence(int[] keys, Object[] values) {
        this.sequence = new ArrayList<>();

        for (int count = 0; count < keys.length; count++) {
            if(keys[count] == Integer.MAX_VALUE) continue;
            sequence.add(new Entry(keys[count], values[count]));
        }
        Collections.sort(this.sequence);

        int counter = -1, last = Integer.MAX_VALUE;
        for(int count = 0; count < sequence.size(); count++) {
            Entry entry = sequence.get(count);

            int key = entry.key;
            if(key != last) {
                counter++;
            }
            entry.key = counter;
            last = key;
        }
    }

    public Object[] get(int key) {
        List<Object> o = new ArrayList<>();
        for(Entry entry : sequence) {
            if(entry.key == key) o.add(entry.value);
            else if(o.size() > 0) break;
        }

        return o.toArray(new Object[o.size()]);
    }

    public int getMaxKey() {
        if(sequence.size() == 0) return -1;
        return sequence.get(sequence.size() - 1).key;
    }

    public int getMinKey() {
        if(sequence.size() == 0) return -1;
        return sequence.get(0).key;
    }

    public int size() {
        return sequence.size();
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
        public String toString() {
            return key + ": " + value.toString();
        }
    }
}
