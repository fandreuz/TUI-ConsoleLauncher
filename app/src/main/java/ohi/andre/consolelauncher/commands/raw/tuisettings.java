package ohi.andre.consolelauncher.commands.raw;/*Copyright Francesco Andreuzzi

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

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.managers.PreferencesManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class tuisettings implements CommandAbstraction {

    @Override
    public String exec(final ExecInfo info) throws Exception {

        File tuiFolder = Tuils.getTuiFolder();
        final File settingsFile = new File(tuiFolder, PreferencesManager.SETTINGS_FILENAME);
        FileManager.openFile(info.context, settingsFile);

        return info.res.getString(R.string.output_opening) + " " + PreferencesManager.SETTINGS_FILENAME;
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
    public int helpRes() {
        return R.string.help_tuisettings;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

    @Override
    public String[] parameters() {
        return null;
    }
}
