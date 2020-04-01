package ohi.andre.consolelauncher.managers.settings;

import it.andreuzzi.comparestring2.StringableObject;
import ohi.andre.consolelauncher.tuils.Function;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

/*
defines an option and info about its type, default value. the difference between this class
and SettingsEntry is: SettingsEntry represents the actual value decided by the user.
SettingsOption doesn't change, unless the developer touches the code.
IMPORTANT: a subclass of this interface should override hashCode()
 */
public interface SettingsOption extends StringableObject {
    String TYPE_APP = "app";
    String TYPE_INTEGER = "int";
    String TYPE_BOOLEAN = "boolean";
    String TYPE_TEXT = "text";
    String TYPE_COLOR = "color";

    // returns the default value of this option
    String defaultValue();

    // returns the type of this option (choose one of TYPE_*)
    String type();

    // returns an info string about this option
    String info();

    // returns the parent SettingsFile of this option
    SettingsFile parent();

    // returns the name of this option (as it appears in the file)
    String label();

    // convenient method to get the associated SettingsEntry
    default SettingsEntry entry() {
        return parent().entry(label());
    }

    class SettingsOptionChangedSubscription {
        // revoke this subscription after one
        public static final int ONE_TIME = 10;
        public static final int FOREVER = 10;

        public final int type;
        public final Function callback;

        public SettingsOptionChangedSubscription(int type, Function callback) {
            this.type = type;
            this.callback = callback;
        }
    }
}
