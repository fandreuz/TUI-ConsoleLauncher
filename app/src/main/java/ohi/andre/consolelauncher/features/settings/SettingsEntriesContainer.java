package ohi.andre.consolelauncher.features.settings;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

/*
contains the SettingsEntry instances for a given file
 */
public class SettingsEntriesContainer {

    private final HashMap<String, SettingsEntry> entries = new HashMap<>();

    public SettingsEntriesContainer(SettingsOption[] options) {
        for(SettingsOption option : options) {
            entries.put(option.label(), new SettingsEntry(option, null));
        }
    }

    // this is intended to be used only once per boot
    public HashMap<String, SettingsEntry> entryCopy() {
        return new HashMap<String, SettingsEntry>(entries);
    }

    public Set<String> options() {
        return entries.keySet();
    }

    public SettingsEntry entry(String option) {
        return entries.get(option);
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }
}