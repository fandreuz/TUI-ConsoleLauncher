package ohi.andre.consolelauncher.commands.raw;

import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by andre on 07/08/15.
 */

public class tracks implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        List<String> names = info.player.getNames();
        Tuils.addPrefix(names, "  ");
        Tuils.insertHeaders(names, false);
        return Tuils.toPlanString(names);
    }

    @Override
    public int helpRes() {
        return R.string.help_tracks;
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
