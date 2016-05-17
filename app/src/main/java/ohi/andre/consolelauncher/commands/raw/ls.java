package ohi.andre.consolelauncher.commands.raw;

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

import java.io.File;
import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.tuils.ShellUtils;
import ohi.andre.consolelauncher.tuils.Tuils;

public class ls implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) throws Exception {
        File file = info.get(File.class, 0);

        if (info.getSu()) {
            ShellUtils.CommandResult result = ShellUtils.execCommand("ls " + file.getAbsolutePath(), true, info.currentDirectory.getAbsolutePath());
            return result.toString();
        } else {
            List<File> files = FileManager.lsFile(file, true);
            return Tuils.filesToPlanString(files, Tuils.NEWLINE);
        }
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
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_ls;
    }

    @Override
    public int notFoundRes() {
        return R.string.output_filenotfound;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        info.set(new File[]{info.currentDirectory});
        try {
            return exec(info);
        } catch (Exception e) {
            return e.toString();
        }
    }

    @Override
    public String[] parameters() {
        return null;
    }
}
