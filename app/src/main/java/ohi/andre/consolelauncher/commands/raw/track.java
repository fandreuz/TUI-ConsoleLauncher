package ohi.andre.consolelauncher.commands.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.commands.generals.music;

public class track extends music {

    @Override
    public String exec(ExecInfo info) {
        if (info.player.isPlaying())
            return info.player.trackInfo();
        else
            return info.res.getString(R.string.output_nomusic);
    }

    @Override
    public int helpRes() {
        return R.string.help_track;
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
