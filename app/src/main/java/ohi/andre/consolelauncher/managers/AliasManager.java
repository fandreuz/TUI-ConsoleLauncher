package ohi.andre.consolelauncher.managers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

public class AliasManager implements Reloadable {

    private Map<String, String> alias;
    private PreferencesManager preferences;

    public AliasManager(PreferencesManager prefs) {
        this.preferences = prefs;
        reload();
    }

    public String printAliases() {
        Iterator<Entry<String, String>> iterator = alias.entrySet().iterator();

        String output = Tuils.EMPTYSTRING;
        Entry<String, String> entry;
        while (iterator.hasNext()) {
            entry = iterator.next();
            output = output.concat(entry.getKey() + " = " + entry.getValue() + Tuils.NEWLINE);
        }

        return output;
    }

    public int getNum() {
        return alias.size();
    }

    public String getAlias(String s) {
        return alias.get(s);
    }

    @Override
    public void reload() {
        alias = new HashMap<>();
        preferences.refresh(PreferencesManager.ALIAS);
        for (int count = 0; count < preferences.getLength(PreferencesManager.ALIAS); count++) {
            String line = preferences.getLine(PreferencesManager.ALIAS, count);
            String name = preferences.obtainKey(line);
            String value = preferences.obtainValue(line);

            alias.put(name, value);
        }
    }
}
