package ohi.andre.consolelauncher.managers.xml.options;

import android.os.Build;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Ui implements XMLPrefsSave {

    show_enter_button {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "Hide/show the enter button";
        }
    },
    system_font {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the default t-ui font (\"Lucida Console\") will be used for all texts";
        }
    },
    ram_size {
        @Override
        public String defaultValue() {
            return "13";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The ram label font size";
        }
    },
    battery_size {
        @Override
        public String defaultValue() {
            return "13";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The battery label font size";
        }
    },
    device_size {
        @Override
        public String defaultValue() {
            return "13";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The device label font size";
        }
    },
    time_size {
        @Override
        public String defaultValue() {
            return "13";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The time label font size";
        }
    },
    storage_size {
        @Override
        public String defaultValue() {
            return "13";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The storage label font size";
        }
    },
    network_size {
        @Override
        public String defaultValue() {
            return "13";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The network label font size";
        }
    },
    notes_size {
        @Override
        public String defaultValue() {
            return "13";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "Notes size";
        }
    },
    input_output_size {
        @Override
        public String defaultValue() {
            return "15";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The input/output font size";
        }
    },
    input_bottom {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the input field will be placed on top of the screen";
        }
    },
    show_ram {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the RAM label will be hidden";
        }
    },
    show_device_name {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the device label will be hidden";
        }
    },
    show_battery {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the battery label will be hidden";
        }
    },
    show_network_info {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the network info label will be hidden";
        }
    },
    show_storage_info {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the time label will be hidden";
        }
    },
    show_notes {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the notes label will be hidden";
        }
    },
    enable_battery_status {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, battery color will change when your battery level reach different percentages battery_color high, battery_color_medium, battery_color_low. If false, only battery_color_high is used";
        }
    },
    show_time {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, the time label will be hidden";
        }
    },
    username {
        @Override
        public String defaultValue() {
            return "user";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "Your username";
        }
    },
    deviceName {
        @Override
        public String defaultValue() {
            return Build.DEVICE;
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "Your device name";
        }
    },
    system_wallpaper {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, your system wallpaper will be used as background";
        }
    },
    fullscreen {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, t-ui will run in fullscreen mode";
        }
    },
    device_index {
        @Override
        public String defaultValue() {
            return "0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "This is used to order the labels on top of the screen";
        }
    },
    ram_index {
        @Override
        public String defaultValue() {
            return "1";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "This is used to order the labels on top of the screen";
        }
    },
    battery_index {
        @Override
        public String defaultValue() {
            return "2";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "This is used to order the labels on top of the screen";
        }
    },
    time_index {
        @Override
        public String defaultValue() {
            return "3";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "This is used to order the labels on top of the screen";
        }
    },
    storage_index {
        @Override
        public String defaultValue() {
            return "4";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "This is used to order the labels on top of the screen";
        }
    },
    network_index {
        @Override
        public String defaultValue() {
            return "5";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "This is used to order the labels on top of the screen";
        }
    },
    notes_index {
        @Override
        public String defaultValue() {
            return "6";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "This is used to order the labels on top of the screen";
        }
    },
    status_line0_alignment {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The alignment of the 1st status line (<0 = left, =0 = center, >0 = right)";
        }
    },
    status_line1_alignment {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The alignment of the 2nd status line (<0 = left, =0 = center, >0 = right)";
        }
    },
    status_line2_alignment {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The alignment of the 3rd status line (<0 = left, =0 = center, >0 = right)";
        }
    },
    status_line3_alignment {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The alignment of the 4th status line (<0 = left, =0 = center, >0 = right)";
        }
    },
    status_line4_alignment {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The alignment of the 5th status line (<0 = left, =0 = center, >0 = right)";
        }
    },
    status_line5_alignment {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The alignment of the 6th status line (<0 = left, =0 = center, >0 = right)";
        }
    },
    status_line6_alignment {
        @Override
        public String defaultValue() {
            return "0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The alignment of the 7th status line (<0 = left, =0 = center, >0 = right)";
        }
    },
    input_prefix {
        @Override
        public String defaultValue() {
            return "$";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The prefix placed before every input";
        }
    },
    input_root_prefix {
        @Override
        public String defaultValue() {
            return "#";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The prefix placed before a root command (\"su ...\")";
        }
    },
    left_margin_mm {
        @Override
        public String defaultValue() {
            return "0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The left margin (in millimeters)";
        }
    },
    right_margin_mm {
        @Override
        public String defaultValue() {
            return "0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The right margin (in millimeters)";
        }
    },
    top_margin_mm {
        @Override
        public String defaultValue() {
            return "0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The top margin (in millimeters)";
        }
    },
    bottom_margin_mm {
        @Override
        public String defaultValue() {
            return "0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The bottom margin (in millimeters)";
        }
    },
    ignore_bar_color {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, statusbar_color and navigationbar_color will be ignored";
        }
    },
    show_app_installed {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, you will receive a message when you install an app";
        }
    },
    show_app_uninstalled {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, you will receive a message when you uninstall an app";
        }
    },
    show_session_info {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, when your input field is empty there will be a short line containing some information about the current session";
        }
    },
    notes_header {
        @Override
        public String defaultValue() {
            return "%( --- Notes : %c ---%n/No notes)";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The header above your notes";
        }
    },
    notes_footer {
        @Override
        public String defaultValue() {
            return "%(%n --- ----- ---/)";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The footer below your notes";
        }
    },
    notes_divider {
        @Override
        public String defaultValue() {
            return "%n";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The divider between two notes";
        }
    };

    @Override
    public XMLPrefsElement parent() {
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