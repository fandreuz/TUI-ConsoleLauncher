package ohi.andre.consolelauncher.features.settings.options;

import android.os.Build;

import ohi.andre.consolelauncher.features.notifications.reply.ReplyManager;
import ohi.andre.consolelauncher.features.settings.SettingsFile;
import ohi.andre.consolelauncher.features.settings.SettingsOption;

/**
 * Created by francescoandreuzzi on 17/01/2018.
 */

public enum Reply implements SettingsOption {

    reply_enabled {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, notification reply will be disabled";
        }
    };

    @Override
    public SettingsFile parent() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) return ReplyManager.instance;
        else return null;
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
