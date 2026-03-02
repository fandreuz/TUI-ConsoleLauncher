package ohi.andre.consolelauncher.commands.main.raw;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 11/05/2017.
 */

public class cntcts extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        ls {
            @Override
            public String exec(ExecutePack pack) {
                List<String> list = ((MainPack) pack).contacts.listNamesAndNumbers();
                Tuils.insertHeaders(list, false);
                return Tuils.toPlanString(list);
            }

            @Override
            public int[] args() {
                return new int[0];
            }
        },
        add {
            @Override
            public String exec(ExecutePack pack) {
                Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                pack.context.startActivity(intent);

                return null;
            }

            @Override
            public int[] args() {
                return new int[0];
            }
        },
        rm {
            @Override
            public String exec(ExecutePack pack) {
                if (ContextCompat.checkSelfPermission(pack.context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) pack.context, new String[]{Manifest.permission.WRITE_CONTACTS}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
                    return pack.context.getString(R.string.output_waitingpermission);
                }

                ((MainPack) pack).contacts.delete(pack.getString());
                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONTACTNUMBER};
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_numbernotfound);
            }
        },
        edit {
            @Override
            public String exec(ExecutePack pack) {
                Intent editIntent = new Intent(Intent.ACTION_EDIT);
                editIntent.setDataAndType(((MainPack) pack).contacts.fromPhone(pack.getString()), ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                pack.context.startActivity(editIntent);

                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONTACTNUMBER};
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_numbernotfound);
            }
        },
        l {
            @Override
            public String exec(ExecutePack pack) {
                String[] about = ((MainPack) pack).contacts.about(pack.getString());
                StringBuilder builder = new StringBuilder();

                builder.append(about[ContactManager.NAME]).append(Tuils.NEWLINE);
                builder.append("\t\t").append(about[ContactManager.NUMBERS].replaceAll(Tuils.NEWLINE, Tuils.NEWLINE + "\t\t")).append(Tuils.NEWLINE);
                builder.append("ID: ").append(about[ContactManager.CONTACT_ID]).append(Tuils.NEWLINE);
                builder.append("Contacted ").append(about[ContactManager.TIME_CONTACTED]).append(" time(s)").append(Tuils.NEWLINE);

                return builder.toString();
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONTACTNUMBER};
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_numbernotfound);
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps)
                if (p.endsWith(p1.label()))
                    return p1;
            return null;
        }

        static String[] labels() {
            Param[] ps = values();
            String[] ss = new String[ps.length];

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return null;
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.context.getString(R.string.help_cntcts);
        }
    }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    protected String doThings(ExecutePack pack) {
        if (ContextCompat.checkSelfPermission(pack.context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) pack.context, new String[]{Manifest.permission.READ_CONTACTS}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return pack.context.getString(R.string.output_waitingpermission);
        }
        return null;
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_cntcts;
    }
}
