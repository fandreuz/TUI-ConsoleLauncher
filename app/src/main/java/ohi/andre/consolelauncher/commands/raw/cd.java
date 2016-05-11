package ohi.andre.consolelauncher.commands.raw;

import java.io.File;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class cd implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        File folder = info.get(File.class, 0);
        if (!folder.isDirectory()) {
            if (!folder.isFile())
                return info.res.getString(R.string.output_filenotfound);
            return info.res.getString(R.string.output_isfile);
        }

        info.currentDirectory = folder;

        return info.res.getString(R.string.output_operationdone);
    }

    @Override
    public int helpRes() {
        return R.string.help_cd;
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
