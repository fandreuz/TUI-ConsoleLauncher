package ohi.andre.consolelauncher.commands.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class about implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        return info.res.getString(R.string.output_about);
    }

    @Override
    public int helpRes() {
        return R.string.help_about;
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
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

    @Override
    public int priority() {
        return 1;
    }
}
