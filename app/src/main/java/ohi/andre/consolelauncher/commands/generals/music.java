package ohi.andre.consolelauncher.commands.generals;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public abstract class music implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        if (!info.player.initPlayer())
            return info.res.getString(R.string.output_musicfoldererror);

        return null;
    }

}
