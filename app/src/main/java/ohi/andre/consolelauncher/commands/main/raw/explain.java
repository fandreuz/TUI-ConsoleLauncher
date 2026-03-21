package ohi.andre.consolelauncher.commands.main.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.APICommand;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class explain implements APICommand, CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        StringBuilder sb = new StringBuilder();
        sb.append(Tuils.span("--- T-UI Launcher Features ---", ((MainPack)pack).commandColor)).append(Tuils.NEWLINE);
        sb.append(Tuils.NEWLINE);
        sb.append(Tuils.span("🚀 Modernization:", ((MainPack)pack).commandColor)).append(Tuils.NEWLINE);
        sb.append("This version is updated for Android 14 (API 34). Permissions like Location are requested only when needed.").append(Tuils.NEWLINE);
        sb.append(Tuils.NEWLINE);
        sb.append(Tuils.span("🛡️ Security:", ((MainPack)pack).commandColor)).append(Tuils.NEWLINE);
        sb.append("Your signing keys are private and stored in local.properties. Binaries are verified with SHA-256.").append(Tuils.NEWLINE);
        sb.append(Tuils.NEWLINE);
        sb.append(Tuils.span("🐧 BusyBox (bbman):", ((MainPack)pack).commandColor)).append(Tuils.NEWLINE);
        sb.append("Use 'bbman -install' to get a full Linux environment (ls, grep, vi, etc.).").append(Tuils.NEWLINE);
        sb.append(Tuils.NEWLINE);
        sb.append(Tuils.span("🎨 Themes:", ((MainPack)pack).commandColor)).append(Tuils.NEWLINE);
        sb.append("Use 'theme -preset [name]' for high-quality themes. Available: blue, red, green, pink, bw, cyberpunk.").append(Tuils.NEWLINE);
        sb.append(Tuils.NEWLINE);
        sb.append(Tuils.span("🌤️ Weather:", ((MainPack)pack).commandColor)).append(Tuils.NEWLINE);
        sb.append("Enable weather with 'weather -enable' or 'tuiweather -enable'. Use '-update' to refresh.").append(Tuils.NEWLINE);
        sb.append(Tuils.NEWLINE);
        sb.append(Tuils.span("👤 Customization:", ((MainPack)pack).commandColor)).append(Tuils.NEWLINE);
        sb.append("Use 'username [user] [device]' to change your terminal prompt instantly.").append(Tuils.NEWLINE);

        return sb.toString();
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return R.string.help_help; // Reuse help string
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }

    @Override
    public boolean willWorkOn(int api) {
        return true;
    }
}
