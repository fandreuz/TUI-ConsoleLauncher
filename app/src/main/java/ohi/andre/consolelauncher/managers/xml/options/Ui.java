package ohi.andre.consolelauncher.managers.xml.options;

import android.os.Build;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Ui implements XMLPrefsManager.XMLPrefsSave {

    show_enter_button {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    system_font {
        @Override
        public String defaultValue() {
            return "false";
        }
    },
    ram_size {
        @Override
        public String defaultValue() {
            return "13";
        }
    },
    battery_size {
        @Override
        public String defaultValue() {
            return "13";
        }
    },
    device_size {
        @Override
        public String defaultValue() {
            return "13";
        }
    },
    time_size {
        @Override
        public String defaultValue() {
            return "13";
        }
    },
    storage_size {
        @Override
        public String defaultValue() {
            return "13";
        }
    },
    network_size {
        @Override
        public String defaultValue() {
            return "13";
        }
    },
    input_output_size {
        @Override
        public String defaultValue() {
            return "13";
        }
    },
    input_bottom {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_ram {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_device_name {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_battery {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_network_info {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_storage_info {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    enable_battery_status {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_time {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    username {
        @Override
        public String defaultValue() {
            return "user";
        }
    },
    deviceName {
        @Override
        public String defaultValue() {
            return Build.DEVICE;
        }
    },
    system_wallpaper {
        @Override
        public String defaultValue() {
            return "false";
        }
    },
    fullscreen {
        @Override
        public String defaultValue() {
            return "false";
        }
    },
    device_index {
        @Override
        public String defaultValue() {
            return "0";
        }
    },
    ram_index {
        @Override
        public String defaultValue() {
            return "1";
        }
    },
    battery_index {
        @Override
        public String defaultValue() {
            return "2";
        }
    },
    time_index {
        @Override
        public String defaultValue() {
            return "3";
        }
    },
    storage_index {
        @Override
        public String defaultValue() {
            return "4";
        }
    },
    network_index {
        @Override
        public String defaultValue() {
            return "5";
        }
    },
    status_line0_position {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    status_line1_position {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    status_line2_position {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    status_line3_position {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    status_line4_position {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    status_line5_position {
        @Override
        public String defaultValue() {
            return "-1";
        }
    },
    input_prefix {
        @Override
        public String defaultValue() {
            return "$";
        }
    },
    input_root_prefix {
        @Override
        public String defaultValue() {
            return "#";
        }
    },
    left_margin_mm {
        @Override
        public String defaultValue() {
            return "0";
        }
    },
    right_margin_mm {
        @Override
        public String defaultValue() {
            return "0";
        }
    },
    top_margin_mm {
        @Override
        public String defaultValue() {
            return "0";
        }
    },
    bottom_margin_mm {
        @Override
        public String defaultValue() {
            return "0";
        }
    },
    ignore_bar_color {
        @Override
        public String defaultValue() {
            return "false";
        }
    },
    show_app_installed {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_app_uninstalled {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    show_session_info {
        @Override
        public String defaultValue() {
            return "true";
        }
    };

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return XMLPrefsManager.XMLPrefsRoot.UI;
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