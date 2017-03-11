package ohi.andre.consolelauncher.commands.main.raw;

import android.app.Activity;
import android.content.SharedPreferences;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 04/03/2017.
 */

public class unhideapp implements CommandAbstraction {
    @Override
    public String exec(ExecutePack pack) throws Exception {
        String app = pack.get(String.class, 0);
        unHideApp((MainPack) pack, app);
        return null;
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
    public int[] argType() {
        return new int[] {CommandAbstraction.HIDDEN_PACKAGE};
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public int helpRes() {
        return R.string.help_appsunhide;
    }

    @Override
    public String onArgNotFound(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        return info.res.getString(R.string.output_appnotfound);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.res.getString(helpRes());
    }

    @Override
    public String[] parameters() {
        return null;
    }

    private String unHideApp(MainPack info, String app) {
        SharedPreferences.Editor editor = ((Activity) info.context).getPreferences(0).edit();
        String result = info.appsManager.unhideApp(app);
        if (result != null) {
            editor.commit();
            return result + Tuils.SPACE + info.res.getString(R.string.output_unhideapp);
        } else
            return info.res.getString(R.string.output_appnotfound);
    }
}
