package ohi.andre.consolelauncher.commands.main.raw;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.RssManager;
import ohi.andre.consolelauncher.managers.TimeManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Rss;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 30/09/2017.
 */

public class rss extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        add {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                long tm = pack.get(long.class);
                String url = pack.getString();

                return ((MainPack) pack).rssManager.add(id, tm, url);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.LONG, CommandAbstraction.PLAIN_TEXT};
            }
        },
        rm {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();

                return ((MainPack) pack).rssManager.rm(id);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }


        },
        ls {
            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).rssManager.list();
            }

            @Override
            public int[] args() {
                return new int[0];
            }
        },
        l {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                return ((MainPack) pack).rssManager.l(id);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }
        },
        show {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                boolean show = pack.getBoolean();

                return ((MainPack) pack).rssManager.setShow(id, show);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.BOOLEAN};
            }
        },
        update_time {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                long tm = pack.get(long.class);

                return ((MainPack) pack).rssManager.setTime(id, tm);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.LONG};
            }
        },
        time_format {
            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).rssManager.setTimeFormat(pack.getInt(), pack.getString());
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }
        },
        format {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                String s = pack.getString();

                return ((MainPack) pack).rssManager.setFormat(id, s);
            }
        },
        color {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.COLOR};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                String c = pack.getString();

                return ((MainPack) pack).rssManager.setColor(id, c);
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                if(index == 2) return pack.context.getString(R.string.output_invalidcolor);
                return super.onArgNotFound(pack, index);
            }
        },
        entry_tag {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).rssManager.setEntryTag(pack.getInt(), pack.getString());
            }
        },
        date_tag {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).rssManager.setDateTag(pack.getInt(), pack.getString());
            }
        },
        last_check {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Node n = XMLPrefsManager.findNode(new File(Tuils.getFolder(), RssManager.PATH), RssManager.RSS_LABEL, new String[] {RssManager.ID_ATTRIBUTE}, new String[] {String.valueOf(pack.getInt())});
                if(n == null) return pack.context.getString(R.string.id_notfound);

                Element el = (Element) n;

                String value = el.hasAttribute(RssManager.LASTCHECKED_ATTRIBUTE) ? el.getAttribute(RssManager.LASTCHECKED_ATTRIBUTE) : null;
                if(value == null) return pack.context.getString(R.string.rss_never_checked);

                try {
                    return TimeManager.instance.replace(XMLPrefsManager.get(Rss.rss_time_format), Long.parseLong(value),
                            Integer.MAX_VALUE).toString();
                } catch (Exception e) {
                    Tuils.log(e);
                    return pack.context.getString(R.string.output_error);
                }
            }
        },
        frc {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                if(!((MainPack) pack).rssManager.updateRss(pack.getInt(), false, true)) return pack.context.getString(R.string.id_notfound);
                return null;
            }
        },
        info {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                RssManager.Rss rss = ((MainPack) pack).rssManager.findId(pack.getInt());
                if(rss == null) return pack.context.getString(R.string.id_notfound);

                return rss.toString();
            }
        },
        include_if_matches {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                String r = pack.getString();

                return ((MainPack) pack).rssManager.setIncludeIfMatches(id, r);
            }
        },
        exclude_if_matches {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                String r = pack.getString();

                return ((MainPack) pack).rssManager.setExcludeIfMatches(id, r);
            }
        },
        add_command {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.NO_SPACE_STRING, CommandAbstraction.NO_SPACE_STRING, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();

                String on = pack.getString();
                String regex = pack.getString();
                String cmd = pack.getString();

                try {
                    Pattern.compile(regex);
                } catch (Exception e) {
                    return e.toString();
                }

                return ((MainPack) pack).rssManager.addRegexCommand(id, on, regex, cmd);
            }
        },
        rm_command {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).rssManager.rmRegexCommand(pack.getInt());
            }
        },
        wifi_only {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.BOOLEAN};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                boolean w = pack.getBoolean();

                return ((MainPack) pack).rssManager.setWifiOnly(id, w);
            }
        },
        add_format {
            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).rssManager.addFormat(pack.getInt(), pack.getString());
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }
        },
        rm_format {
            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).rssManager.removeFormat(pack.getInt());
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }
        },
        file {
            @Override
            public String exec(ExecutePack pack) {
                pack.context.startActivity(Tuils.openFile(pack.context, new File(Tuils.getFolder(), RssManager.PATH)));
                return null;
            }

            @Override
            public int[] args() {
                return new int[0];
            }
        };

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

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
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.context.getString(R.string.help_rss);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.context.getString(R.string.invalid_integer);
        }
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
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_rss;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }
}
