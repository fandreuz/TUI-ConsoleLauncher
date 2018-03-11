package ohi.andre.consolelauncher.managers.xml.classes;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

public class XMLPrefsEntry {

    public String key, value;

    public XMLPrefsEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof XMLPrefsEntry) return this == obj;
        else if(obj instanceof XMLPrefsSave) return this.key.equals(((XMLPrefsSave) obj).label());
        return obj.equals(key);
    }

    @Override
    public String toString() {
        return key + " --> " + value;
    }
}