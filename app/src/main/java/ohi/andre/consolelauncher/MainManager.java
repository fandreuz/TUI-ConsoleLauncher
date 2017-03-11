package ohi.andre.consolelauncher;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;

import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandGroup;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.RedirectCommand;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.MusicManager;
import ohi.andre.consolelauncher.managers.PreferencesManager;
import ohi.andre.consolelauncher.tuils.ShellUtils;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Redirectator;

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

    private CmdTrigger[] triggers = new CmdTrigger[]{
            new AliasTrigger(),
            new TuiCommandTrigger(),
            new AppTrigger(),
//            keep this as last trigger
            new SystemCommandTrigger()
    };
    private MainPack mainPack;

    private Context mContext;

    private Inputable in;
    private Outputable out;

    private boolean showAliasValue;

    protected MainManager(LauncherActivity c, Inputable i, Outputable o, PreferencesManager prefsMgr, DevicePolicyManager devicePolicyManager, ComponentName componentName) {
        mContext = c;

        in = i;
        out = o;

        showAliasValue = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.SHOW_ALIAS_VALUE));

        CommandGroup group = new CommandGroup(mContext, COMMANDS_PKG);

        ContactManager cont = null;
        try {
            cont = new ContactManager(mContext);
        } catch (NullPointerException e) {}

        CommandExecuter executer = new CommandExecuter() {
            @Override
            public String exec(String input, String alias) {
                onCommand(input, alias);
                return null;
            }
        };

        MusicManager music = new MusicManager(mContext, prefsMgr, out);

        AppsManager appsMgr = new AppsManager(c, Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.COMPARESTRING_APPS)), out);
        AliasManager aliasManager = new AliasManager(prefsMgr);

        mainPack = new MainPack(mContext, prefsMgr, group, aliasManager, appsMgr, music, cont, devicePolicyManager, componentName, c, executer, out, redirectator);
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
            out.onOutput(alias + " --> " + "[" + input + "]");
        }

        for (CmdTrigger trigger : triggers) {
            boolean r;
            try {
                r = trigger.trigger(mainPack, input);
            } catch (Exception e) {
                out.onOutput(Tuils.getStackTrace(e));
                return;
            }
            if (r) {
                return;
            } else {}
        }
    }

    public void onLongBack() {
        in.in(Tuils.EMPTYSTRING);
    }

    public void sendPermissionNotGrantedWarning() {
        redirectator.cleanup();
    }

    //    dispose
    public void dispose() {
        mainPack.dispose();
    }

    public void destroy() {
        mainPack.destroy();
    }

    public MainPack getMainPack() {
        return mainPack;
    }

    interface CmdTrigger {
        boolean trigger(ExecutePack info, String input) throws Exception;
    }

    private class AliasTrigger implements CmdTrigger {
        @Override
        public boolean trigger(ExecutePack info, String alias) {
            String aliasValue = mainPack.aliasManager.getAlias(alias);
            if (aliasValue == null) {
                return false;
            }

            mainPack.executer.exec(aliasValue, alias);

            return true;
        }
    }

    //    this must be the last trigger
    private class SystemCommandTrigger implements CmdTrigger {

        final int COMMAND_NOTFOUND = 127;

        @Override
        public boolean trigger(final ExecutePack info, String input) throws Exception {

//            this is the last trigger, it has to say "command not found"

            boolean su = false;
            if (CommandTuils.isSuCommand(input)) {
                su = true;
                input = input.substring(3);
                if (input.startsWith(ShellUtils.COMMAND_SU_ADD + Tuils.SPACE)) {
                    input = input.substring(3);
                }
            }

            final String cmd = input;
            final boolean useSU = su;

            new Thread() {
                @Override
                public void run() {
                    super.run();

                    ShellUtils.CommandResult result = ShellUtils.execCommand(cmd, useSU, mainPack.currentDirectory.getAbsolutePath());
                    if (result.result == COMMAND_NOTFOUND) {
                        out.onOutput(mContext.getString(R.string.output_commandnotfound));
                    } else {
                        String output = result.toString();
                        if(output != null) {
                            output = output.trim();
                            if(output.length() == 0 ) {
                                output = mainPack.res.getString(R.string.output_commandexitvalue) + Tuils.SPACE + result.result;
                            }
                        }
                        out.onOutput(output);
                    }
                }
            }.start();

            return true;
        }
    }

    private class AppTrigger implements CmdTrigger {

        @Override
        public boolean trigger(ExecutePack info, String input) {
            String packageName = mainPack.appsManager.findPackage(input, AppsManager.SHOWN_APPS);
            if (packageName == null) {
                return false;
            }

            Intent intent = mainPack.appsManager.getIntent(packageName);
            if (intent == null) {
                return false;
            }

            out.onOutput("-->" + Tuils.SPACE + intent.getComponent().getClassName());

            mContext.startActivity(intent);

            return true;
        }
    }

    private class TuiCommandTrigger implements CmdTrigger {

        @Override
        public boolean trigger(final ExecutePack info, final String input) throws Exception {

            final boolean[] returnValue = new boolean[1];
            Thread t = new Thread() {
                @Override
                public void run() {
                    super.run();

                    Looper.prepare();

                    mainPack.lastCommand = input;

                    try {
                        Command command = CommandTuils.parse(input, info, false);

                        synchronized (returnValue) {
                            returnValue[0] = command != null;
                            returnValue.notify();
                        }

                        if (returnValue[0]) {

                            String output = command.exec(mContext.getResources(), info);
                            if(output != null) {
                                out.onOutput(output);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("andre", "", e);
                        out.onOutput(e.toString());
                    }
                }
            };

            t.start();

            synchronized (returnValue) {
                returnValue.wait();
                return returnValue[0];
            }
        }
    }
}
