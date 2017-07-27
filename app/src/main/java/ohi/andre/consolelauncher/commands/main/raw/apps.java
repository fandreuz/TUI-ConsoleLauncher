package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class apps extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        lsh {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).appsManager.printApps(AppsManager.HIDDEN_APPS);
            }
        },
        show {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.HIDDEN_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                AppsManager.LaunchInfo i = pack.get(AppsManager.LaunchInfo.class, 1);
                ((MainPack) pack).appsManager.showActivity(i);
                return null;
            }
        },
        hide {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                AppsManager.LaunchInfo i = pack.get(AppsManager.LaunchInfo.class, 1);
                ((MainPack) pack).appsManager.hideActivity(i);
                return null;
            }
        },
        l {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                try {
                    AppsManager.LaunchInfo i = pack.get(AppsManager.LaunchInfo.class, 1);

                    PackageInfo info = pack.context.getPackageManager().getPackageInfo(i.componentName.getPackageName(), PackageManager.GET_PERMISSIONS | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS);
                    return AppsManager.AppUtils.format(info);
                } catch (PackageManager.NameNotFoundException e) {
                    return e.toString();
                }
            }
        },
        ps {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                openPlaystore(pack.context, pack.get(AppsManager.LaunchInfo.class, 1).componentName.getPackageName());
                return null;
            }
        },
        default_app {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.DEFAULT_APP};
            }

            @Override
            public String exec(ExecutePack pack) {
                int index = pack.get(int.class, 1);

                Object o = pack.get(Object.class, 2);

                String marker;
                if(o instanceof AppsManager.LaunchInfo) {
                    AppsManager.LaunchInfo i = (AppsManager.LaunchInfo) o;
                    marker = i.componentName.getPackageName() + "-" + i.componentName.getClassName();
                } else {
                    marker = (String) o;
                }

                try {
                    XMLPrefsManager.XMLPrefsSave save = AppsManager.Options.valueOf("default_app_n" + index);
                    save.parent().write(save, marker);
                    return null;
                } catch (Exception e) {
                    return pack.context.getString(R.string.invalid_integer);
                }
            }
        },
        st {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                openSettings(pack.context, pack.get(AppsManager.LaunchInfo.class, 1).componentName.getPackageName());
                return null;
            }
        },
        frc {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent intent = ((MainPack) pack).appsManager.getIntent(pack.get(AppsManager.LaunchInfo.class, 1));
                pack.context.startActivity(intent);

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
                pack.context.startActivity(Tuils.openFile(new File(Tuils.getFolder(), AppsManager.PATH)));
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

    private static void openSettings(Context context, String packageName) {
        Tuils.openSettingsPage(context, packageName);
    }

    private static void openPlaystore(Context context, String packageName) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    @Override
    public int helpRes() {
        return R.string.help_apps;
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
        return 4;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        MainPack pack = (MainPack) info;
        if (nArgs > 0) return pack.res.getString(helpRes());
        return pack.appsManager.printApps(AppsManager.SHOWN_APPS);
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        MainPack pack = (MainPack) info;
        return pack.res.getString(R.string.output_appnotfound);
    }

    @Override
    public String[] params() {
        return Param.labels();
    }
}
