package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import java.io.File;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

public class notifications extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        inc {
            @Override
            public String exec(ExecutePack pack) {
                NotificationManager.notificationsChangeFor(new NotificationManager.NotificatedApp(pack.get(AppsManager.LaunchInfo.class, 1).componentName.getPackageName(), null, true));
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }
        },
        exc {
            @Override
            public String exec(ExecutePack pack) {
                NotificationManager.notificationsChangeFor(new NotificationManager.NotificatedApp(pack.get(AppsManager.LaunchInfo.class, 1).componentName.getPackageName(), null, false));
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }
        },
        clr {
            @Override
            public String exec(ExecutePack pack) {
                if(pack.args.length < 3) return ((MainPack) pack).context.getString(R.string.help_notifications);
                try {
                    NotificationManager.notificationsChangeFor(new NotificationManager.NotificatedApp(pack.get(AppsManager.LaunchInfo.class, 2).componentName.getPackageName(), pack.get(String.class, 1), true));
                } catch (Exception e) {}
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.COLOR, CommandAbstraction.VISIBLE_PACKAGE};
            }
        },
        title_filter {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.get(int.class, 1);
                NotificationManager.excludeRegex(pack.get(String.class, 2), "title", id);
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }
        },
        text_filter {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.get(int.class, 1);
                NotificationManager.excludeRegex(pack.get(String.class, 2), "text", id);
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }
        },
        apply_filter {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.get(int.class, 1);
                NotificationManager.applyFilter(id, pack.get(AppsManager.LaunchInfo.class, 2).componentName.getPackageName());
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
                pack.context.startActivity(Tuils.openFile(new File(Tuils.getFolder(), NotificationManager.PATH)));
                return null;
            }
        },
        access {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                try {
                    pack.context.startActivity(new Intent(Build.VERSION.SDK_INT >= 22 ? Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS : "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                } catch (Exception e) {
                    return pack.context.getString(R.string.activity_not_found);
                }
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
    public String[] params() {
        return Param.labels();
    }

    @Override
    public int minArgs() {
        return 1;
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
        String arg = pack.get(String.class, 0).toLowerCase();
        if(index == 1 && (arg.equals(Param.title_filter.label()) || arg.equals(Param.text_filter.label()) || arg.equals(Param.apply_filter.label()))) {
            return ((MainPack) pack).context.getString(R.string.invalid_integer);
        }
        return ((MainPack) pack).context.getString(R.string.output_appnotfound);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return ((MainPack) pack).context.getString(helpRes());
    }
}
