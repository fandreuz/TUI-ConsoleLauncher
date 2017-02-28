package ohi.andre.consolelauncher.commands.main.raw;

import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by andre on 07/08/15.
 */

public class tracks implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        List<String> names = info.player.getNames();
        if(names == null) {
            return info.res.getString(R.string.output_musicfoldererror);
        }

        Tuils.addPrefix(names, Tuils.DOUBLE_SPACE);
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
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public String onArgNotFound(ExecutePack info) {
        return null;
    }
}
