package ohi.andre.consolelauncher.commands.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

/**
 * Created by andre on 15/11/15.
 */
public class alias implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        if (info.aliasManager.getNum() > 0)
            return info.aliasManager.printAliases();
        else
            return info.res.getString(helpRes());
    }

    @Override
    public int helpRes() {
        return R.string.help_aliases;
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
        return 2;
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
