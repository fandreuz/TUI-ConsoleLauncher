package ohi.andre.consolelauncher.commands.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.commands.generals.music;

public class previous extends music {

    @Override
    public String exec(ExecInfo info) {
        String output = super.exec(info);
        if (output != null)
            return output;

        output = info.player.prev();
        return info.res.getString(R.string.output_playing) + " " + output;
    }

    @Override
    public int helpRes() {
        return R.string.help_previous;
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
