package ohi.andre.consolelauncher.tuils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dalvik.system.DexFile;
import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.music.MusicManager2;
import ohi.andre.consolelauncher.managers.music.Song;
import ohi.andre.consolelauncher.managers.notifications.NotificationService;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.interfaces.OnBatteryUpdate;
import ohi.andre.consolelauncher.tuils.stuff.FakeLauncherActivity;

public class Tuils {

    public static final String SPACE = " ";
    public static final String DOUBLE_SPACE = "  ";
    public static final String NEWLINE = "\n";
    public static final String TRIBLE_SPACE = "   ";
    public static final String DOT = ".";
    public static final String EMPTYSTRING = "";
    private static final String TUI_FOLDER = "t-ui";
    public static final String MINUS = "-";

    public static Pattern patternNewline = Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

    private static Typeface globalTypeface = null;
    public static String fontPath = null;

    static Pattern calculusPattern = Pattern.compile("([\\+\\-\\*\\/\\^])(\\d+\\.?\\d*)");
    public static double textCalculus(double input, String text) {
        Matcher m = calculusPattern.matcher(text);
        while(m.find()) {
            char operator = m.group(1).charAt(0);
            double value = Double.parseDouble(m.group(2));

            switch (operator) {
                case '+':
                    input += value;
                    break;
                case '-':
                    input -= value;
                    break;
                case '*':
                    input *= value;
                    break;
                case '/':
                    input = input / value;
                    break;
                case '^':
                    input = Math.pow(input, value);
                    break;
            }

            Tuils.log("now im", input);
        }

        return input;
    }

    public static Typeface getTypeface(Context context) {
        if(globalTypeface == null) {
            try {
                XMLPrefsManager.loadCommons(context);
            } catch (Exception e) {
                return null;
            }

            boolean systemFont = XMLPrefsManager.getBoolean(Ui.system_font);
            if(systemFont) globalTypeface = Typeface.DEFAULT;
            else {
                File tui = Tuils.getFolder();
                if(tui == null) {
                    return Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
                }

                Pattern p = Pattern.compile(".[ot]tf$");

                File font = null;
                for(File f : tui.listFiles()) {
                    String name = f.getName();
                    if(p.matcher(name).find()) {
                        font = f;
                        fontPath = f.getAbsolutePath();
                        break;
                    }
                }

                if(font != null) {
                    try {
                        globalTypeface = Typeface.createFromFile(font);
                        if(globalTypeface == null) throw new UnsupportedOperationException();
                    } catch (Exception e) {
                        globalTypeface = null;
                    }
                }
            }

            if(globalTypeface == null) globalTypeface = systemFont ? Typeface.DEFAULT : Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
        }
        return globalTypeface;
    }

    public static void cancelFont() {
        globalTypeface = null;
        fontPath = null;
    }

    public static String locationName(Context context, double lat, double lng) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
            return addresses.get(0).getAddressLine(2);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean notificationServiceIsRunning(Context context) {
        ComponentName collectorComponent = new ComponentName(context, NotificationService.class);
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean collectorRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null ) {
            return false;
        }

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(collectorComponent)) {
                if (service.pid == Process.myPid()) {
                    collectorRunning = true;
                }
            }
        }

        return collectorRunning;
    }

    public static boolean arrayContains(int[] array, int value) {
        if(array == null) return false;

        for(int i : array) {
            if(i == value) {
                return true;
            }
        }
        return false;
    }

    public static String readerToString(Reader initialReader) throws IOException {
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = initialReader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        initialReader.close();
        return buffer.toString();
    }

    private static OnBatteryUpdate batteryUpdate;
    private static BroadcastReceiver batteryReceiver = null;

    public static void registerBatteryReceiver(Context context, OnBatteryUpdate listener) {
        try {
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(batteryUpdate == null) return;

                    switch (intent.getAction()) {
                        case Intent.ACTION_BATTERY_CHANGED:
                            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                            batteryUpdate.update(level);
                            break;
                        case Intent.ACTION_POWER_CONNECTED:
                            batteryUpdate.onCharging();
                            break;
                        case Intent.ACTION_POWER_DISCONNECTED:
                            batteryUpdate.onNotCharging();
                            break;
                    }
                }
            };

            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            iFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            iFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);

            context.registerReceiver(batteryReceiver, iFilter);

            batteryUpdate = listener;
        } catch (Exception e) {
            Tuils.toFile(e);
        }
    }

    public static void unregisterBatteryReceiver(Context context) {
        if(batteryReceiver != null) context.unregisterReceiver(batteryReceiver);
    }

    public static boolean containsExtension(String[] array, String value) {
        try {
            value = value.toLowerCase().trim();
            for (String s : array) {
                if (value.endsWith(s)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<Song> getSongsInFolder(File folder) {
        List<Song> songs = new ArrayList<>();

        File[] files = folder.listFiles();
        if(files == null || files.length == 0) {
            return songs;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                List<Song> s = getSongsInFolder(file);
                if(s != null) {
                    songs.addAll(s);
                }
            }
            else if (containsExtension(MusicManager2.MUSIC_EXTENSIONS, file.getName())) {
                songs.add(new Song(file));
            }
        }

        return songs;
    }

    public static String convertStreamToString(java.io.InputStream is) {
        if (is == null) return Tuils.EMPTYSTRING;

        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : Tuils.EMPTYSTRING;
    }

    public static long download(InputStream in, File file) throws Exception {
        OutputStream out = new FileOutputStream(file, false);

        byte data[] = new byte[1024];

        long bytes = 0;

        int count;
        while ((count = in.read(data)) != -1) {
            out.write(data, 0, count);
            bytes += count;
        }

        out.flush();
        out.close();
        in.close();

        return bytes;
    }

    public static void write(File file, String separator, String... ss) throws Exception {
        FileOutputStream headerStream = new FileOutputStream(file, false);

        for(int c = 0; c < ss.length - 1; c++) {
            headerStream.write(ss[c].getBytes());
            headerStream.write(separator.getBytes());
        }
        headerStream.write(ss[ss.length - 1].getBytes());

        headerStream.flush();
        headerStream.close();
    }

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public static boolean hasNotificationAccess(Context context) {
        String pkgName = BuildConfig.APPLICATION_ID;
        final String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void resetPreferredLauncherAndOpenChooser(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, FakeLauncherActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(selector);

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void openSettingsPage(Context c, String packageName) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        c.startActivity(intent);
    }

    public static Intent requestAdmin(ComponentName component, String explanation) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
        return intent;
    }

    public static Intent webPage(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }

    public static double getAvailableInternalMemorySize(int unit) {
        return getAvailableSpace(Environment.getDataDirectory(), unit);
    }

    public static double getTotalInternalMemorySize(int unit) {
        return getTotaleSpace(Environment.getDataDirectory(), unit);
    }

    public static double getAvailableExternalMemorySize(int unit) {
        try {
            return getAvailableSpace(XMLPrefsManager.get(File.class, Behavior.external_storage_path), unit);
        } catch (Exception e) {
            return -1;
        }
    }

    public static double getTotalExternalMemorySize(int unit) {
        try {
            return getTotaleSpace(XMLPrefsManager.get(File.class, Behavior.external_storage_path), unit);
        } catch (Exception e) {
            return -1;
        }
    }

    public static double getAvailableSpace(File dir, int unit) {
        if(dir == null) return -1;

        StatFs statFs = new StatFs(dir.getAbsolutePath());
        long blocks = statFs.getAvailableBlocks();
        return formatSize(blocks * statFs.getBlockSize(), unit);
    }

    public static double getTotaleSpace(File dir, int unit) {
        if(dir == null) return -1;

        StatFs statFs = new StatFs(dir.getAbsolutePath());
        long blocks = statFs.getBlockCount();
        return formatSize(blocks * statFs.getBlockSize(), unit);
    }

    public static double percentage(double part, double total) {
        return round(part * 100 / total, 2);
    }

    public static double formatSize(long bytes, int unit) {
        double convert = 1048576.0;
        double smallConvert = 1024.0;

        double result;

        switch (unit) {
            case TERA:
                result = (bytes / convert) / convert;
                break;
            case GIGA:
                result = (bytes / convert) / smallConvert;
                break;
            case MEGA:
                result = bytes / convert;
                break;
            case KILO:
                result = bytes / smallConvert;
                break;
            case BYTE:
                result = bytes;
                break;
            default: return -1;
        }

        return round(result, 2);
    }

    public static boolean isMyLauncherDefault(PackageManager packageManager) {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<>();
        filters.add(filter);

        final String myPackageName = BuildConfig.APPLICATION_ID;
        List<ComponentName> activities = new ArrayList<>();

        // You can use name of your package here as third argument
        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            if (myPackageName.equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }


    public static SpannableString span(CharSequence text, int color) {
        return span(null, text, color, Integer.MAX_VALUE);
    }

    public static SpannableString span(Context context, int size, CharSequence text) {
        return span(context, text, Integer.MAX_VALUE, size);
    }

    public static SpannableString span(Context context, CharSequence text, int color, int size) {
        return span(context, Integer.MAX_VALUE, color, text, size);
    }

    public static SpannableString span(int bgColor, int foreColor, CharSequence text) {
        return span(null, bgColor, foreColor, text, Integer.MAX_VALUE);
    }

    public static SpannableString span(Context context, int bgColor, int foreColor, CharSequence text, int size) {
        if(text == null) {
            text = Tuils.EMPTYSTRING;
        }

        SpannableString spannableString;
        if(text instanceof SpannableString) spannableString = (SpannableString) text;
        else spannableString = new SpannableString(text);

        if(size != Integer.MAX_VALUE && context != null) spannableString.setSpan(new AbsoluteSizeSpan(convertSpToPixels(size, context)), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if(foreColor != Integer.MAX_VALUE) spannableString.setSpan(new ForegroundColorSpan(foreColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if(bgColor != Integer.MAX_VALUE) spannableString.setSpan(new BackgroundColorSpan(bgColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    public static int span(int bgColor, SpannableString text, String section, int fromIndex) {
        int index = text.toString().indexOf(section, fromIndex);
        if(index == -1) return index;

        text.setSpan(new BackgroundColorSpan(bgColor), index, index + section.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return index + section.length();
    }

    public static int convertSpToPixels(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static String inputStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : Tuils.EMPTYSTRING;
    }

//    static final int WEATHER_TIMEOUT = 6000;
//    public static boolean location(Context context, final ArgsRunnable whenFound, final Runnable notFound, final Handler handler) {
//        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        if(locationManager == null) return false;
//
//        final LocationListener locationListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(Location location) {
//                whenFound.run(location.getLatitude(), location.getLongitude());
//            }
//
//            @Override
//            public void onStatusChanged(String provider, int status, Bundle extras) {
//            }
//
//            @Override
//            public void onProviderEnabled(String provider) {
//            }
//
//            @Override
//            public void onProviderDisabled(String provider) {
//            }
//        };
//
//        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//        boolean passiveProvider = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
//
//        if (!gpsStatus && !networkStatus) return false;
//
//        try {
//            locationManager.requestSingleUpdate(gpsStatus ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER, locationListener, Looper.getMainLooper());
//        } catch (SecurityException e) {
//            Tuils.log(e);
//            Tuils.toFile(e);
//            return false;
//        }
//
//        Location location;
//        try {
//            Location[] ls = {
//                    locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER),
//                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
//                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)};
//
//            location = ls[0];
//            for(int c = 1; c < ls.length; c++) {
//                if(location == null) location = ls[c];
//                else if(ls[c] != null && location.getTime() < ls[c].getTime()) location = ls[c];
//            }
//        } catch (SecurityException e) {
//            Tuils.toFile(e);
//            return false;
//        }
//
//        if(handler != null) {
//            handler.postDelayed(notFound, WEATHER_TIMEOUT);
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if(locationManager != null) locationManager.removeUpdates(locationListener);
//                }
//            }, WEATHER_TIMEOUT);
//        }
//
//        return true;
//    }

//    public static Location getLocation(Context context) {
//        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        if(locationManager == null) return null;
//
//        Location location;
//        try {
//            Location[] ls = {
//                    locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER),
//                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
//                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)};
//
//            location = ls[0];
//            for(int c = 1; c < ls.length; c++) {
//                if(location == null) location = ls[c];
//                else if(ls[c] != null && location.getTime() < ls[c].getTime()) location = ls[c];
//            }
//
//            return location;
//        } catch (SecurityException e) {
//            Tuils.toFile(e);
//            return null;
//        }
//    }

    public abstract static class ArgsRunnable implements Runnable {
        private Object[] args;

        public void setArgs(Object... args) {
            this.args = args;
        }

        public void run(Object... args) {
            setArgs(args);
            run();
        }

        public <T> T get(Class<T> c, int index) {
            if(index < args.length) return (T) args[index];
            return null;
        }
    }

    public static void deleteContentOnly(File dir) {
        File[] files = dir.listFiles();
        if(files == null) return;

        for(File f : dir.listFiles()) {
            if(f.isDirectory()) delete(f);
            f.delete();
        }
    }

    public static void delete(File dir) {
        File[] files = dir.listFiles();
        if(files == null) return;

        for(File f : dir.listFiles()) {
            if(f.isDirectory()) delete(f);
            f.delete();
        }
        dir.delete();
    }

    public static boolean insertOld(File oldFile) {
        if(oldFile == null || !oldFile.exists()) return false;

        String oldPath = oldFile.getAbsolutePath();

        File oldFolder = new File(Tuils.getFolder(), "old");
        if(!oldFolder.exists()) oldFolder.mkdir();

        File dest = new File(oldFolder, oldFile.getName());
        if(dest.exists()) dest.delete();

        return oldFile.renameTo(dest) && new File(oldPath).delete();
    }

    public static File getOld(String name) {
        File old = new File(Tuils.getFolder(), "old");
        File file = new File(old, name);

        if(file.exists()) return file;
        return null;
    }

    public static void deepView(View v) {
        Tuils.log(v.toString());

        if(!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;

        Tuils.log(g.getChildCount());
        for(int c = 0; c < g.getChildCount(); c++) deepView(g.getChildAt(c));

        Tuils.log("end of parents of: " + v.toString());
    }

    private static View.OnClickListener deepClickListener = v -> Tuils.log(v.toString());

    public static void deepClickView(View v) {
        v.setOnClickListener(deepClickListener);

        if(!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;

        for(int c = 0; c < g.getChildCount(); c++) deepClickView(g.getChildAt(c));
    }

    public static void scaleImage(ImageView view, int newX, int newY) throws NoSuchElementException {
        // Get bitmap from the the ImageView.
        Bitmap bitmap = null;

        try {
            Drawable drawing = view.getDrawable();
            bitmap = ((BitmapDrawable) drawing).getBitmap();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("No drawable on given view");
        }

        // Get current dimensions AND the desired bounding box
        int width = 0;

        try {
            width = bitmap.getWidth();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("Can't find bitmap on given view/drawable");
        }

        int height = bitmap.getHeight();
        int xBounding = dpToPx(view.getContext(), newX);
        int yBounding = dpToPx(view.getContext(), newY);

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) xBounding) / width;
        float yScale = ((float) yBounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = scaledBitmap.getWidth(); // re-use
        height = scaledBitmap.getHeight(); // re-use
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);

        // Apply the scaled bitmap
        view.setImageDrawable(result);

        // Now change ImageView's dimensions to match the scaled image
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }

    public static int dpToPx(Context context, int dp) {
        float density = context.getApplicationContext().getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density);
    }

    public static void sendOutput(Context context, int res) {
        sendOutput(Integer.MAX_VALUE, context, res);
    }

    public static void sendOutput(int color, Context context, int res) {
        sendOutput(color, context, context.getString(res));
    }

    public static void sendOutput(Context context, int res, int type) {
        sendOutput(Integer.MAX_VALUE, context, res, type);
    }

    public static void sendOutput(int color, Context context, int res, int type) {
        sendOutput(color, context, context.getString(res), type);
    }

    public static void sendOutput(Context context, CharSequence s) {
        sendOutput(Integer.MAX_VALUE, context, s);
    }

    public static void sendOutput(int color, Context context, CharSequence s) {
        sendOutput(color, context, s, TerminalManager.CATEGORY_OUTPUT);
    }

    public static void sendOutput(Context context, CharSequence s, int type) {
        sendOutput(Integer.MAX_VALUE, context, s, type);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type) {
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendOutput(MainPack mainPack, CharSequence s, int type) {
        sendOutput(mainPack.commandColor, mainPack.context, s, type);
    }

    public static void sendOutput(Context context, CharSequence s, int type, Object action) {
        sendOutput(Integer.MAX_VALUE, context, s, type, action);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type, Object action) {
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);

        if(action instanceof String) intent.putExtra(PrivateIOReceiver.ACTION, (String) action);
        else if(action instanceof Parcelable) intent.putExtra(PrivateIOReceiver.ACTION, (Parcelable) action);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendOutput(Context context, CharSequence s, int type, Object action, Object longAction) {
        sendOutput(Integer.MAX_VALUE, context, s, type, action, longAction);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type, Object action, Object longAction) {
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);

        if(action instanceof String) intent.putExtra(PrivateIOReceiver.ACTION, (String) action);
        else if(action instanceof Parcelable) intent.putExtra(PrivateIOReceiver.ACTION, (Parcelable) action);

        if(longAction instanceof String) intent.putExtra(PrivateIOReceiver.LONG_ACTION, (String) longAction);
        else if(longAction instanceof Parcelable) intent.putExtra(PrivateIOReceiver.LONG_ACTION, (Parcelable) longAction);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendInput(Context context, String text) {
        Intent intent = new Intent(PrivateIOReceiver.ACTION_INPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, text);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static final int TERA = 0;
    public static final int GIGA = 1;
    public static final int MEGA = 2;
    public static final int KILO = 3;
    public static final int BYTE = 4;

    private static long total = -1;

    public static double freeRam(ActivityManager mgr, MemoryInfo info) {
        mgr.getMemoryInfo(info);
        return info.availMem;
    }

    public static long totalRam() {
        if(total > 0) return total;

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")));

            String line;
            while((line = reader.readLine()) != null) {
                if(line.startsWith("MemTotal")) {
                    line = line.replaceAll("\\D+", Tuils.EMPTYSTRING);
                    return Long.parseLong(line);
                }
            }
        } catch (Exception e) {}
        return 0;
    }

    public static double round(double value, int places) {
        if (places < 0) places = 0;

        try {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.doubleValue();
        } catch (Exception e) {
            return value;
        }
    }

    public static List<String> getClassesInPackage(String packageName, Context c) throws IOException {
        List<String> classes = new ArrayList<>();
        String packageCodePath = c.getPackageCodePath();
        DexFile df = new DexFile(packageCodePath);
        for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
            String className = iter.nextElement();
            if (className.contains(packageName) && !className.contains("$")) {
                classes.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
            }
        }

        return classes;
    }

    public static int scale(int[] from, int[] to, int n) {
        return (to[1] - to[0])*(n - from[0])/(from[1] - from[0]) + to[0];
    }

    public static String[] toString(Enum[] enums) {
        String[] arr = new String[enums.length];
        for(int count = 0; count < enums.length; count++) arr[count] = enums[count].name();
        return arr;
    }

    private static String getNicePath(String filePath) {
        if(filePath == null) return "null";

        String home = XMLPrefsManager.get(File.class, Behavior.home_path).getAbsolutePath();

        if(filePath.equals(home)) {
            return "~";
        } else if(filePath.startsWith(home)) {
            return "~" + filePath.replace(home, Tuils.EMPTYSTRING);
        } else {
            return filePath;
        }
    }

    public static int find(Object o, Object[] array) {
        return find(o, Arrays.asList(array));
    }

    public static int find(Object o, List list) {
        for(int count = 0; count < list.size(); count++) {
            Object x = list.get(count);
            if(x == null) continue;

            if(o == x) return count;

            if (o instanceof XMLPrefsSave) {
                try {
                    if (((XMLPrefsSave) o).label().equals((String) x)) return count;
                } catch (Exception e) {}
            }

            if (o instanceof String && x instanceof XMLPrefsSave) {
                try {
                    if (((XMLPrefsSave) x).label().equals((String) o)) return count;
                } catch (Exception e) {}
            }

            try {
                if (o.equals(x) || x.equals(o)) return count;
            } catch (Exception e) {
                continue;
            }
        }
        return -1;
    }

    static Pattern pd = Pattern.compile("%d", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    static Pattern pu = Pattern.compile("%u", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    static Pattern pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    public static String getHint(String currentPath) {
        if(!XMLPrefsManager.getBoolean(Ui.show_session_info)) return null;

        String format = XMLPrefsManager.get(Behavior.session_info_format);
        if(format.length() == 0) return null;

        String deviceName = XMLPrefsManager.get(Ui.deviceName);
        if(deviceName == null || deviceName.length() == 0) {
            deviceName = Build.DEVICE;
        }

        String username = XMLPrefsManager.get(Ui.username);
        if(username == null) username = Tuils.EMPTYSTRING;

        format = pd.matcher(format).replaceAll(Matcher.quoteReplacement(deviceName));
        format = pu.matcher(format).replaceAll(Matcher.quoteReplacement(username));
        format = pp.matcher(format).replaceAll(Matcher.quoteReplacement(Tuils.getNicePath(currentPath)));

        return format;
    }

    public static int findPrefix(List<String> list, String prefix) {
        for (int count = 0; count < list.size(); count++)
            if (list.get(count).startsWith(prefix))
                return count;
        return -1;
    }

    public static int mmToPx(DisplayMetrics metrics, int mm) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm, metrics);
    }

    public static void insertHeaders(List<String> s, boolean newLine) {
        char current = 0;
        for (int count = 0; count < s.size(); count++) {
            String st = s.get(count).trim().toUpperCase();
            if(st.length() < 0) continue;

            char c = st.charAt(0);
            if (current != c) {
                s.add(count, (newLine ? NEWLINE : EMPTYSTRING) + c + (newLine ? NEWLINE : EMPTYSTRING));
                current = c;
            }
        }
    }

    public static void addPrefix(List<String> list, String prefix) {
        for (int count = 0; count < list.size(); count++) {
            list.set(count, prefix.concat(list.get(count)));
        }
    }

    public static void addSeparator(List<String> list, String separator) {
        for (int count = 0; count < list.size(); count++)
            list.set(count, list.get(count).concat(separator));
    }

    public static String toPlanString(String[] strings, String separator) {
        if(strings == null) {
            return Tuils.EMPTYSTRING;
        }

        String output = Tuils.EMPTYSTRING;
        for (int count = 0; count < strings.length; count++) {
            output = output.concat(strings[count]);
            if (count < strings.length - 1) output = output.concat(separator);
        }
        return output;
    }

    public static String toPlanString(String[] strings) {
        if (strings != null) {
            return Tuils.toPlanString(strings, Tuils.NEWLINE);
        }
        return Tuils.EMPTYSTRING;
    }

    public static String toPlanString(String separator, List strings) {
        if(strings == null) {
            return Tuils.EMPTYSTRING;
        }

        String output = Tuils.EMPTYSTRING;
        for (int count = 0; count < strings.size(); count++) {
            output = output.concat(strings.get(count).toString());
            if (count < strings.size() - 1) output = output.concat(separator);
        }
        return output;
    }

    public static String nodeToString(Node node) {
        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(node);
            trans.transform(source, result);

            return sw.toString();
        }
        catch (TransformerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void log(Object o) {
//        Log.e("andre", Arrays.toString(Thread.currentThread().getStackTrace()));

        if(o instanceof Throwable) {
            Log.e("andre", "", (Throwable) o);
        } else {
            String text;
            if(o instanceof Object[]) text = Arrays.toString((Object[]) o);
            else text = o.toString();
            Log.e("andre", text);
        }
    }

    public static void log(Object o, Object o2) {
        if(o instanceof Object[] && o2 instanceof Object[]){
            Log.e("andre", Arrays.toString((Object[]) o) + " -- " + Arrays.toString((Object[]) o2));
        } else {
            Log.e("andre", String.valueOf(o) + " -- " + String.valueOf(o2));
        }
    }

    public static void log(Object o, PrintStream to) {
//        Log.e("andre", Arrays.toString(Thread.currentThread().getStackTrace()));

        if(o instanceof Throwable) {
            ((Throwable) o).printStackTrace(to);
        } else {
            String text;
            if(o instanceof Object[]) text = Arrays.toString((Object[]) o);
            else text = o.toString();

            try {
                to.write(text.getBytes());
            } catch (IOException e) {
                Tuils.log(e);
            }
        }
    }

    public static void log(Object o, Object o2, OutputStream to) {
        try {
            if(o instanceof Object[] && o2 instanceof Object[]){
                to.write((Arrays.toString((Object[]) o) + " -- " + Arrays.toString((Object[]) o2)).getBytes());
            } else {
                to.write((String.valueOf(o) + " -- " + String.valueOf(o2)).getBytes());
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public static boolean hasInternetAccess() {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://clients3.google.com/generate_204").openConnection());
            return (urlc.getResponseCode() == 204 && urlc.getContentLength() == 0);
        } catch (IOException e) {
            return false;
        }
    }

    public static <T> T getDefaultValue(Class<T> clazz) {
        return (T) Array.get(Array.newInstance(clazz, 1), 0);
    }

    public static void toFile(String s) {
        try {
            RandomAccessFile f = new RandomAccessFile(new File(Tuils.getFolder(), "crash.txt"), "rw");
            f.seek(0);
            f.write((new Date().toString() + Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
            OutputStream is = Channels.newOutputStream(f.getChannel());
            is.write(s.getBytes());
            f.write((Tuils.NEWLINE + Tuils.NEWLINE).getBytes());

            is.close();
            f.close();
        } catch (Exception e1) {}
    }

    public static void toFile(Object o) {
        if(o == null) return;

//            RandomAccessFile f = new RandomAccessFile(new File(Tuils.getFolder(), "crash.txt"), "rw");
//            f.seek(0);
//            f.write((new Date().toString() + Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
//            OutputStream is = Channels.newOutputStream(f.getChannel());
//            e.printStackTrace(new PrintStream(is));
//            f.write((Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
//
//            is.close();
//            f.close();

        try {
            FileOutputStream stream = new FileOutputStream(new File(Tuils.getFolder(), "crash.txt"));
            stream.write((Tuils.NEWLINE + Tuils.NEWLINE).getBytes());

            if(o instanceof Throwable) {
                PrintStream ps = new PrintStream(stream);
                ((Throwable) o).printStackTrace(ps);
            } else {
                stream.write(o.toString().getBytes());
            }

            stream.write((Tuils.NEWLINE + "----------------------------").getBytes());

            stream.close();
        } catch (Exception e1) {}
    }

    public static String toPlanString(List<String> strings, String separator) {
        if(strings != null) {
            String[] object = new String[strings.size()];
            return Tuils.toPlanString(strings.toArray(object), separator);
        }
        return Tuils.EMPTYSTRING;
    }

    public static String filesToPlanString(List<File> files, String separator) {
        if(files == null || files.size() == 0) {
            return null;
        }

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
        return Tuils.toPlanString(strings, NEWLINE);
    }

    public static String toPlanString(Object[] objs, String separator) {
        if(objs == null) {
            return Tuils.EMPTYSTRING;
        }

        StringBuilder output = new StringBuilder();
        for(int count = 0; count < objs.length; count++) {
            output.append(objs[count]);
            if(count < objs.length - 1) {
                output.append(separator);
            }
        }
        return output.toString();
    }

    static Pattern unnecessarySpaces = Pattern.compile("\\s{2,}");
    public static String removeUnncesarySpaces(String string) {
        return unnecessarySpaces.matcher(string).replaceAll(Tuils.SPACE);
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static boolean isAlpha(String s) {
        if(s == null) {
            return false;
        }
        char[] chars = s.toCharArray();

        for (char c : chars)
            if (!Character.isLetter(c))
                return false;

        return true;
    }

    public static boolean isPhoneNumber(String s) {
        if(s == null) {
            return false;
        }
        char[] chars = s.toCharArray();

        for (char c : chars) {
            if (Character.isLetter(c)) {
                return false;
            }
        }

        return true;
    }

//    return 0 if only digit
    public static char firstNonDigit(String s) {
        if(s == null) {
            return 0;
        }

        char[] chars = s.toCharArray();

        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return c;
            }
        }

        return 0;
    }

    public static boolean isNumber(String s) {
        if(s == null || s.length() == 0) {
            return false;
        }

        char[] chars = s.toCharArray();

        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    public static Intent openFile(Context c, File f) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri u = buildFile(c, f);
        String mimetype = MimeTypes.getMimeType(f.getAbsolutePath(), f.isDirectory());

        intent.setDataAndType(u, mimetype);

        int flags;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
        } else {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        }

        intent.addFlags(flags);

        return intent;
    }

    public static Intent shareFile(Context c, File f) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri u = buildFile(c, f);

        String mimetype = MimeTypes.getMimeType(f.getAbsolutePath(), f.isDirectory());

        intent.setDataAndType(u, mimetype);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, u);

        return intent;
    }

    private static Uri buildFile(Context context, File file) {
        Uri uri;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            uri = Uri.fromFile(file);
        }
        else {
            uri = FileProvider.getUriForFile(context, GenericFileProvider.PROVIDER_NAME, file);
        }
        return uri;
    }

    private static File getTuiFolder() {
        File internalDir = Environment.getExternalStorageDirectory();
        return new File(internalDir, TUI_FOLDER);
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }

    public static String getTextFromClipboard(Context context) {
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item item = manager.getPrimaryClip().getItemAt(0);
                return item.getText().toString();
            } else {
                android.text.ClipboardManager manager = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                return manager.getText().toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static int dpToPx(Resources resources, int dp) {
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private static final int FILEUPDATE_DELAY = 100;
    private static File folder = null;
    public static File getFolder() {
        if(folder != null) return folder;

        int elapsedTime = 0;
        while (elapsedTime < 1000) {
            File tuiFolder = Tuils.getTuiFolder();
            if(tuiFolder != null && ((tuiFolder.exists() && tuiFolder.isDirectory()) || tuiFolder.mkdir())) {
                folder = tuiFolder;
                return folder;
            }

            try {
                Thread.sleep(FILEUPDATE_DELAY);
            } catch (InterruptedException e) {}

            elapsedTime += FILEUPDATE_DELAY;
        }

        return null;
    }

    public static int alphabeticCompare(String s1, String s2) {
        String cmd1 = removeSpaces(s1).toLowerCase();
        String cmd2 = removeSpaces(s2).toLowerCase();

        for (int count = 0; count < cmd1.length() && count < cmd2.length(); count++) {
            char c1 = cmd1.charAt(count);
            char c2 = cmd2.charAt(count);

            if (c1 < c2) {
                return -1;
            } else if (c1 > c2) {
                return 1;
            }
        }

        if (s1.length() > s2.length()) {
            return 1;
        } else if (s1.length() < s2.length()) {
            return -1;
        }
        return 0;
    }

    private static final String SPACE_REGEXP = "\\s";
    public static String removeSpaces(String string) {
        return string.replaceAll(SPACE_REGEXP, EMPTYSTRING);
    }

    public static String getNetworkType(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2g";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3g";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4g";
            default:
                return "unknown";
        }
    }

    public static void setCursorDrawableColor(EditText editText, int color) {
        try {
            Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            fCursorDrawableRes.setAccessible(true);
            int mCursorDrawableRes = fCursorDrawableRes.getInt(editText);
            Field fEditor = TextView.class.getDeclaredField("mEditor");
            fEditor.setAccessible(true);
            Object editor = fEditor.get(editText);
            Class<?> clazz = editor.getClass();
            Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
            fCursorDrawable.setAccessible(true);
            Drawable[] drawables = new Drawable[2];
            drawables[0] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
            drawables[1] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
            drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            fCursorDrawable.set(editor, drawables);
        } catch (Throwable ignored) {}
    }

    public static int nOfBytes(File file) {
        int count = 0;
        try {
            FileInputStream in = new FileInputStream(file);

            while(in.read() != -1) count++;

            return count;
        } catch (IOException e) {
            Tuils.log(e);
            return count;
        }
    }

    public static void sendXMLParseError(Context context, String PATH, SAXParseException e) {
        Tuils.sendOutput(
                Color.RED,
                context, context.getString(R.string.output_xmlproblem1) + Tuils.SPACE + PATH + context.getString(R.string.output_xmlproblem2) + Tuils.NEWLINE + context.getString(R.string.output_errorlabel) +
                "File: " + e.getSystemId() + Tuils.NEWLINE +
                "Message" + e.getMessage() + Tuils.NEWLINE +
                "Line" + e.getLineNumber() + Tuils.NEWLINE +
                "Column" + e.getColumnNumber());
    }

    public static void sendXMLParseError(Context context, String PATH) {
        Tuils.sendOutput(Color.RED, context, context.getString(R.string.output_xmlproblem1) + Tuils.SPACE + PATH + context.getString(R.string.output_xmlproblem2));
    }
}
