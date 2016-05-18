package ohi.andre.consolelauncher.tuils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import dalvik.system.DexFile;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.tuils.tutorial.TutorialIndexActivity;

public class Tuils {

    public static final String SPACE = ShellUtils.SPACE;
    public static final String DOUBLE_SPACE = "  ";
    public static final String NEWLINE = ShellUtils.COMMAND_LINE_END;
    public static final String TRIBLE_SPACE = "   ";
    public static final String DOT = ".";
    public static final String EMPTYSTRING = "";
    private static final String TUI_FOLDER = "t-ui";
    private static final String[] path = {
            "/system/bin",
            "/system/xbin"
    };
    private static final String METACHARACTERS = "\\?*+[](){}^$.|";

    public static List<String> getOSCommands() {
        List<String> commands = new ArrayList<>();

        for (String s : path)
            commands.addAll(Arrays.asList(new File(s).list()));

        return commands;
    }

    public static Set<AppInfo> getApps(PackageManager mgr) {
        Set<AppInfo> set = new HashSet<>();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> infos = mgr.queryIntentActivities(i, 0);

        AppInfo app;
        for (ResolveInfo info : infos) {
            app = new AppInfo(info.activityInfo.packageName, info.loadLabel(mgr).toString());
            set.add(app);
        }

        return set;
    }

    public static List<File> getSongsInFolder(File folder) {
        List<File> songs = new ArrayList<>();

        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                songs.addAll(getSongsInFolder(file));
            else if (file.getName().toLowerCase().endsWith(".mp3"))
                songs.add(file);
        }

        return songs;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void enableUpNavigation(AppCompatActivity activity) {
        try {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
        }
    }

    public static void showTutorial(Context context) {
        Intent intent = new Intent(context, TutorialIndexActivity.class);
        context.startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void openSettingsPage(Activity c, String toast) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", c.getPackageName(), null);
        intent.setData(uri);
        c.startActivity(intent);
        Toast.makeText(c, toast, Toast.LENGTH_LONG).show();
    }

    public static void requestAdmin(Activity a, ComponentName component, String label) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, label);
        a.startActivityForResult(intent, 0);
    }

    public static String removeExtension(String s) {
        return s.substring(0, s.lastIndexOf("."));
    }

    public static String ramDetails(ActivityManager mgr, MemoryInfo info) {
        mgr.getMemoryInfo(info);
        long availableMegs = info.availMem / 1048576L;

        return availableMegs + " MB";
    }

    public static List<String> getClassesOfPackage(String packageName, Context c)
            throws IOException {
        List<String> classes = new ArrayList<>();
        String packageCodePath = c.getPackageCodePath();
        DexFile df = new DexFile(packageCodePath);
        for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
            String className = iter.nextElement();
            if (className.contains(packageName) && !className.contains("$"))
                classes.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
        }

        return classes;
    }

    @SuppressWarnings("unchecked")
    public static CommandAbstraction getCommandInstance(String cmdName) throws Exception {
        Class<CommandAbstraction> clazz = (Class<CommandAbstraction>) Class.forName(cmdName);
        Constructor<?> ctor = clazz.getConstructor();
        return (CommandAbstraction) ctor.newInstance();
    }

    public static int findPrefix(List<String> list, String prefix) {
        for (int count = 0; count < list.size(); count++)
            if (list.get(count).startsWith(prefix))
                return count;
        return -1;
    }

    public static int count(String string, String toCount) {
        return string.length() - string.replaceAll(toCount, "").length();
    }

    public static int count(CharSequence[] sequences, String toCount) {
        return count(toPlanSequence(sequences).toString(), toCount);
    }

    public static boolean verifyRoot() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("echo \"root?\" >/system/sd/temporary.txt\n");

            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                return p.exitValue() != 255;
            } catch (InterruptedException e) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static void insertHeaders(List<String> s, boolean newLine) {
        char current = 0;
        for (int count = 0; count < s.size(); count++) {
            char c = 0;

            String st = s.get(count);
            for (int count2 = 0; count2 < st.length(); count2++) {
                c = st.charAt(count2);
                if (c != ' ')
                    break;
            }

            if (current != c) {
                s.add(count, (newLine ? "\n" : "") + Character.toString(c).toUpperCase() + (newLine ? "\n" : ""));
                current = c;
            }
        }
    }

    public static void addPrefix(List<String> list, String prefix) {
        for (int count = 0; count < list.size(); count++)
            list.set(count, prefix.concat(list.get(count)));
    }

    public static void addSeparator(List<String> list, String separator) {
        for (int count = 0; count < list.size(); count++)
            list.set(count, list.get(count).concat(separator));
    }

    public static String toPlanString(String[] strings, String separator) {
        String output = "";
        for (int count = 0; count < strings.length; count++) {
            output = output.concat(strings[count]);
            if (count < strings.length - 1)
                output = output.concat(separator);
        }
        return output;
    }

    public static String toPlanString(String[] strings) {
        return Tuils.toPlanString(strings, "\n");
    }

    public static String toPlanString(List<String> strings, String separator) {
        String[] object = new String[strings.size()];
        return Tuils.toPlanString(strings.toArray(object), separator);
    }

    public static String filesToPlanString(List<File> files, String separator) {
        StringBuilder builder = new StringBuilder();
        int limit = files.size() - 1;
        for (int count = 0; count < files.size(); count++) {
            builder.append(files.get(count).getName());
            if (count < limit) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    public static String toPlanString(List<String> strings) {
        return Tuils.toPlanString(strings, "\n");
    }

    public static CharSequence toPlanSequence(List<CharSequence> sequences, CharSequence separator) {
        return toPlanSequence(sequences.toArray(new CharSequence[sequences.size()]), separator);
    }

    public static CharSequence toPlanSequence(CharSequence[] sequences, CharSequence separator) {
        if (sequences.length == 0)
            return null;

        CharSequence sequence = null;
        int count;
        for (count = 0; (sequence = sequences[count]) == null; count++) {
        }

        CharSequence output = sequences[count];
        do {
            count++;
            CharSequence current = sequences[count];
            if (current == null)
                continue;

            output = TextUtils.concat(output, current);
            if (count < sequences.length - 1 && !current.toString().contains(separator))
                output = TextUtils.concat(output, separator);
        } while (count + 1 < sequences.length);
        return output;
    }

    public static CharSequence toPlanSequence(CharSequence[] sequences) {
        return TextUtils.concat(sequences);
    }

    public static CharSequence[] split(CharSequence input, CharSequence re, int limit) {
        // Can we do it cheaply?
        int len = re.length();
        if (len == 0) {
            return null;
        }
        char ch = re.charAt(0);
        if (len == 1 && METACHARACTERS.indexOf(ch) == -1) {
            // We're looking for a single non-metacharacter. Easy.
        } else if (len == 2 && ch == '\\') {
            // We're looking for a quoted character.
            // Quoted metacharacters are effectively single non-metacharacters.
            ch = re.charAt(1);
            if (METACHARACTERS.indexOf(ch) == -1) {
                return null;
            }
        } else {
            return null;
        }
        // We can do this cheaply...
        // Unlike Perl, which considers the result of splitting the empty string to be the empty
        // array, Java returns an array containing the empty string.
        if (input.length() == 0) {
            return new CharSequence[]{""};
        }
        // Count separators
        int separatorCount = 0;
        int begin = 0;
        int end;
        while (separatorCount + 1 != limit && (end = input.toString().indexOf(ch, begin)) != -1) {
            ++separatorCount;
            begin = end + 1;
        }
        int lastPartEnd = input.length();
        if (limit == 0 && begin == lastPartEnd) {
            // Last part is empty for limit == 0, remove all trailing empty matches.
            if (separatorCount == lastPartEnd) {
                // Input contains only separators.
                return new CharSequence[0];
            }
            // Find the beginning of trailing separators.
            do {
                --begin;
            } while (input.charAt(begin - 1) == ch);
            // Reduce separatorCount and fix lastPartEnd.
            separatorCount -= input.length() - begin;
            lastPartEnd = begin;
        }
        // Collect the result parts.
        CharSequence[] result = new CharSequence[separatorCount];
        begin = 0;
        for (int i = 0; i != separatorCount; ++i) {
            end = input.toString().indexOf(ch, begin);
            result[i] = input.subSequence(begin, end);
            begin = end + 1;
        }

        return result;
    }

    public static String removeUnncesarySpaces(String string) {
        while (string.contains(DOUBLE_SPACE))
            string = string.replace(DOUBLE_SPACE, SPACE);
        return string;
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static boolean isAlpha(String s) {
        char[] chars = s.toCharArray();

        for (char c : chars)
            if (!Character.isLetter(c))
                return false;

        return true;
    }

    public static boolean isNumber(String s) {
        char[] chars = s.toCharArray();

        for (char c : chars)
            if (Character.isLetter(c))
                return false;

        return true;
    }

    public static String trimSpaces(String s) {
        while (s.startsWith(" "))
            s = s.substring(1);
        while (s.endsWith(" "))
            s = s.substring(0, s.length() - 1);
        return s;
    }

    public static String getSDK() {
        return "android-sdk " + Build.VERSION.SDK_INT;
    }

    public static String getUsername(Context context) {
        Pattern email = Patterns.EMAIL_ADDRESS;
        Account[] accs = AccountManager.get(context).getAccounts();
        for (Account a : accs)
            if (email.matcher(a.name).matches())
                return a.name;
        return null;
    }

    public static Intent openFile(File url) {
        Uri uri = Uri.fromFile(url);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
            // Word document
            intent.setDataAndType(uri, "application/msword");
        } else if (url.toString().contains(".apk")) {
            // apk
            intent.setDataAndType(uri,
                    "application/vnd.android.package-archive");
        } else if (url.toString().contains(".pdf")) {
            // PDF file
            intent.setDataAndType(uri, "application/pdf");
        } else if (url.toString().contains(".ppt")
                || url.toString().contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        } else if (url.toString().contains(".xls")
                || url.toString().contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(uri, "application/vnd.ms-excel");
        } else if (url.toString().contains(".zip")
                || url.toString().contains(".rar")) {
            // ZIP Files
            intent.setDataAndType(uri, "application/zip");
        } else if (url.toString().contains(".rtf")) {
            // RTF file
            intent.setDataAndType(uri, "application/rtf");
        } else if (url.toString().contains(".wav")
                || url.toString().contains(".mp3")) {
            // WAV audio file
            intent.setDataAndType(uri, "audio/x-wav");
        } else if (url.toString().contains(".gif")) {
            // GIF file
            intent.setDataAndType(uri, "image/gif");
        } else if (url.toString().contains(".jpg")
                || url.toString().contains(".jpeg")
                || url.toString().contains(".png")) {
            // JPG file
            intent.setDataAndType(uri, "image/jpeg");
        } else if (url.toString().contains(".txt")) {
            // Text file
            intent.setDataAndType(uri, "text/plain");
        } else if (url.toString().contains(".3gp")
                || url.toString().contains(".mpg")
                || url.toString().contains(".mpeg")
                || url.toString().contains(".mpe")
                || url.toString().contains(".mp4")
                || url.toString().contains(".avi")) {
            // Video files
            intent.setDataAndType(uri, "video/*");
        } else {
            intent.setDataAndType(uri, "*/*");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static String getInternalDirectoryPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static File getTuiFolder() {
        return new File(Tuils.getInternalDirectoryPath(), TUI_FOLDER);
    }

}
