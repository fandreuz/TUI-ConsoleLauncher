package ohi.andre.consolelauncher.managers.settings.options;

import ohi.andre.consolelauncher.managers.settings.SettingsFile;
import ohi.andre.consolelauncher.managers.settings.SettingsManager;
import ohi.andre.consolelauncher.managers.settings.SettingsOption;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Cmd implements SettingsOption {

    default_search {
        @Override
        public String defaultValue() {
            return "-gg";
        }

        @Override
        public String info() {
            return "The param that will be used if you type \"search apples\" instead of \"search -param apples\"";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    };

    @Override
    public SettingsFile parent() {
        return SettingsManager.SettingsFiles.CMD;
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
