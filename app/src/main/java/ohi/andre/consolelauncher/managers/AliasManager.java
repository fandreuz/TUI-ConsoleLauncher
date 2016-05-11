package ohi.andre.consolelauncher.managers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AliasManager {

    private Map<String, String> alias;

    public AliasManager(PreferencesManager prefs) {
        alias = new HashMap<>();
        for (int count = 0; count < prefs.getLength(PreferencesManager.ALIAS); count++) {
            String line = prefs.getLine(PreferencesManager.ALIAS, count);
            String name = prefs.obtainKey(line);
            String value = prefs.obtainValue(line);

            alias.put(name, value);
        }
    }

    public String printAliases() {
        Iterator<Entry<String, String>> iterator = alias.entrySet().iterator();

        String output = "";
        Entry<String, String> entry;
        while (iterator.hasNext()) {
            entry = iterator.next();
            output = output.concat(entry.getKey() + " = " + entry.getValue() + "\n");
        }

        return output;
    }

    public int getNum() {
        return alias.size();
    }

    public String getAlias(String s) {
        if (!alias.containsKey(s))
            return null;

        return alias.get(s);
    }

    public Set<String> getAliass() {
        return alias.keySet();
    }

}
