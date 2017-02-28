package ohi.andre.consolelauncher.commands.main.generals;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;

public abstract class music implements CommandAbstraction {

    @Override
    public String exec(ExecutePack info) {
        MainPack pack = (MainPack) info;
        if (!pack.player.initPlayer())
            return pack.res.getString(R.string.output_musicfoldererror);

        return null;
    }

}
