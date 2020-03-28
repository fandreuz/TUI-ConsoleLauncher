package ohi.andre.consolelauncher.managers.xml.classes;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

/*
contains the SettingsEntry instances for a given file
 */
public class SettingsEntriesContainer {

    // enum has its own hashCode, which is used in this map
    // each enum is unique => good hash code
    private final HashMap<SettingsOption, SettingsEntry> entries = new HashMap<>();

    public int size() {
        return entries.size();
    }

    public String values() {
        StringBuilder builder = new StringBuilder();

        for(Map.Entry<SettingsOption, SettingsEntry> entry : entries.entrySet()) {
            builder.append(entry.getKey().label()).append(" = ").append(entry.getValue().value).append("\n");
        }

        return builder.toString().trim();
    }
}