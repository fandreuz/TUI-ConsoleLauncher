package ohi.andre.consolelauncher.tuils.libsuperuser;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.BusyBoxInstaller;
import ohi.andre.consolelauncher.tuils.Tuils;

public class ShellHolder {

    private Context context;

    public ShellHolder(Context context) {
        this.context = context;
    }

    Pattern p = Pattern.compile("^\\n");

    public Shell.Interactive build() {
        Shell.Interactive interactive = new Shell.Builder()
                .setOnSTDOUTLineListener(line -> {
                    line = p.matcher(line).replaceAll(Tuils.EMPTYSTRING);
                    Tuils.sendOutput(context, line, TerminalManager.CATEGORY_OUTPUT);
                })
                .setOnSTDERRLineListener(line -> {
                    line = p.matcher(line).replaceAll(Tuils.EMPTYSTRING);
                    Tuils.sendOutput(context, line, TerminalManager.CATEGORY_OUTPUT);
                })
                .open();
        
        setupBusyBox(interactive);
        interactive.addCommand("cd " + XMLPrefsManager.get(File.class, Behavior.home_path));
        return interactive;
    }

    private void setupBusyBox(Shell.Interactive interactive) {
        if (BusyBoxInstaller.isInstalled(context)) {
            String bbPath = BusyBoxInstaller.getBusyboxPath(context);
            if (bbPath != null) {
                // 1. Alias 'busybox' to the downloaded path
                interactive.addCommand("alias busybox='" + bbPath + "'");

                // 2. Add common applet aliases so they use BusyBox instead of Toybox
                String[] commonApplets = {
                    "ls", "grep", "egrep", "fgrep", "sed", "awk", "find", "xargs",
                    "vi", "vim", "less", "more", "cat", "tail", "head", "cut", "sort",
                    "top", "ps", "kill", "pkill", "pgrep", "free", "uptime", "watch",
                    "ping", "ping6", "traceroute", "netstat", "ip", "ifconfig", "route",
                    "tar", "gzip", "gunzip", "bzip2", "bunzip2", "xz", "unxz", "zip", "unzip",
                    "wget", "curl", "nc", "telnet", "ftpget", "ftpput",
                    "ssh", "scp", "chmod", "chown", "chgrp", "cp", "mv", "rm", "mkdir", "rmdir"
                };

                for (String applet : commonApplets) {
                    interactive.addCommand("alias " + applet + "='" + bbPath + " " + applet + "'");
                }
            }
        }
    }
}
