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
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Apps;
import ohi.andre.consolelauncher.tuils.Tuils;

public class apps extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        ls {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).appsManager.printApps(AppsManager.SHOWN_APPS, pack.getString());
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return ((MainPack) pack).appsManager.printApps(AppsManager.SHOWN_APPS);
            }
        },
        lsh {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return ((MainPack) pack).appsManager.printApps(AppsManager.HIDDEN_APPS, pack.getString());
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
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
                AppsManager.LaunchInfo i = pack.getLaunchInfo();
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
                AppsManager.LaunchInfo i = pack.getLaunchInfo();
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
                    AppsManager.LaunchInfo i = pack.getLaunchInfo();

                    PackageInfo info = pack.context.getPackageManager().getPackageInfo(i.componentName.getPackageName(), PackageManager.GET_PERMISSIONS | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS);
                    return AppsManager.AppUtils.format(i, info);
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
                openPlaystore(pack.context, pack.getLaunchInfo().componentName.getPackageName());
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
                int index = pack.getInt();

                Object o = pack.get();

                String marker;
                if(o instanceof AppsManager.LaunchInfo) {
                    AppsManager.LaunchInfo i = (AppsManager.LaunchInfo) o;
                    marker = i.componentName.getPackageName() + "-" + i.componentName.getClassName();
                } else {
                    marker = (String) o;
                }

                try {
                    XMLPrefsSave save = Apps.valueOf("default_app_n" + index);
                    save.parent().write(save, marker);
                    return null;
                } catch (Exception e) {
                    return pack.context.getString(R.string.invalid_integer);
                }
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                int res;
                if(index == 1) res = R.string.invalid_integer;
                else res = R.string.output_appnotfound;

                return pack.context.getString(res);
            }
        },
        st {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                openSettings(pack.context, pack.getLaunchInfo().componentName.getPackageName());
                return null;
            }
        },
        frc {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.ALL_PACKAGES};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent intent = ((MainPack) pack).appsManager.getIntent(pack.getLaunchInfo());
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
                pack.context.startActivity(Tuils.openFile(pack.context, new File(Tuils.getFolder(), AppsManager.PATH)));
                return null;
            }
        },
//        services {
//            @Override
//            public int[] args() {
//                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
//            }
//
//            @Override
//            public String exec(ExecutePack pack) {
//                AppsManager.LaunchInfo info = pack.get(AppsManager.LaunchInfo.class, 1);
//
//                List<String> services = new ArrayList<>();
//
//                ActivityManager activityManager = (ActivityManager) pack.context.getSystemService(Context.ACTIVITY_SERVICE);
//                for(ActivityManager.RunningServiceInfo i : activityManager.getRunningServices(Integer.MAX_VALUE)) {
//                    ComponentName name = i.service;
//
//                    if(info.equals(name.getPackageName())) {
//                        services.add(name.getClassName().replace(name.getPackageName(), Tuils.EMPTYSTRING));
//                    }
//                }
//
//                if(services.size() == 0) return "[]";
//                Collections.sort(services);
//                return Tuils.toPlanString(services, Tuils.NEWLINE);
//            }
//
//            @Override
//            public String onNotArgEnough(ExecutePack pack, int n) {
//
//                List<SimpleMutableEntry<String, ArrayList<String>>> services = new ArrayList<>();
//
//                ActivityManager activityManager = (ActivityManager) pack.context.getSystemService(Context.ACTIVITY_SERVICE);
//                Tuils.log(activityManager.getRunningServices(Integer.MAX_VALUE).toString());
//                for(ActivityManager.RunningServiceInfo i : activityManager.getRunningServices(Integer.MAX_VALUE)) {
//
//                    boolean check = false;
//                    for(SimpleMutableEntry<String, ArrayList<String>> s : services) {
//                        if(s.getKey().equals(i.service.getPackageName())) {
//                            s.getValue().add(i.service.getClassName().replace(i.service.getPackageName(), Tuils.EMPTYSTRING));
//
//                            check = true;
//                            break;
//                        }
//                    }
//
//                    if(!check) {
//                        SimpleMutableEntry<String,ArrayList<String>> s = new SimpleMutableEntry<>(i.service.getPackageName(), new ArrayList<String>());
//                        s.getValue().add(i.service.getClassName().replace(i.service.getPackageName(), Tuils.EMPTYSTRING));
//                        services.add(s);
//                    }
//                }
//
//                if(services.size() == 0) return "[]";
//                Collections.sort(services, new Comparator<SimpleMutableEntry<String, ArrayList<String>>>() {
//                    @Override
//                    public int compare(SimpleMutableEntry<String, ArrayList<String>> o1, SimpleMutableEntry<String, ArrayList<String>> o2) {
//                        return o1.getKey().compareTo(o2.getKey());
//                    }
//                });
//
//                PackageManager manager = pack.context.getPackageManager();
//                StringBuilder b = new StringBuilder();
//                for(SimpleMutableEntry<String, ArrayList<String>> s : services) {
//                    String appName = null;
//                    try {
//                        appName = manager.getApplicationInfo(s.getKey(), 0).loadLabel(manager).toString();
//                    } catch (PackageManager.NameNotFoundException e) {}
//
//                    if(appName != null) b.append(appName).append(Tuils.SPACE).append("(").append(s.getKey()).append(")");
//                    else b.append(s.getKey());
//                    b.append(Tuils.NEWLINE);
//
//                    for(String st : s.getValue()) {
//                        b.append(" - ").append(st).append(Tuils.NEWLINE);
//                    }
//                    b.append(Tuils.NEWLINE);
//                }
//                return b.toString().trim();
//            }
//        },
        reset {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                AppsManager.LaunchInfo app = pack.getLaunchInfo();
                app.launchedTimes = 0;
                ((MainPack) pack).appsManager.writeLaunchTimes(app);

                return null;
            }
        },
        mkgp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.NO_SPACE_STRING};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                return ((MainPack) pack).appsManager.createGroup(name);
            }
        },
        rmgp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                return ((MainPack) pack).appsManager.removeGroup(name);
            }
        },
        gp_bg_color {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP, CommandAbstraction.COLOR};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                String color = pack.getString();
                return ((MainPack) pack).appsManager.groupBgColor(name, color);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                if(n == 2) {
                    String name = pack.getString();
                    return ((MainPack) pack).appsManager.groupBgColor(name, Tuils.EMPTYSTRING);
                }
                return super.onNotArgEnough(pack, n);
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_invalidcolor);
            }
        },
        gp_fore_color {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP, CommandAbstraction.COLOR};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                String color = pack.getString();
                return ((MainPack) pack).appsManager.groupForeColor(name, color);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                if(n == 2) {
                    String name = pack.getString();
                    return ((MainPack) pack).appsManager.groupForeColor(name, Tuils.EMPTYSTRING);
                }
                return super.onNotArgEnough(pack, n);
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_invalidcolor);
            }
        },
        lsgp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                return ((MainPack) pack).appsManager.listGroup(name);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return ((MainPack) pack).appsManager.listGroups();
            }
        },
        addtogp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP, CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                AppsManager.LaunchInfo app = pack.getLaunchInfo();
                return ((MainPack) pack).appsManager.addAppToGroup(name, app);
            }
        },
        rmfromgp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP, CommandAbstraction.APP_INSIDE_GROUP};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                AppsManager.LaunchInfo app = pack.getLaunchInfo();
                return ((MainPack) pack).appsManager.removeAppFromGroup(name, app);
            }
        },
        tutorial {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                pack.context.startActivity(Tuils.webPage("https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/Apps"));
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
            return pack.context.getString(R.string.help_apps);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.context.getString(R.string.output_appnotfound);
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
    public int priority() {
        return 4;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }
}
