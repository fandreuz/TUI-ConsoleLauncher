package ohi.andre.consolelauncher.managers.settings.options;

import android.os.Environment;

import java.io.File;

import ohi.andre.consolelauncher.managers.settings.SettingsManager;
import ohi.andre.consolelauncher.managers.settings.SettingsFile;
import ohi.andre.consolelauncher.managers.settings.SettingsOption;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Behavior implements SettingsOption {

    double_tap_lock {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, t-ui will lock the screen on double tap";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    double_tap_cmd {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "The command that will run when you touch two times the screen quickly";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    random_play {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, music player will play your tracks in random order";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    songs_folder {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "The folder that contains your music files";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    songs_from_mediastore {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, t-ui will get tracks from the system mediastore";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    tui_notification {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, there will always be a notification in your status bar, telling you that t-ui is running";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    auto_show_keyboard {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, your keyboard will be shown everytime you go back to t-ui";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    auto_scroll {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, the terminal will be automatically scrolled down when the keyboard is open";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    show_hints {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, t-ui will tell you some useful hints sometime";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    show_alias_content {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, when you use an alias you'll also be able to know what command has been executed";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    show_launch_history {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, t-ui won't show the apps that you launch";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    clear_after_cmds {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String info() {
            return "Auto-clear after n commands (if -1, this feature will be disabled)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    clear_after_seconds {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String info() {
            return "Auto-clear after n seconds (if -1, this feature will be disabled)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    max_lines {
        @Override
        public String defaultValue() {
            return "-1";
        }

        @Override
        public String info() {
            return "Set maximum number of lines that will be shown in the terminal (if -1, this feature is be disabled)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    time_format {
        @Override
        public String defaultValue() {
            return "d MMM yyyy HH:mm:ss@HH:mm:ss";
        }

        @Override
        public String info() {
            return "Define the time format (see also Time Format)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    time_format_separator {
        @Override
        public String defaultValue() {
            return "@";
        }

        @Override
        public String info() {
            return "This is the separator between your different time formats";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    battery_medium {
        @Override
        public String defaultValue() {
            return "50";
        }

        @Override
        public String info() {
            return "The percentage below which the battery level will be considered \"medium\"";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    battery_low {
        @Override
        public String defaultValue() {
            return "15";
        }

        @Override
        public String info() {
            return "The percentage below which the battery level will be considered \"low\"";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    // todo: fill
    device_info_format {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    input_format {
        @Override
        public String defaultValue() {
            return "[%t] %p %i";
        }

        @Override
        public String info() {
            return "Define the input format ";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    output_format {
        @Override
        public String defaultValue() {
            return "%o";
        }

        @Override
        public String info() {
            return "Define the output format ";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    session_info_format {
        @Override
        public String defaultValue() {
            return "%u@%d:%p";
        }

        @Override
        public String info() {
            return "Define the session info format";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    enable_app_launch {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, you won't be able to launch apps from t-ui, unless you use \"apps -frc\"";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    app_launch_format {
        @Override
        public String defaultValue() {
            return "--> %a";
        }

        @Override
        public String info() {
            return "Define app launch format ";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    alias_param_marker {
        @Override
        public String defaultValue() {
            return "%";
        }

        @Override
        public String info() {
            return "Define the marker that will be replaced with a provided param";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    alias_param_separator {
        @Override
        public String defaultValue() {
            return ",";
        }

        @Override
        public String info() {
            return "Define the separator between a group of params";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    alias_replace_all_markers {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, if you pass a lower number of parameters to an alias, t-ui will use the first one to replace the others";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    multiple_cmd_separator {
        @Override
        public String defaultValue() {
            return ";";
        }

        @Override
        public String info() {
            return "The separator between two or more commands in a single input";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    alias_content_format {
        @Override
        public String defaultValue() {
            return "%a --> [%v]";
        }

        @Override
        public String info() {
            return "Define the format used to show your alias contents ";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    external_storage_path {
        @Override
        public String defaultValue() {
            String path = System.getenv("SECONDARY_STORAGE");
            if (path == null) return "";

            File file = new File(path);
            if (file != null && file.exists()) return file.getAbsolutePath();

            return "";
        }

        @Override
        public String info() {
            return "The path to your external storage (used to evaluate free/total space)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    home_path {
        @Override
        public String defaultValue() {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        @Override
        public String info() {
            return "The path to your home directory";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    enable_music {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, you will be able to use t-ui as a music player. Otherwise, the music command will try to communicate with the music player that your using";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    max_optional_depth {
        @Override
        public String defaultValue() {
            return "2";
        }

        @Override
        public String info() {
            return "A value which is used to tell how deep t-ui can go in a nested optional value";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    network_info_update_ms {
        @Override
        public String defaultValue() {
            return "3500";
        }

        @Override
        public String info() {
            return "The time between two network info updates";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    tui_notification_title {
        @Override
        public String defaultValue() {
            return "T-UI";
        }

        @Override
        public String info() {
            return "The title of the T-UI notification";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    tui_notification_subtitle {
        @Override
        public String defaultValue() {
            return "T-UI is running";
        }

        @Override
        public String info() {
            return "The subtitle of the T-UI notification";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    tui_notification_click_cmd {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "The command ran when the T-UI notification is clicked";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    tui_notification_click_showhome {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, the click on the T-UI notification won't bring you to your phone home";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    tui_notification_lastcmds_size {
        @Override
        public String defaultValue() {
            return "5";
        }

        @Override
        public String info() {
            return "The number of used commands that will appear inside the T-UI notification (<0 will disable the feature)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    tui_notification_lastcmds_updown {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, the last used command will appear on top";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    tui_notification_priority {
        @Override
        public String defaultValue() {
            return "0";
        }

        @Override
        public String info() {
            return "The priority of the T-UI notification (min: -2, max: 2)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    long_click_vibration_duration {
        @Override
        public String defaultValue() {
            return "100";
        }

        @Override
        public String info() {
            return "The duration (in milliseconds) of the vibration when you long click a notification or an RSS item (<0 will disable the feature)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    long_click_duration {
        @Override
        public String defaultValue() {
            return "700";
        }

        @Override
        public String info() {
            return "The minimum duration of the long click on a notification or an RSS item";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    click_commands {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, you will be able to use a command again clicking on it";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    long_click_commands {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, you will be able to put a used command in the input field long-clicking it";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    append_quote_before_file {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, t-ui will automatically append a quote before a file inserted clicking on a suggestion";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    optional_values_separator {
        @Override
        public String defaultValue() {
            return "/";
        }

        @Override
        public String info() {
            return "The separator between two optional values (doesn\'t affect notification optional values)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    notes_sorting {
        @Override
        public String defaultValue() {
            return "0";
        }

        @Override
        public String info() {
            return "0 = time up->down; 1 = time down->up; 2 = alphabetical up->down; 3 = alphabetical down->up; 4 = locked before; 5 = unlocked before";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    notes_allow_link {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, adding links to notes will be disallowed (may slightly increase performance)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    orientation {
        @Override
        public String defaultValue() {
            return "2";
        }

        @Override
        public String info() {
            return "0 = landscape, 1 = portrait, 2 = auto-rotate";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    htmlextractor_default_format {
        @Override
        public String defaultValue() {
            return "%t -> %v%n%a(%an = %av)(%n)";
        }

        @Override
        public String info() {
            return "The default format used by htmlextract -use";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    htmlextractor_notfound_message {
        @Override
        public String defaultValue() {
            return "Not found";
        }

        @Override
        public String info() {
            return "The message printed when there are no result inside your htmlextract query";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    tui_notification_time_color {
        @Override
        public String defaultValue() {
            return Theme.time_color.defaultValue();
        }

        @Override
        public String info() {
            return "The time color inside the t-ui notification";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_COLOR;
        }
    },
    tui_notification_input_color {
        @Override
        public String defaultValue() {
            return Theme.input_color.defaultValue();
        }

        @Override
        public String info() {
            return "The input color inside the t-ui notification";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_COLOR;
        }
    },
    weather_key {
        @Override
        public String defaultValue() {
            return "1f798f99228596c20ccfda51b9771a86";
        }

        @Override
        public String info() {
            return "The key of your account on OpenWeatherMap. You can keep the default one, or create your custom key (check the wiki)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    weather_temperature_measure {
        @Override
        public String defaultValue() {
            return "metric";
        }

        @Override
        public String info() {
            return "metric = Celsius; imperial = Fahrenheit; standard = Kelvin";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    weather_location {
        @Override
        public String defaultValue() {
            return "null";
        }

        @Override
        public String info() {
            return "The ID of your country (check the wiki) or your coords separated by a comma (lat,lon)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    weather_format {
        @Override
        public String defaultValue() {
            return "Weather: %main, Temp: %temp";
        }

        @Override
        public String info() {
            return "The format used to show the weather";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    clear_on_lock {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, t-ui will clear the screen when you lock the phone";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    unlock_counter_format {
        @Override
        public String defaultValue() {
            return "Unlocked %c times (%a10/)%n%t(Unlock n. %i --> %w)3";
        }

        @Override
        public String info() {
            return "The format used to show the unlock counter";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    unlock_time_divider {
        @Override
        public String defaultValue() {
            return "%n";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }

        @Override
        public String info() {
            return "The divider between the last unlock times";
        }
    },
    unlock_time_order {
        @Override
        public String defaultValue() {
            return "1";
        }

        @Override
        public String info() {
            return "1 = up-down. 2 = down-up";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    unlock_counter_cycle_start {
        @Override
        public String defaultValue() {
            return "6.00";
        }

        @Override
        public String info() {
            return "The starting hour of the unlock counter cycle (hh.mm)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    not_available_text {
        @Override
        public String defaultValue() {
            return "n/a";
        }

        @Override
        public String info() {
            return "The text shown when a value is not available";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_TEXT;
        }
    },
    back_button_enabled {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, the back button will put the previous command inside the input area";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    weather_update_time {
        @Override
        public String defaultValue() {
            return "3600";
        }

        @Override
        public String info() {
            return "The weather update time in seconds. This can only be used if you\'re using a custom weather key";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    location_update_mintime {
        @Override
        public String defaultValue() {
            return "20";
        }

        @Override
        public String info() {
            return "The amount of time between two location updates (in minutes, must be an integer value)";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    },
    location_update_mindistance {
        @Override
        public String defaultValue() {
            return "500";
        }

        @Override
        public String info() {
            return "The minimum distance (in meters) to get a location update";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_INTEGER;
        }
    },
    show_weather_updates {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, t-ui won't show information about the weather in the output field";
        }

        @Override
        public String type() {
            return SettingsOption.TYPE_BOOLEAN;
        }
    };

    @Override
    public SettingsFile parent() {
        return SettingsManager.SettingsFiles.BEHAVIOR;
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
