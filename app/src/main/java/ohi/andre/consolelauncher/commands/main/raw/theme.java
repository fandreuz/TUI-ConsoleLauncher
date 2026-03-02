package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import android.graphics.Color;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.ThemeManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Suggestions;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

public class theme extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        apply {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent intent = new Intent(ThemeManager.ACTION_APPLY);
                intent.putExtra(ThemeManager.NAME, pack.getString());
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(intent);
                return null;
            }
        },
        set {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Object o = pack.get();
                if (!(o instanceof Theme)) {
                    return "Invalid theme element.";
                }
                Theme element = (Theme) o;
                String color = pack.getString();

                try {
                    Color.parseColor(color);
                } catch (Exception e) {
                    return "Invalid color format. Use #RRGGBB or #AARRGGBB";
                }

                XMLPrefsManager.XMLPrefsRoot.THEME.write(element, color);

                try {
                    if (pack.context instanceof Reloadable) {
                        ((Reloadable) pack.context).reload();
                    }
                } catch (Exception e) {}

                return element.label() + " updated to " + color;
            }
        },
        preset {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.THEME_PRESET};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString().toLowerCase();
                
                Map<Theme, String> colors = new HashMap<>();
                Map<Suggestions, String> suggestionColors = new HashMap<>();
                
                // Common transparency for background (90% opacity = E6)
                String transBg = "#E6";

                switch(name) {
                    case "blue":
                        colors.put(Theme.bg_color, transBg + "001221");
                        colors.put(Theme.input_color, "#00BFFF");
                        colors.put(Theme.output_color, "#E0FFFF");
                        colors.put(Theme.device_color, "#1E90FF");
                        colors.put(Theme.enter_color, "#00BFFF");
                        colors.put(Theme.toolbar_color, "#00BFFF");
                        colors.put(Theme.time_color, "#87CEFA");

                        suggestionColors.put(Suggestions.apps_bg_color, "#0000FF");
                        suggestionColors.put(Suggestions.alias_bg_color, "#4169E1");
                        suggestionColors.put(Suggestions.cmd_bg_color, "#00BFFF");
                        suggestionColors.put(Suggestions.file_bg_color, "#87CEFA");
                        suggestionColors.put(Suggestions.song_bg_color, "#1E90FF");
                        break;
                    case "red":
                        colors.put(Theme.bg_color, transBg + "210000");
                        colors.put(Theme.input_color, "#FF4500");
                        colors.put(Theme.output_color, "#FFEBEE");
                        colors.put(Theme.device_color, "#B71C1C");
                        colors.put(Theme.enter_color, "#FF0000");
                        colors.put(Theme.toolbar_color, "#FF5252");
                        colors.put(Theme.time_color, "#FF8A80");

                        suggestionColors.put(Suggestions.apps_bg_color, "#FF0000");
                        suggestionColors.put(Suggestions.alias_bg_color, "#DC143C");
                        suggestionColors.put(Suggestions.cmd_bg_color, "#FF4500");
                        suggestionColors.put(Suggestions.file_bg_color, "#FA8072");
                        suggestionColors.put(Suggestions.song_bg_color, "#B22222");
                        break;
                    case "green":
                        colors.put(Theme.bg_color, transBg + "001B00");
                        colors.put(Theme.input_color, "#00FF41");
                        colors.put(Theme.output_color, "#D5F5E3");
                        colors.put(Theme.device_color, "#2ECC71");
                        colors.put(Theme.enter_color, "#00FF41");
                        colors.put(Theme.toolbar_color, "#27AE60");
                        colors.put(Theme.time_color, "#A9DFBF");

                        suggestionColors.put(Suggestions.apps_bg_color, "#00FF00");
                        suggestionColors.put(Suggestions.alias_bg_color, "#32CD32");
                        suggestionColors.put(Suggestions.cmd_bg_color, "#00FF41");
                        suggestionColors.put(Suggestions.file_bg_color, "#90EE90");
                        suggestionColors.put(Suggestions.song_bg_color, "#228B22");
                        break;
                    case "pink":
                        colors.put(Theme.bg_color, transBg + "1A0010");
                        colors.put(Theme.input_color, "#FF69B4");
                        colors.put(Theme.output_color, "#FCE4EC");
                        colors.put(Theme.device_color, "#AD1457");
                        colors.put(Theme.enter_color, "#FF1493");
                        colors.put(Theme.toolbar_color, "#F06292");
                        colors.put(Theme.time_color, "#F8BBD0");

                        suggestionColors.put(Suggestions.apps_bg_color, "#FF69B4");
                        suggestionColors.put(Suggestions.alias_bg_color, "#FF1493");
                        suggestionColors.put(Suggestions.cmd_bg_color, "#FFB6C1");
                        suggestionColors.put(Suggestions.file_bg_color, "#FFC0CB");
                        suggestionColors.put(Suggestions.song_bg_color, "#C71585");
                        break;
                    case "bw":
                        colors.put(Theme.bg_color, transBg + "000000");
                        colors.put(Theme.input_color, "#FFFFFF");
                        colors.put(Theme.output_color, "#CCCCCC");
                        colors.put(Theme.device_color, "#AAAAAA");
                        colors.put(Theme.enter_color, "#FFFFFF");
                        colors.put(Theme.toolbar_color, "#FFFFFF");
                        colors.put(Theme.time_color, "#FFFFFF");

                        suggestionColors.put(Suggestions.apps_bg_color, "#FFFFFF");
                        suggestionColors.put(Suggestions.alias_bg_color, "#EEEEEE");
                        suggestionColors.put(Suggestions.cmd_bg_color, "#DDDDDD");
                        suggestionColors.put(Suggestions.file_bg_color, "#CCCCCC");
                        suggestionColors.put(Suggestions.song_bg_color, "#BBBBBB");
                        
                        suggestionColors.put(Suggestions.apps_text_color, "#000000");
                        suggestionColors.put(Suggestions.alias_text_color, "#000000");
                        suggestionColors.put(Suggestions.cmd_text_color, "#000000");
                        suggestionColors.put(Suggestions.file_text_color, "#000000");
                        suggestionColors.put(Suggestions.song_text_color, "#000000");
                        break;
                    case "cyberpunk":
                        colors.put(Theme.bg_color, transBg + "0D0615");
                        colors.put(Theme.input_color, "#FCEE09"); 
                        colors.put(Theme.output_color, "#00F0FF"); 
                        colors.put(Theme.device_color, "#FF003C"); 
                        colors.put(Theme.enter_color, "#FCEE09");
                        colors.put(Theme.toolbar_color, "#39FF14"); 
                        colors.put(Theme.time_color, "#00F0FF");

                        suggestionColors.put(Suggestions.apps_bg_color, "#FF003C");
                        suggestionColors.put(Suggestions.alias_bg_color, "#FCEE09");
                        suggestionColors.put(Suggestions.cmd_bg_color, "#00F0FF");
                        suggestionColors.put(Suggestions.file_bg_color, "#39FF14");
                        suggestionColors.put(Suggestions.song_bg_color, "#BC00FF");
                        
                        suggestionColors.put(Suggestions.alias_text_color, "#000000");
                        break;
                    default:
                        return "Unknown preset. Available: blue, red, green, pink, bw, cyberpunk";
                }

                // Force toolbar_bg to transparent to avoid coloring the entire bar
                colors.put(Theme.toolbar_bg, "#00000000");

                for (Map.Entry<Theme, String> entry : colors.entrySet()) {
                    XMLPrefsManager.XMLPrefsRoot.THEME.write(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<Suggestions, String> entry : suggestionColors.entrySet()) {
                    XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.write(entry.getKey(), entry.getValue());
                }

                try {
                    if (pack.context instanceof Reloadable) {
                        ((Reloadable) pack.context).reload();
                    }
                } catch (Exception e) {}

                return "Applied " + name + " preset!";
            }
        },
        standard {
            @Override
            public int[] args() {
                return new int[] {};
            }

            @Override
            public String exec(ExecutePack pack) {
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(new Intent(ThemeManager.ACTION_STANDARD));
                return null;
            }
        },
        old {
            @Override
            public String exec(ExecutePack pack) {
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(new Intent(ThemeManager.ACTION_REVERT));
                return null;
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps)
                if (p.endsWith(p1.label()))
                    return p1;
            return null;
        }

        static String[] labels() {
            Param[] ps = values();
            String[] ss = new String[ps.length];

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.context.getString(R.string.help_theme);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return null;
        }

        @Override
        public int[] args() {
            return new int[0];
        }
    }

    @Override
    public String[] params() {
        return Param.labels();
    };

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_theme;
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }
}
