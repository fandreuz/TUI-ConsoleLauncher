package ohi.andre.consolelauncher.commands.main.raw;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.FileManager;

/**
 * Created by andre on 03/12/15.
 */
public class rm implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        ArrayList<File> args = info.get(ArrayList.class, 0);

        File[] files = new File[args.size()];
        args.toArray(files);

        int result = FileManager.rm(files, info.getSu());
        switch (result) {
            case FileManager.ISFILE:
                return info.res.getString(R.string.output_isfile);
            case FileManager.IOERROR:
                return info.res.getString(R.string.output_error);
            case FileManager.NOT_WRITEABLE:
                return info.res.getString(R.string.output_nowriteable);
        }
        return null;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public int maxArgs() {
        return CommandAbstraction.UNDEFINIED;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.FILE_LIST};
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_rm;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        MainPack info = (MainPack) pack;
        return info.res.getString(R.string.output_filenotfound);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.res.getString(R.string.output_filenotfound);
    }
}
