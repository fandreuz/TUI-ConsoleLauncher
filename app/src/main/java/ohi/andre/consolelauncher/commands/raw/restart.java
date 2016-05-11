package ohi.andre.consolelauncher.commands.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class restart implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) throws Exception {
        info.reloadable.reload();
        return "";
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
        return 4;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public int helpRes() {
        return R.string.help_restart;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

}
