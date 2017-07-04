package ohi.andre.consolelauncher.tuils;

/* Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

import java.io.Serializable;
import java.util.Map;

public class SimpleMutableEntry<K, V> implements Map.Entry<K, V>, Serializable {
    private static final long serialVersionUID = -8499721149061103585L;

    private final K key;
    private V value;

    public SimpleMutableEntry(K theKey, V theValue) {
        key = theKey;
        value = theValue;
    }

    /**
     * Constructs an instance with the key and value of {@code copyFrom}.
     */
    public SimpleMutableEntry(Map.Entry<? extends K, ? extends V> copyFrom) {
        key = copyFrom.getKey();
        value = copyFrom.getValue();
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V object) {
        V result = value;
        value = object;
        return result;
    }

    @Override public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Map.Entry) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
            return (key == null ? entry.getKey() == null : key.equals(entry
                    .getKey()))
                    && (value == null ? entry.getValue() == null : value
                    .equals(entry.getValue()));
        }
        return false;
    }

    @Override public int hashCode() {
        return (key == null ? 0 : key.hashCode())
                ^ (value == null ? 0 : value.hashCode());
    }

    @Override public String toString() {
        return key + "=" + value;
    }
}