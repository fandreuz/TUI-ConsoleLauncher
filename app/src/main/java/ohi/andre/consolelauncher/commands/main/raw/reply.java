package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import android.os.Build;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.APICommand;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.notifications.reply.ReplyManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 05/11/2017.
 */

public class reply extends ParamCommand implements APICommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        to {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.BOUND_REPLY_APP, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent intent = new Intent(ReplyManager.ACTION);

                intent.putExtra(ReplyManager.ID, pack.getString());
                intent.putExtra(ReplyManager.WHAT, pack.getString());

                LocalBroadcastManager.getInstance(pack.context).sendBroadcast(intent);
                return null;
            }
        },
        bind {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                String output = ReplyManager.bind(pack.getLaunchInfo().componentName.getPackageName());
                LocalBroadcastManager.getInstance(pack.context).sendBroadcast(new Intent(ReplyManager.ACTION_UPDATE));
                return output;
            }
        },
        check {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.BOUND_REPLY_APP};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent intent = new Intent(ReplyManager.ACTION);
                intent.putExtra(ReplyManager.ID, pack.getString());

                LocalBroadcastManager.getInstance(pack.context).sendBroadcast(intent);
                return null;
            }
        },
        unbind {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                String output = ReplyManager.unbind(pack.getLaunchInfo().componentName.getPackageName());
                LocalBroadcastManager.getInstance(pack.context).sendBroadcast(new Intent(ReplyManager.ACTION_UPDATE));

                if(output != null && output.length() == 0) return pack.context.getString(R.string.reply_app_not_found) + pack.getLaunchInfo().componentName.getPackageName();
                return output;
            }
        },
        ls {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent intent = new Intent(ReplyManager.ACTION_LS);
                LocalBroadcastManager.getInstance(pack.context).sendBroadcast(intent);
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
                pack.context.startActivity(Tuils.webPage("https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/Reply"));
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
            return pack.context.getString(R.string.help_reply);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.context.getString(R.string.help_reply);
        }
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    protected Param paramForString(MainPack pack, String param) {
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
        return R.string.help_reply;
    }

    @Override
    public boolean willWorkOn(int api) {
        return api >= Build.VERSION_CODES.KITKAT_WATCH;
    }
}
