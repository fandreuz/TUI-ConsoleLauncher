package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import android.net.Uri;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 10/06/2017.
 */

public class tui extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        rm {
            @Override
            public String exec(ExecutePack pack) {
                MainPack info = (MainPack) pack;
                info.policy.removeActiveAdmin(info.component);

                Uri packageURI = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                info.context.startActivity(uninstallIntent);

                return Tuils.EMPTYSTRING;
            }
        },
        about {
            @Override
            public String exec(ExecutePack pack) {
                MainPack info = (MainPack) pack;
                return info.res.getString(R.string.version_label) + Tuils.SPACE + BuildConfig.VERSION_NAME + Tuils.NEWLINE + Tuils.NEWLINE + info.res.getString(R.string.output_about);
            }
        },
        reset {
            @Override
            public String exec(ExecutePack pack) {
                FileManager.rm(Tuils.getFolder(), false);
                return null;
            }
        };

        @Override
        public int[] args() {
            return new int[0];
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

            for(int count = 0; count < ps.length; count++) {
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
        return 1;
    }

    @Override
    public int maxArgs() {
        return 1;
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_tui;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return ((MainPack) pack).context.getString(helpRes());
    }
}
