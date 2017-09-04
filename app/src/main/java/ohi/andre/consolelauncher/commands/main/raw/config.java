package ohi.andre.consolelauncher.commands.main.raw;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.tuils.Tuils;

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
                XMLPrefsManager.XMLPrefsSave save = pack.get(XMLPrefsManager.XMLPrefsSave.class, 1);
                save.parent().write(save, pack.get(String.class, 2));

                if(save.label().startsWith("default_app_n")) {
                    return pack.context.getString(R.string.output_usedefapp);
                }
                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                pack.args = new Object[] {pack.args[0], pack.args[1], Tuils.EMPTYSTRING};
                return set.exec(pack);
            }
        },
        file {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_FILE};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = new File(Tuils.getFolder(), pack.get(String.class, 1));
                pack.context.startActivity(Tuils.openFile(file));
                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_filenotfound);
            }
        },
        get {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsManager.XMLPrefsSave save = pack.get(XMLPrefsManager.XMLPrefsSave.class, 1);
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
                File file = new File(Tuils.getFolder(), pack.get(String.class, 1));
                String name = file.getName();

                for(XMLPrefsManager.XMLPrefsRoot r : XMLPrefsManager.XMLPrefsRoot.values()) {
                    if(name.equalsIgnoreCase(r.path)) {
                        List<String> strings = r.getValues().values();
                        Tuils.addPrefix(strings, Tuils.DOUBLE_SPACE);
                        strings.add(0, r.path);
                        return Tuils.toPlanString(strings, Tuils.NEWLINE);
                    }
                }

                return "[]";
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                List<String> ss = new ArrayList<>();

                for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values()) {
                    ss.add(element.path);
                    for(XMLPrefsManager.XMLPrefsSave save : element.copy) {
                        ss.add(Tuils.DOUBLE_SPACE + save.label());
                    }
                }
                ss.add(AppsManager.PATH);
                for(XMLPrefsManager.XMLPrefsSave save : AppsManager.Options.values()) {
                    ss.add(Tuils.DOUBLE_SPACE + save.label());
                }
                ss.add(NotificationManager.PATH);
                for(XMLPrefsManager.XMLPrefsSave save : NotificationManager.Options.values()) {
                    ss.add(Tuils.DOUBLE_SPACE + save.label());
                }

                return Tuils.toPlanString(ss);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return onArgNotFound(pack, -1);
            }
        },
        reset {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsManager.XMLPrefsSave save = pack.get(XMLPrefsManager.XMLPrefsSave.class, 1);
                save.parent().write(save, save.defaultValue());
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
                File file = pack.get(File.class, 1);

                File old = new File(Tuils.getFolder(), file.getName());
                Tuils.insertOld(old);

                file.renameTo(new File(Tuils.getFolder(), file.getName()));

                return null;
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
