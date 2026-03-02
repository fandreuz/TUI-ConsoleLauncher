package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.HTMLExtractManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 29/03/2018.
 */

public class htmlextract extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        query {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(HTMLExtractManager.ACTION_QUERY);

                i.putExtra(HTMLExtractManager.ID, pack.getInt());
                i.putExtra(HTMLExtractManager.FORMAT_ID, pack.getInt());
                i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, pack.getString());
                i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(i);

                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                if(index == 1) return pack.context.getString(R.string.invalid_integer);

                if(index == 2) {
//                    the user was trying to use the default format

//                    waste the first
                    pack.get();

                    Intent i = new Intent(HTMLExtractManager.ACTION_QUERY);
                    i.putExtra(HTMLExtractManager.ID, pack.getInt());
                    i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, pack.getString());
                    i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
                    LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(i);

                    return null;
                }

                return pack.context.getString(R.string.help_htmlextract);
            }
        },
        add {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.DATASTORE_PATH_TYPE, CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(HTMLExtractManager.ACTION_ADD);
                i.putExtra(HTMLExtractManager.TAG_NAME, pack.getString());
                i.putExtra(HTMLExtractManager.ID, pack.getInt());
                i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, pack.getString());
                i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(i);

                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                if(index == 1) return pack.context.getString(R.string.invalid_datastoretype);
                return super.onArgNotFound(pack, index);
            }
        },
        rm {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(HTMLExtractManager.ACTION_RM);
                i.putExtra(HTMLExtractManager.ID, pack.getInt());
                i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(i);

                return null;
            }
        },
        edit {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(HTMLExtractManager.ACTION_EDIT);
                i.putExtra(HTMLExtractManager.ID, pack.getInt());
                i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
                i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, pack.getString());
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(i);

                return null;
            }
        },
        ls {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.DATASTORE_PATH_TYPE};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(HTMLExtractManager.ACTION_LS);
                i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
                i.putExtra(HTMLExtractManager.TAG_NAME, pack.getString());
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(i);

                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                Intent i = new Intent(HTMLExtractManager.ACTION_LS);
                i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(i);

                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.invalid_datastoretype);
            }
        },
        tutorial {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                pack.context.startActivity(Tuils.webPage("https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/HTMLExtract"));
                return null;
            }
        },
        file {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = new File(Tuils.getFolder(), HTMLExtractManager.PATH);
                pack.context.startActivity(Tuils.openFile(pack.context, file));
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
            return pack.context.getString(R.string.help_htmlextract);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.context.getString(R.string.invalid_integer);
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
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_htmlextract;
    }
}
