package ohi.andre.consolelauncher.managers.xml.options;

import android.os.Environment;

import java.io.File;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Behavior implements XMLPrefsManager.XMLPrefsSave {

    double_tap_lock {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    double_tap_cmd {
        @Override
        public String defaultValue() {
            return "";
        }
    },
    random_play {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    songs_folder {
        @Override
        public String defaultValue() {
            return "";
        }
    },
    songs_from_mediastore {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    tui_notification {
        @Override
        public String defaultValue() {
            return "false";
        }
    },
    auto_show_keyboard {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    auto_scroll {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_hints {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_alias_content {
        @Override
        public String defaultValue() {
            return "false";
        }
    },
    show_launch_history {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    clear_after_cmds {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    clear_after_seconds {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    max_lines {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    time_format {
        @Override
        public String defaultValue() {
            return "%m/%d/%y %H.%M";
        }
    },
    battery_medium {
        @Override
        public String defaultValue() {
            return "50";
        }
    },
    battery_low {
        @Override
        public String defaultValue() {
            return "15";
        }
    },
    device_format {
        @Override
        public String defaultValue() {
            return "%d: %u";
        }
    },
    ram_format {
        @Override
        public String defaultValue() {
            return "Available RAM: %avgb GB of %totgb GB (%av%%)";
        }
    },
    battery_format {
        @Override
        public String defaultValue() {
            return "%(Charging: /)%v%";
        }
    },
    storage_format {
        @Override
        public String defaultValue() {
            return "Internal Storage: %iavgb GB / %itotgb GB (%iav%%)";
        }
    },
    network_info_format {
        @Override
        public String defaultValue() {
            return "%(WiFi - %wn/%[Mobile Data: %d3/No Internet access])";
        }
    },
    input_format {
        @Override
        public String defaultValue() {
            return "[%t] %p %i";
        }
    },
    output_format {
        @Override
        public String defaultValue() {
            return "%o";
        }
    },
    session_info_format {
        @Override
        public String defaultValue() {
            return "%u@%d:%p";
        }
    },
    enable_app_launch {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    app_launch_format {
        @Override
        public String defaultValue() {
            return "--> %a";
        }
    },
    time_format_separator {
        @Override
        public String defaultValue() {
            return "@";
        }
    },
    alias_param_marker {
        @Override
        public String defaultValue() {
            return "%";
        }
    },
    alias_param_separator {
        @Override
        public String defaultValue() {
            return ",";
        }
    },
    multiple_cmd_separator {
        @Override
        public String defaultValue() {
            return ";";
        }
    },
    alias_content_format {
        @Override
        public String defaultValue() {
            return "%a --> [%v]";
        }
    },
    external_storage_path {
        @Override
        public String defaultValue() {
            String path = System.getenv("SECONDARY_STORAGE");
            if(path == null) return Tuils.EMPTYSTRING;

            File file = new File(path);
            if(file != null && file.exists()) return file.getAbsolutePath();

            return Tuils.EMPTYSTRING;
        }
    },
    home_path {
        @Override
        public String defaultValue() {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    },
    app_installed_format {
        @Override
        public String defaultValue() {
            return "App installed: %p";
        }
    },
    app_uninstalled_format {
        @Override
        public String defaultValue() {
            return "App uninstalled: %p";
        }
    },
    enable_music {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    autolower_firstchar {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    max_optional_depth_network_info {
        @Override
        public String defaultValue() {
            return "2";
        }
    },
    network_info_update_ms {
        @Override
        public String defaultValue() {
            return "3500";
        }
    };

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return XMLPrefsManager.XMLPrefsRoot.BEHAVIOR;
    }

    @Override
    public String label() {
        return name();
    }

    @Override
    public boolean is(String s) {
        return name().equals(s);
    }
}