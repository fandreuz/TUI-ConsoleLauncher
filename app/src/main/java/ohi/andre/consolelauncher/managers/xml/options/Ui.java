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
    status_lines_alignment {
        @Override
        public String defaultValue() {
            return "0,-1,-1,-1,-1,-1,-1,-1,-1";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The alignment of the nth status line (<0 = left, =0 = center, >0 = right)";
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
    display_margin_mm {
        @Override
        public String defaultValue() {
            return "0,0,0,0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "[left margin],[top margin],[right margin],[bottom margin]";
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
    },
    show_restart_message {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, the restart message won\'t be shown";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    notes_max_lines {
        @Override
        public String defaultValue() {
            return "10";
        }

        @Override
        public String info() {
            return "The max number of lines of notes (-1 to disable)";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }
    },
    show_scroll_notes_message {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, you will get a message when your notes reach the value set in notes_max_lines";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    show_weather {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, you will see a label containing the weather in your area";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    weather_index {
        @Override
        public String defaultValue() {
            return "7";
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
    weather_size {
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
            return "Weather size";
        }
    },
    show_unlock_counter {
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
            return "If false, the unlock counter feature will be disabled";
        }
    },
    unlock_index {
        @Override
        public String defaultValue() {
            return "8";
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
    unlock_size {
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
            return "Unlock size";
        }
    },
    statusbar_light_icons {
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
            return "If true, your status bar icons will be white. Dark otherwise";
        }
    },
    bgrect_params {
        @Override
        public String defaultValue() {
            return "2,0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "[Stroke width],[Rect corner radius]";
        }
    },
    shadow_params {
        @Override
        public String defaultValue() {
            return "2,2,0.2";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "[Shadow X offset],[Shadow Y offset],[Shadow radius]";
        }
    },
    text_redraw_times {
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
            return "A greater value will produce a bigger outline";
        }
    },
    status_lines_margins {
        @Override
        public String defaultValue() {
            return "3,3,0,0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "[horizontal_margin],[vertical_margin],[horizontal_padding],[vertical_padding]";
        }
    },
    output_field_margins {
        @Override
        public String defaultValue() {
            return "3,3,0,0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "[horizontal_margin],[vertical_margin],[horizontal_padding],[vertical_padding]";
        }
    },
    input_field_margins {
        @Override
        public String defaultValue() {
            return "3,3,0,0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The dimension of the input field (where cmds are inserted). [horizontal_margin],[vertical_margin],[horizontal_padding],[vertical_padding]";
        }
    },
    input_area_margins {
        @Override
        public String defaultValue() {
            return "3,3,0,0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The dimension of the input area (prefix + input field + toolbar + suggestions). [horizontal_margin],[vertical_margin],[horizontal_padding],[vertical_padding]";
        }
    },
    toolbar_margins {
        @Override
        public String defaultValue() {
            return "3,3,0,0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "[horizontal_margin],[vertical_margin],[horizontal_padding],[vertical_padding]";
        }
    },
    suggestions_area_margin {
        @Override
        public String defaultValue() {
            return "3,3,0,0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "[horizontal_margin],[vertical_margin],[horizontal_padding],[vertical_padding]";
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