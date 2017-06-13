package ohi.andre.consolelauncher.commands.main.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.Param;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

public class notifications extends ParamCommand {

    private final String INCLUDE_PARAM = "-inc";
    private final String EXCLUDE_PARAM = "-exc";
    private final String COLOR_PARAM = "-clr";

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        inc {
            @Override
            public String exec(ExecutePack pack) {
                NotificationManager.notificationsChangeFor(new NotificationManager.NotificatedApp(pack.get(String.class, 1), -1, true));
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }
        },
        exc{
            @Override
            public String exec(ExecutePack pack) {
                NotificationManager.notificationsChangeFor(new NotificationManager.NotificatedApp(pack.get(String.class, 1), -1, false));
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }
        },
        clr{
            @Override
            public String exec(ExecutePack pack) {
                if(pack.args.length < 3) return ((MainPack) pack).context.getString(R.string.help_notifications);
                try {
                    NotificationManager.notificationsChangeFor(new NotificationManager.NotificatedApp(pack.get(String.class, 2), pack.get(String.class, 1), true));
                } catch (Exception e) {}
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.COLOR, CommandAbstraction.VISIBLE_PACKAGE};
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
    }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(String param) {
        return Param.get(param);
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    public int minArgs() {
        return 2;
    }

    @Override
    public int maxArgs() {
        return 3;
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_notifications;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return ((MainPack) pack).context.getString(R.string.output_appnotfound);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return ((MainPack) pack).context.getString(helpRes());
    }
}
