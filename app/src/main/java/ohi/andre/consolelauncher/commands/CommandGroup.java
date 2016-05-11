package ohi.andre.consolelauncher.commands;

import android.content.Context;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import ohi.andre.consolelauncher.tuils.Tuils;

public class CommandGroup {

    private String packageName;

    private List<String> commands;

    public CommandGroup(Context c, String packageName) {
        this.packageName = packageName;

        try {
            this.commands = Tuils.getClassesOfPackage(packageName, c);
        } catch (IOException e) {
        }

        Collections.sort(commands);
    }

    public CommandAbstraction getCommandByName(String name) throws Exception {
        for (int count = 0; count < commands.size(); count++) {
            String cmdName = commands.get(count);
            if (!cmdName.equals(name))
                continue;

            String fullCmdName = packageName + "." + cmdName;
            return Tuils.getCommandInstance(fullCmdName);
        }

        return null;
    }

    public List<String> getCommands() {
        return commands;
    }

}
