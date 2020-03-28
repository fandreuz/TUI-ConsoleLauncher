package ohi.andre.consolelauncher.managers.xml.classes;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

/*

 */
public class SettingsEntry {

    // the option which observes this entry
    public final SettingsOption option;

    // the value assigned by the user
    public final String value;

    public SettingsEntry(SettingsOption option, String value) {
        this.option = option;
        this.value = value;
    }

    @Override
    public String toString() {
        return option.label() + " --> " + value;
    }
}