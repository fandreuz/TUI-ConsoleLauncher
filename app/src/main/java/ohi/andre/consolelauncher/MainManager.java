package ohi.andre.consolelauncher;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandGroup;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.RedirectCommand;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.music.MusicManager2;
import ohi.andre.consolelauncher.tuils.Compare;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.TimeManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Hintable;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Redirectator;
import ohi.andre.consolelauncher.tuils.interfaces.Rooter;
import ohi.andre.consolelauncher.tuils.interfaces.Suggester;
import ohi.andre.consolelauncher.tuils.libsuperuser.Shell;
import ohi.andre.consolelauncher.tuils.libsuperuser.ShellHolder;

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

public class MainManager {

    private RedirectCommand redirect;
    private Redirectator redirectator = new Redirectator() {
        @Override
        public void prepareRedirection(RedirectCommand cmd) {
            redirect = cmd;

            if(redirectionListener != null) {
                redirectionListener.onRedirectionRequest(cmd);
            }
        }

        @Override
        public void cleanup() {
            if(redirect != null) {
                redirect.beforeObjects.clear();
                redirect.afterObjects.clear();

                if(redirectionListener != null) {
                    redirectionListener.onRedirectionEnd(redirect);
                }

                redirect = null;
            }
        }
    };
    private OnRedirectionListener redirectionListener;
    public void setRedirectionListener(OnRedirectionListener redirectionListener) {
        this.redirectionListener = redirectionListener;
    }

    private final String COMMANDS_PKG = "ohi.andre.consolelauncher.commands.main.raw";

    private CmdTrigger[] triggers = new CmdTrigger[] {
            new GroupTrigger(),
            new AliasTrigger(),
            new TuiCommandTrigger(),
            new AppTrigger(),
            new SystemCommandTrigger()
    };
    private MainPack mainPack;

    private Context mContext;

    private Inputable in;
    private Outputable out;

    private boolean showAliasValue;
    private boolean showAppHistory;
    private int aliasContentColor;

    private String multipleCmdSeparator;

    public static Shell.Interactive interactive;

    private Hintable hintable;

    protected MainManager(LauncherActivity c, Inputable i, Outputable o, Suggester sugg, CommandExecuter executer) {
        mContext = c;

        in = i;
        out = o;

        showAliasValue = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.show_alias_content);
        showAppHistory = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.show_launch_history);
        aliasContentColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.alias_content_color);

        multipleCmdSeparator = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.multiple_cmd_separator);

        CommandGroup group = new CommandGroup(mContext, COMMANDS_PKG);

        ContactManager cont = null;
        try {
            cont = new ContactManager(mContext);
        } catch (NullPointerException e) {}

        MusicManager2 music = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.enable_music) ? new MusicManager2(mContext) : null;

        AppsManager appsMgr = new AppsManager(c, sugg);
        AliasManager aliasManager = new AliasManager(mContext);

        ShellHolder shellHolder = new ShellHolder(out);
        interactive = shellHolder.build();

        mainPack = new MainPack(mContext, group, aliasManager, appsMgr, music, cont, c, executer, redirectator, shellHolder);
    }

//    command manager
    public void onCommand(String input, String alias) {

        input = Tuils.removeUnncesarySpaces(input);

        if(redirect != null) {
            if(!redirect.isWaitingPermission()) {
                redirect.afterObjects.add(input);
            }
            String output = redirect.onRedirect(mainPack);
            out.onOutput(output);

            return;
        }

        if(alias != null && showAliasValue) {
            out.onOutput(aliasContentColor, mainPack.aliasManager.formatLabel(alias, input));
        }

        String[] cmds;
        if(multipleCmdSeparator.length() > 0) {
            cmds = input.split(multipleCmdSeparator);
        } else {
            cmds = new String[] {input};
        }

        for(String cmd : cmds) {
            for (CmdTrigger trigger : triggers) {
                boolean r;
                try {
                    r = trigger.trigger(mainPack, cmd);
                } catch (Exception e) {
                    out.onOutput(Tuils.getStackTrace(e));
                    break;
                }
                if (r) {
                    break;
                }
            }
        }
    }

    public void onLongBack() {
        in.in(Tuils.EMPTYSTRING);
    }

    public void sendPermissionNotGrantedWarning() {
        redirectator.cleanup();
    }

    public void dispose() {
        mainPack.dispose();
    }

    public void destroy() {
        mainPack.destroy();

        new Thread() {
            @Override
            public void run() {
                super.run();

                interactive.kill();
                interactive.close();
            }
        }.start();
    }

    public MainPack getMainPack() {
        return mainPack;
    }

    public void setHintable(Hintable hintable) {
        this.hintable = hintable;
    }

    public void setRooter(Rooter rooter) {
        this.mainPack.rooter = rooter;
    }

    interface CmdTrigger {
        boolean trigger(ExecutePack info, String input) throws Exception;
    }

    private class AliasTrigger implements CmdTrigger {

        @Override
        public boolean trigger(ExecutePack info, String input) {
            String alias[] = mainPack.aliasManager.getAlias(input, true);

            String aliasValue = alias[0];
            if (alias[0] == null) {
                return false;
            }

            String aliasName = alias[1];
            String residual = alias[2];

            aliasValue = mainPack.aliasManager.format(aliasValue, residual);

            mainPack.executer.exec(aliasValue, aliasName);

            return true;
        }
    }

    private class GroupTrigger implements CmdTrigger {

        @Override
        public boolean trigger(ExecutePack info, String input) throws Exception {
            int index = input.indexOf(Tuils.SPACE);
            String name;

            if(index != -1) {
                name = input.substring(0,index);
                input = input.substring(index + 1);
            } else {
                name = input;
                input = null;
            }

            List<? extends Group> appGroups = AppsManager.groups;
            if(appGroups != null) {
                for(Group g : appGroups) {
                    if(name.equals(g.name())) {
                        if(input == null) {
                            out.onOutput(AppsManager.AppUtils.printApps(AppsManager.AppUtils.labelList((List<AppsManager.LaunchInfo>) g.members(), false)));
                            return true;
                        } else {
                            return g.use(mainPack, input);
                        }
                    }
                }
            }

            return false;
        }
    }

    private class SystemCommandTrigger implements CmdTrigger {

        final int CD_CODE = 10;
        final int PWD_CODE = 11;

        final Shell.OnCommandResultListener pwdResult = new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if(commandCode == PWD_CODE && output.size() == 1) {
                    File f = new File(output.get(0));
                    if(f.exists()) {
                        mainPack.currentDirectory = f;
                        if(hintable != null) hintable.updateHint();
                    }
                }
            }
        };

        final Shell.OnCommandResultListener cdResult = new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if(commandCode == CD_CODE) {
                    interactive.addCommand("pwd", PWD_CODE, pwdResult);
                }
            }
        };

        @Override
        public boolean trigger(final ExecutePack info, final String input) throws Exception {

            new Thread() {
                @Override
                public void run() {
                    if(input.trim().equalsIgnoreCase("su")) {
                        if(Shell.SU.available() && mainPack.rooter != null) mainPack.rooter.onRoot();
                        interactive.addCommand("su");

                    } else if(input.contains("cd ")) {
                        interactive.addCommand(input, CD_CODE, cdResult);
                    } else {
                        interactive.addCommand(input);
                    }

                }
            }.start();

            return true;
        }
    }

    private class AppTrigger implements CmdTrigger {

        String appFormat;
        int timeColor;
        int outputColor;

        Pattern pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        Pattern pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        Pattern pl = Pattern.compile("%l", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        Pattern pn = Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

        @Override
        public boolean trigger(ExecutePack info, String input) {
            AppsManager.LaunchInfo i = mainPack.appsManager.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS);
            if (i == null) {
                return false;
            }

            Intent intent = mainPack.appsManager.getIntent(i);
            if (intent == null) {
                return false;
            }

            if(showAppHistory) {
                if(appFormat == null) {
                    appFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.app_launch_format);
                    timeColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.time_color);
                    outputColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.output_color);
                }

                String a = new String(appFormat);
                a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(intent.getComponent().getClassName()));
                a = pp.matcher(a).replaceAll(Matcher.quoteReplacement(intent.getComponent().getPackageName()));
                a = pl.matcher(a).replaceAll(Matcher.quoteReplacement(i.publicLabel));
                a = pn.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

                SpannableString text = new SpannableString(a);
                text.setSpan(new ForegroundColorSpan(outputColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                CharSequence s = TimeManager.replace(text, timeColor);

                out.onOutput(s, TerminalManager.CATEGORY_GENERAL);
            }

            mContext.startActivity(intent);
            return true;
        }
    }

    private class TuiCommandTrigger implements CmdTrigger {

        @Override
        public boolean trigger(final ExecutePack info, final String input) throws Exception {

            final Command command = CommandTuils.parse(input, info, false);
            if(command == null) return false;

            mainPack.lastCommand = input;

            new StoppableThread() {
                @Override
                public void run() {
                    super.run();

                    try {
                        String output = command.exec(mContext.getResources(), info);

                        if(output != null) {
                            out.onOutput(output);
                        }
                    } catch (Exception e) {
                        out.onOutput(Tuils.getStackTrace(e));
                        Tuils.log(e);
                    }
                }
            }.start();

            return true;
        }
    }

    public interface Group {
        List<? extends Compare.Stringable> members();
        boolean use(MainPack mainPack, String input);
        String name();
    }
}
