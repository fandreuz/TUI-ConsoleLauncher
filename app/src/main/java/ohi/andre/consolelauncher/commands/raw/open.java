package ohi.andre.consolelauncher.commands.raw;

import java.io.File;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.managers.FileManager;

public class open implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        File file = info.get(File.class, 0);

        int result = FileManager.openFile(info.context, file);

        if (result == FileManager.ISDIRECTORY)
            return info.res.getString(R.string.output_isdirectory);
        if (result == FileManager.IOERROR)
            return info.res.getString(R.string.output_ioerror);

        return info.res.getString(R.string.output_opening) + " " + file.getName();
    }

    @Override
    public int helpRes() {
        return R.string.help_open;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public int maxArgs() {
        return 1;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.FILE};
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
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return info.res.getString(helpRes());
    }

    @Override
    public int notFoundRes() {
        return R.string.output_filenotfound;
    }

}
