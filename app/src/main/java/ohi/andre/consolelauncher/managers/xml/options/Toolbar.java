package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.xml.SettingsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.SettingsOption;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Toolbar implements SettingsOption {

    show_toolbar {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, the toolbar is hidden";
        }

        @Override
        public String type() {
            return SettingsOption.BOOLEAN;
        }
    },
    hide_toolbar_no_input {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, the toolbar will be hidden when the input field is empty";
        }

        @Override
        public String type() {
            return SettingsOption.BOOLEAN;
        }
    };

    @Override
    public XMLPrefsElement parent() {
        return SettingsManager.XMLPrefsRoot.TOOLBAR;
    }

    @Override
    public String label() {
        return name();
    }

    @Override
    public String[] invalidValues() {
        return null;
    }

    @Override
    public String getLowercaseString() {
        return label();
    }

    @Override
    public String getString() {
        return label();
    }
}