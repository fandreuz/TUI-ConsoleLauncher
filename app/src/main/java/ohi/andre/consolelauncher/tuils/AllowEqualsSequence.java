package ohi.andre.consolelauncher.tuils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by francescoandreuzzi on 28/08/2017.
 */

public class AllowEqualsSequence {

    List<Entry> sequence;

    public AllowEqualsSequence(float[] values, Object[] objs) {
        this.sequence = new ArrayList<>();

        for (int count = 0; count < values.length; count++) {
            if(values[count] == Integer.MAX_VALUE) continue;
            sequence.add(new Entry(values[count], objs[count]));
        }
        Collections.sort(this.sequence);

        int counter = -1, last = Integer.MAX_VALUE;
        for(int count = 0; count < sequence.size(); count++) {
            Entry entry = sequence.get(count);

            int i = (int) entry.value;
            if(i != last) counter++;

            entry.key = counter;
            last = i;
        }
    }

    public Object[] get(int key) {
        List<Object> o = new ArrayList<>();
        for(Entry entry : sequence) {
            if(entry.key == key) o.add(entry.obj);
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

        float value;
        Object obj;

        int key;

        public Entry(float value, Object obj) {
            this.value = value;
            this.obj = obj;
        }

        @Override
        public int compareTo(@NonNull Entry o) {
            float result = value - o.value;

            if(result == 0) return 0;
            if(result < 0) return -1;
            return 1;
        }

        @Override
        public String toString() {
            return "key: " + key + ": " + obj.toString();
        }
    }
}
