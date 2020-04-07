package ohi.andre.consolelauncher.settings.options;

import ohi.andre.consolelauncher.settings.SettingsManager;
import ohi.andre.consolelauncher.settings.SettingsFile;
import ohi.andre.consolelauncher.settings.SettingsOption;

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
            return SettingsOption.TYPE_BOOLEAN;
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
            return SettingsOption.TYPE_BOOLEAN;
        }
    };

    @Override
    public SettingsFile parent() {
        return SettingsManager.SettingsFiles.TOOLBAR;
    }

    @Override
    public String label() {
        return name();
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
