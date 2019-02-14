package ohi.andre.consolelauncher.commands.main.raw;

import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.RssManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Apps;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;
import ohi.andre.consolelauncher.managers.xml.options.Rss;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

import static ohi.andre.consolelauncher.UIManager.PREFS_NAME;

/**
 * Created by francescoandreuzzi on 11/06/2017.
 */

public class config extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        set {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                String value = pack.getString();
                save.parent().write(save, value);

                ((Reloadable) pack.context).addMessage(save.parent().path(), save.label() + " -> " + value);

                if(save.label().startsWith("default_app_n")) {
                    return pack.context.getString(R.string.output_usedefapp);
                } else if(save == Behavior.unlock_counter_cycle_start) {
                    SharedPreferences preferences = pack.context.getSharedPreferences(PREFS_NAME, 0);
                    preferences.edit()
                            .putLong(UIManager.NEXT_UNLOCK_CYCLE_RESTART, 0)
                            .putInt(UIManager.UNLOCK_KEY, 0)
                            .apply();
                }

                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                pack.args = new Object[] {pack.args[1], Tuils.EMPTYSTRING};
                return set.exec(pack);
            }
        },
        info {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();

                return "Type:" + Tuils.SPACE + save.type() + Tuils.NEWLINE
                        + "Default:" + Tuils.SPACE + save.defaultValue() + Tuils.NEWLINE
                        + save.info();
            }
        },
        file {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_FILE};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = new File(Tuils.getFolder(), pack.getString());

                try {
                    pack.context.startActivity(Tuils.openFile(pack.context, file));
                } catch (ActivityNotFoundException e) {
                    Tuils.log("nf");
                    Tuils.toFile(e);
                } catch (Exception ex) {
                    Tuils.log(ex);
                    Tuils.toFile(ex);
                }

                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_filenotfound);
            }
        },
        append {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                String value = XMLPrefsManager.get(save) + pack.getString();

                save.parent().write(save, value);

                ((Reloadable) pack.context).addMessage(save.parent().path(), save.label() + " -> " + value);

                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                pack.args = new Object[] {pack.args[0], pack.args[1], Tuils.EMPTYSTRING};
                return set.exec(pack);
            }
        },
        erase {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                save.parent().write(save, Tuils.EMPTYSTRING);

                ((Reloadable) pack.context).addMessage(save.parent().path(), save.label() + " -> " + "\"\"");

                return null;
            }
        },
        get {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                String s = XMLPrefsManager.get(String.class, save);
                if(s.length() == 0) return "\"\"";
                return s;
            }
        },
        ls {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_FILE};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = new File(Tuils.getFolder(), pack.getString());
                String name = file.getName();

                for(XMLPrefsManager.XMLPrefsRoot r : XMLPrefsManager.XMLPrefsRoot.values()) {
                    if(name.equalsIgnoreCase(r.path)) {
                        List<String> strings = r.getValues().values();
                        Tuils.addPrefix(strings, Tuils.DOUBLE_SPACE);
                        strings.add(0, r.path);
                        return Tuils.toPlanString(strings, Tuils.NEWLINE);
                    }
                }

                if(name.equalsIgnoreCase(AppsManager.PATH)) {
                    List<String> strings = AppsManager.instance.getValues().values();
                    Tuils.addPrefix(strings, Tuils.DOUBLE_SPACE);
                    strings.add(0, AppsManager.PATH);
                    return Tuils.toPlanString(strings, Tuils.NEWLINE);
                }

                if(name.equalsIgnoreCase(NotificationManager.PATH)) {
                    List<String> strings = NotificationManager.instance.getValues().values();
                    Tuils.addPrefix(strings, Tuils.DOUBLE_SPACE);
                    strings.add(0, NotificationManager.PATH);
                    return Tuils.toPlanString(strings, Tuils.NEWLINE);
                }

                if(name.equalsIgnoreCase(RssManager.PATH)) {
                    List<String> strings = NotificationManager.instance.getValues().values();
                    Tuils.addPrefix(strings, Tuils.DOUBLE_SPACE);
                    strings.add(0, RssManager.PATH);
                    return Tuils.toPlanString(strings, Tuils.NEWLINE);
                }

                return "[]";
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_filenotfound);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                List<String> ss = new ArrayList<>();

                for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values()) {
                    ss.add(element.path);
                    for(XMLPrefsSave save : element.enums) {
                        ss.add(Tuils.DOUBLE_SPACE + save.label());
                    }
                }
                ss.add(AppsManager.PATH);
                for(XMLPrefsSave save : Apps.values()) {
                    ss.add(Tuils.DOUBLE_SPACE + save.label());
                }
                ss.add(NotificationManager.PATH);
                for(XMLPrefsSave save : Notifications.values()) {
                    ss.add(Tuils.DOUBLE_SPACE + save.label());
                }
                ss.add(RssManager.PATH);
                for(XMLPrefsSave save : Rss.values()) {
                    ss.add(Tuils.DOUBLE_SPACE + save.label());
                }

                return Tuils.toPlanString(ss);
            }
        },
        fontsize {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsElement parent = Ui.device_size.parent();

                int size = pack.getInt();

                parent.write(Ui.device_size, String.valueOf(size));
                parent.write(Ui.ram_size, String.valueOf(size));
                parent.write(Ui.network_size, String.valueOf(size));
                parent.write(Ui.storage_size, String.valueOf(size));
                parent.write(Ui.battery_size, String.valueOf(size));
                parent.write(Ui.notes_size, String.valueOf(size));
                parent.write(Ui.time_size, String.valueOf(size));
                parent.write(Ui.weather_size, String.valueOf(size));
                parent.write(Ui.unlock_size, String.valueOf(size));
                parent.write(Ui.input_output_size, String.valueOf(size));

                return null;
            }
        },
        reset {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                save.parent().write(save, save.defaultValue());

                ((Reloadable) pack.context).addMessage(save.parent().path(), save.label() + " -> " + save.defaultValue());

                return null;
            }
        },
        apply {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.FILE};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = pack.get(File.class);

                if(!file.getName().endsWith(".xml")) {
//                    is font
                    if(Tuils.fontPath != null) {
                        File font = new File(Tuils.fontPath);
                        if (font.exists()) {
                            File[] files = font.listFiles();
                            if (files.length > 0) Tuils.insertOld(files[0]);
                            Tuils.deleteContentOnly(font);
                        } else {
                            font.mkdir();
                        }
                    }
                } else {
                    File toPutInsideOld = new File(Tuils.getFolder(), file.getName());
                    Tuils.insertOld(toPutInsideOld);
                }

                File dest = new File(Tuils.getFolder(), file.getName());
                file.renameTo(dest);

                return "Path: " + dest.getAbsolutePath();
            }
        },
        tutorial {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                pack.context.startActivity(Tuils.webPage("https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/Customize-T_UI"));
                return null;
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps) if (p.endsWith(p1.label())) return p1;
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
            return pack.context.getString(R.string.help_config);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.context.getString(R.string.output_invalidarg);
        }
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_config;
    }
}
