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

    private final K key;
    private V value;

    public SimpleMutableEntry(K theKey, V theValue) {
        key = theKey;
        value = theValue;
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

    @Override
    public String toString() {
        return key + "=" + value;
    }
}