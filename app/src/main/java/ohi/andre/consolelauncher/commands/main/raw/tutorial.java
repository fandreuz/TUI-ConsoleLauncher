package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 10/07/2017.
 */

public class tutorial implements CommandAbstraction {

    final String url = "https://github.com/Andre1299/TUI-ConsoleLauncher/wiki";

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Intent intent = Tuils.webPage(url);
        if(intent != null) pack.context.startActivity(intent);
        return null;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_tutorial;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }
}
