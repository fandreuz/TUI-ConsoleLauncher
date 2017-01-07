package ohi.andre.consolelauncher.commands;

import java.util.HashMap;

import ohi.andre.consolelauncher.managers.PreferencesManager;

/**
 * Created by francescoandreuzzi on 06/01/2017.
 */

public class CommandsPreferences {

    public static final int DEFAULT_PARAM = 10;

    private HashMap<String, Preference> preferenceHashMap;

    public CommandsPreferences(PreferencesManager preferencesManager) {
        preferenceHashMap = new HashMap<>();

//        search
        Preference searchP = new Preference();
        searchP.add(preferencesManager.getValue(PreferencesManager.DEFAULT_SEARCH), DEFAULT_PARAM);
        preferenceHashMap.put("search", searchP);
    }

    public Preference forCommand(String cmd) {
        return preferenceHashMap.get(cmd);
    }

    public class Preference {
        HashMap<Integer, String> prefs = new HashMap<>();

        public void add(String pref, int id) {
            prefs.put(id, pref);
        }

        public String get(int id) {
            return prefs.get(id);
        }
    }
}
