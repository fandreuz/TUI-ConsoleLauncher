package ohi.andre.consolelauncher.commands.raw;

import android.app.Activity;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.PreferencesManager;

public class refresh implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        info.appsManager.fill(((Activity) info.context).getPreferences(0));

        info.preferencesManager.refresh(PreferencesManager.ALIAS);
        info.aliasManager = new AliasManager(info.preferencesManager);

        return info.res.getString(R.string.output_refresh);
    }

    @Override
    public int helpRes() {
        return R.string.help_refresh;
    }

    @Override
    public int minArgs() {
        return 0;
    }

    @Override
    public int maxArgs() {
        return 0;
    }

    @Override
    public int[] argType() {
        return null;
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }
}
