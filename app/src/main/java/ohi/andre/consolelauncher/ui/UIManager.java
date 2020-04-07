package ohi.andre.consolelauncher.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArraySet;
import android.support.v4.view.GestureDetectorCompat;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.RedirectCommand;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.TuiLocationManager;
import ohi.andre.consolelauncher.settings.SettingsManager;
import ohi.andre.consolelauncher.settings.options.Behavior;
import ohi.andre.consolelauncher.settings.options.Suggestions;
import ohi.andre.consolelauncher.settings.options.Theme;
import ohi.andre.consolelauncher.settings.options.Toolbar;
import ohi.andre.consolelauncher.settings.options.Ui;
import ohi.andre.consolelauncher.suggestions.SuggestionTextWatcher;
import ohi.andre.consolelauncher.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.tuils.Color;
import ohi.andre.consolelauncher.tuils.OutlineTextView;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
import ohi.andre.consolelauncher.tuils.stuff.PolicyReceiver;
import ohi.andre.consolelauncher.ui.device.info.DeviceInfoManager;

import static ohi.andre.consolelauncher.tuils.Tuils.BiPack;
import static ohi.andre.consolelauncher.tuils.Tuils.dpToPx;
import static ohi.andre.consolelauncher.tuils.Tuils.getFolder;
import static ohi.andre.consolelauncher.tuils.Tuils.identity2;
import static ohi.andre.consolelauncher.tuils.Tuils.identity3;
import static ohi.andre.consolelauncher.tuils.Tuils.identity4;
import static ohi.andre.consolelauncher.tuils.Tuils.identityPredicate;
import static ohi.andre.consolelauncher.tuils.Tuils.locationName;
import static ohi.andre.consolelauncher.tuils.Tuils.log;
import static ohi.andre.consolelauncher.tuils.Tuils.mmToPx;
import static ohi.andre.consolelauncher.tuils.Tuils.requestAdmin;
import static ohi.andre.consolelauncher.tuils.Tuils.sendOutput;
import static ohi.andre.consolelauncher.tuils.Tuils.toFile;
import static ohi.andre.consolelauncher.tuils.Tuils.unregisterBatteryReceiver;

public class UIManager implements OnTouchListener {

    public static String ACTION_UPDATE_SUGGESTIONS = BuildConfig.APPLICATION_ID + ".ui_update_suggestions";
    public static String ACTION_UPDATE_HINT = BuildConfig.APPLICATION_ID + ".ui_update_hint";
    public static String ACTION_ROOT = BuildConfig.APPLICATION_ID + ".ui_root";
    public static String ACTION_NOROOT = BuildConfig.APPLICATION_ID + ".ui_noroot";
    public static String ACTION_LOGTOFILE = BuildConfig.APPLICATION_ID + ".ui_log";
    public static String ACTION_CLEAR = BuildConfig.APPLICATION_ID + "ui_clear";
    public static String ACTION_WEATHER = BuildConfig.APPLICATION_ID + "ui_weather";
    public static String ACTION_WEATHER_GOT_LOCATION = BuildConfig.APPLICATION_ID + "ui_weather_location";
    public static String ACTION_WEATHER_DELAY = BuildConfig.APPLICATION_ID + "ui_weather_delay";
    public static String ACTION_WEATHER_MANUAL_UPDATE = BuildConfig.APPLICATION_ID + "ui_weather_update";

    public static final int TERMINAL_VIEW_INDEX = 0;
    public static final int INPUT_VIEW_INDEX = 1;
    public static final int PREFIX_VIEW_INDEX = 2;
    public static final int SUBMIT_VIEW_INDEX = 3;
    public static final int BACK_VIEW_INDEX = 4;
    public static final int NEXT_VIEW_INDEX = 5;
    public static final int DELETE_VIEW_INDEX = 6;
    public static final int PASTE_VIEW_INDEX = 7;
    public static final int INPUT_GROUP_INDEX = 8;
    public static final int TOOLBAR_VIEW_INDEX = 9;
    public static final int DEVICE_INFO_INDEX = 10;
    public static final int SUGGESTIONS_GROUP_INDEX = 11;

    // the number of styleable views in UIManager
    public static final int VIEWS_LENGTH = 12;

    public static String FILE_NAME = "fileName";
    public static String PREFS_NAME = "ui";

    private SuggestionsManager suggestionsManager;

    // the view which shows the output of the terminal
    private TextView terminalView;

    // the command invoked when a double tap occurs
    private String doubleTapCmd;

    // if true, a double tap will lock the screen
    private boolean lockOnDoubleTap;

    private BroadcastReceiver receiver;

    private final Context context;

    private DevicePolicyManager policy;
    private ComponentName component;
    private GestureDetectorCompat gestureDetector;

    private TextView inputView, submitView, prefixView;
    private View inputGroup;

    private InputMethodManager imm;
    private TerminalManager terminalManager;

    // if true, the toolbar will be hidden when the input is empty
    // todo: remove. who would want this?
    private boolean hideToolbarNoInput;

    private View toolbarView;
    private ImageView backView, nextView, deleteView, pasteView;

    // holds Disposables from RxJava
    private CompositeDisposable disposableSet;

    // if true, the screen will be cleared everytime the screen is locked
    private boolean clearOnLock;

    private DeviceInfoManager deviceInfoManager;

    public static Typeface loadTypeface(Context context) {
        if (globalTypeface == null) {
            try {
                SettingsManager.loadCommons(context);
            } catch (Exception e) {
                return null;
            }

            boolean systemFont = SettingsManager.getBoolean(Ui.system_font);
            if (systemFont) globalTypeface = Typeface.DEFAULT;
            else {
                File tui = getFolder();
                if (tui == null) {
                    return Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
                }

                Pattern p = Pattern.compile(".[ot]tf$");

                File font = null;
                for (File f : tui.listFiles()) {
                    String name = f.getName();
                    if (p.matcher(name).find()) {
                        font = f;
                        fontPath = f.getAbsolutePath();
                        break;
                    }
                }

                if (font != null) {
                    try {
                        globalTypeface = Typeface.createFromFile(font);
                        if (globalTypeface == null) throw new UnsupportedOperationException();
                    } catch (Exception e) {
                        globalTypeface = null;
                    }
                }
            }

            if (globalTypeface == null)
                globalTypeface = systemFont ? Typeface.DEFAULT : Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
        }
        return globalTypeface;
    }

    // this code is a bit tangled, it depends on the big number of observables needed to keep track of user preferences..
    public UIManager(Context context, final ViewGroup rootView, MainPack mainPack, boolean canApplyTheme, CommandExecuter executer) {

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_SUGGESTIONS);
        filter.addAction(ACTION_UPDATE_HINT);
        filter.addAction(ACTION_ROOT);
        filter.addAction(ACTION_NOROOT);
//        filter.addAction(ACTION_CLEAR_SUGGESTIONS);
        filter.addAction(ACTION_LOGTOFILE);
        filter.addAction(ACTION_CLEAR);
        filter.addAction(ACTION_WEATHER);
        filter.addAction(ACTION_WEATHER_GOT_LOCATION);
        filter.addAction(ACTION_WEATHER_DELAY);
        filter.addAction(ACTION_WEATHER_MANUAL_UPDATE);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(ACTION_UPDATE_SUGGESTIONS)) {
                    if (suggestionsManager != null)
                        suggestionsManager.requestSuggestion("");
                } else if (action.equals(ACTION_UPDATE_HINT)) {
                    terminalManager.setDefaultHint();
                } else if (action.equals(ACTION_ROOT)) {
                    terminalManager.onRoot();
                } else if (action.equals(ACTION_NOROOT)) {
                    terminalManager.onStandard();
//                } else if(action.equals(ACTION_CLEAR_SUGGESTIONS)) {
//                    if(suggestionsManager != null) suggestionsManager.clear();
                } else if (action.equals(ACTION_LOGTOFILE)) {
                    String fileName = intent.getStringExtra(FILE_NAME);
                    if (fileName == null || fileName.contains(File.separator)) return;

                    File file = new File(getFolder(), fileName);
                    if (file.exists()) file.delete();

                    try {
                        file.createNewFile();

                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(terminalManager.getTerminalText().getBytes());

                        sendOutput(context, "Logged to " + file.getAbsolutePath());
                    } catch (Exception e) {
                        sendOutput(Color.RED, context, e.toString());
                    }
                } else if (action.equals(ACTION_CLEAR)) {
                    terminalManager.clear();
                    if (suggestionsManager != null)
                        suggestionsManager.requestSuggestion("");
                } else if (action.equals(ACTION_WEATHER)) {
                    Calendar c = Calendar.getInstance();

                    CharSequence s = intent.getCharSequenceExtra(SettingsManager.VALUE_ATTRIBUTE);
                    if (s == null) s = intent.getStringExtra(SettingsManager.VALUE_ATTRIBUTE);
                    if (s == null) return;

                    s = span(context, s, weatherColor, labelSizes[Label.weather.ordinal()]);

                    updateText(Label.weather, s);

                    if (showWeatherUpdate) {
                        String message = context.getString(R.string.weather_updated) + " " + c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE) + " " + "(" + lastLatitude + ", " + lastLongitude + ")";
                        sendOutput(context, message, TerminalManager.CATEGORY_OUTPUT);
                    }
                } else if (action.equals(ACTION_WEATHER_GOT_LOCATION)) {
//                    int result = intent.getIntExtra(XMLPrefsManager.VALUE_ATTRIBUTE, 0);
//                    if(result == PackageManager.PERMISSION_DENIED) {
//                        updateText(Label.weather, span(context, context.getString(R.string.location_error), weatherColor, labelSizes[Label.weather.ordinal()]));
//                    } else handler.post(weatherRunnable);

                    if (intent.getBooleanExtra(TuiLocationManager.FAIL, false)) {
                        handler.removeCallbacks(weatherRunnable);
                        //weatherRunnable = null;

                        CharSequence s = span(context, context.getString(R.string.location_error), weatherColor, labelSizes[Label.weather.ordinal()]);

                        updateText(Label.weather, s);
                    } else {
                        lastLatitude = intent.getDoubleExtra(TuiLocationManager.LATITUDE, 0);
                        lastLongitude = intent.getDoubleExtra(TuiLocationManager.LONGITUDE, 0);

                        location = locationName(context, lastLatitude, lastLongitude);

                        //if(!weatherPerformedStartupRun || XMLPrefsManager.wasChanged(Behavior.weather_key, false)) {
                        handler.removeCallbacks(weatherRunnable);
                        handler.post(weatherRunnable);
                        //}
                    }
                } else if (action.equals(ACTION_WEATHER_DELAY)) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(System.currentTimeMillis() + 1000 * 10);

                    if (showWeatherUpdate) {
                        String message = context.getString(R.string.weather_error) + " " + c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE);
                        sendOutput(context, message, TerminalManager.CATEGORY_OUTPUT);
                    }

                    handler.removeCallbacks(weatherRunnable);
                    handler.postDelayed(weatherRunnable, 1000 * 60);
                } else if (action.equals(ACTION_WEATHER_MANUAL_UPDATE)) {
                    handler.removeCallbacks(weatherRunnable);
                    handler.post(weatherRunnable);
                }
            }
        };

        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);

        deviceInfoManager = new DeviceInfoManager();

        policy = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        component = new ComponentName(context, PolicyReceiver.class);

        this.context = context;

        disposableSet = new ArraySet<>();

        handler = new Handler();

        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        SettingsManager settingsManager = SettingsManager.getInstance();

        Single<View> ioViewSingle = settingsManager.requestUpdates(Ui.input_bottom, boolean.class)
                .firstOrError()
                .observeOn(AndroidSchedulers.mainThread())
                .map(bottom -> bottom ? R.layout.input_down_layout : R.layout.input_up_layout)
                .map(layoutId -> {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    return inflater.inflate(layoutId, null);
                });

        // make sure that all the views are initialized
        Single<List<View>> initViewsSingle = ioViewSingle
                .observeOn(AndroidSchedulers.mainThread())
                // assign a tag to recognize the view index with ease
                .doOnSuccess(ioView -> {
                    terminalView = ioView.findViewById(R.id.terminal_view);
                    terminalView.setTag(TERMINAL_VIEW_INDEX);

                    inputView = ioView.findViewById(R.id.input_view);
                    terminalView.setTag(INPUT_VIEW_INDEX);

                    prefixView = ioView.findViewById(R.id.prefix_view);
                    terminalView.setTag(PREFIX_VIEW_INDEX);

                    submitView = ioView.findViewById(R.id.submit_tv);
                    terminalView.setTag(SUBMIT_VIEW_INDEX);

                    backView = ioView.findViewById(R.id.back_view);
                    terminalView.setTag(BACK_VIEW_INDEX);

                    nextView = ioView.findViewById(R.id.next_view);
                    terminalView.setTag(NEXT_VIEW_INDEX);

                    deleteView = ioView.findViewById(R.id.delete_view);
                    terminalView.setTag(DELETE_VIEW_INDEX);

                    pasteView = ioView.findViewById(R.id.paste_view);
                    terminalView.setTag(PASTE_VIEW_INDEX);

                    // input group contains the input area AND the toolbar
                    inputGroup = ioView.findViewById(R.id.input_group);
                    terminalView.setTag(INPUT_GROUP_INDEX);

                    toolbarView = ioView.findViewById(R.id.tools_view);
                    terminalView.setTag(TOOLBAR_VIEW_INDEX);
                })
                .flatMapObservable(ioView -> Observable.fromArray(
                        terminalView,
                        inputView,
                        prefixView,
                        submitView,
                        backView,
                        nextView,
                        deleteView,
                        pasteView,
                        inputGroup,
                        toolbarView
                        )
                )
                .toList();

        disposableSet.add(
                ioViewSingle.subscribe(ioView -> rootView.addView(ioView))
        );

        // holds the observables which keep info about the margins/padding
        Observable<Float[]>[] marginsPaddingObservables = new Observable[VIEWS_LENGTH];

        // todo: manage the case when there are less than 4 numbers
        marginsPaddingObservables[DEVICE_INFO_INDEX] = settingsManager.requestList(Ui.status_lines_margins, Float.class, 0f);
        marginsPaddingObservables[TERMINAL_VIEW_INDEX] = settingsManager.requestList(Ui.output_field_margins, float.class, 0f);
        marginsPaddingObservables[INPUT_GROUP_INDEX] = settingsManager.requestList(Ui.input_area_margins, float.class, 0f);
        marginsPaddingObservables[INPUT_VIEW_INDEX] = settingsManager.requestList(Ui.input_field_margins, float.class, 0f);
        marginsPaddingObservables[PREFIX_VIEW_INDEX] = settingsManager.requestList(Ui.input_field_margins, float.class, 0f);
        marginsPaddingObservables[TOOLBAR_VIEW_INDEX] = settingsManager.requestList(Ui.toolbar_margins, float.class, 0f);
        marginsPaddingObservables[SUGGESTIONS_GROUP_INDEX] = settingsManager.requestList(Ui.suggestions_area_margin, float.class, 0f);

        // holds the observables which keep info about the bg rect colors
        Observable<Color>[] bgRectColorsObservables = new Observable[VIEWS_LENGTH];
        bgRectColorsObservables[INPUT_VIEW_INDEX] = settingsManager.requestUpdates(Theme.input_bgrectcolor, Color.class);
        bgRectColorsObservables[TERMINAL_VIEW_INDEX] = settingsManager.requestUpdates(Theme.output_bgrectcolor, Color.class);
        bgRectColorsObservables[SUGGESTIONS_GROUP_INDEX] = settingsManager.requestUpdates(Theme.suggestions_bgrectcolor, Color.class);
        bgRectColorsObservables[TOOLBAR_VIEW_INDEX] = settingsManager.requestUpdates(Theme.toolbar_bgrectcolor, Color.class);

        // holds the observables which keep info about the bg colors
        Observable<Color>[] bgColorsObservables = new Observable[VIEWS_LENGTH];
        bgColorsObservables[INPUT_VIEW_INDEX] = settingsManager.requestUpdates(Theme.input_bg, Color.class);
        bgColorsObservables[TERMINAL_VIEW_INDEX] = settingsManager.requestUpdates(Theme.input_bg, Color.class);
        bgColorsObservables[SUGGESTIONS_GROUP_INDEX] = settingsManager.requestUpdates(Theme.input_bg, Color.class);
        bgColorsObservables[TOOLBAR_VIEW_INDEX] = settingsManager.requestUpdates(Theme.input_bg, Color.class);

        // shadowXOffset : 0
        // shadowYOffset : 1
        // shadowRadius  : 2
        Observable<Float[]> shadowObservable = settingsManager.requestList(Ui.shadow_params, float.class, 0f);

        // strokeWidth  : 0
        // cornerRadius : 1
        Observable<Float[]> strokeObservable = settingsManager.requestList(Ui.bgrect_params, float.class, 0f);

        Observable<Color>[] shadowColorsObservables = new Observable[VIEWS_LENGTH];
        shadowColorsObservables[INPUT_VIEW_INDEX] = settingsManager.requestUpdates(Theme.input_shadow_color, Color.class);
        shadowColorsObservables[PREFIX_VIEW_INDEX] = settingsManager.requestUpdates(Theme.input_shadow_color, Color.class);
        shadowColorsObservables[TERMINAL_VIEW_INDEX] = settingsManager.requestUpdates(Theme.output_shadow_color, Color.class);

        // wallpaper
        Observable<Integer> bgColor = settingsManager.requestUpdates(Theme.bg_color, Integer.class);
        // for some reason we aren't able to apply the theme which shows the system wallpaper
        if (!canApplyTheme) {
            disposableSet.add(
                    bgColor.observeOn(AndroidSchedulers.mainThread())
                            .subscribe(rootView::setBackgroundColor)
            );
        } else {
            // we're able to show the system wp, so we need to check
            // 1) if the user wants the system wp
            Observable<Boolean> showSystemWp = settingsManager.requestUpdates(Ui.system_wallpaper, Boolean.class);
            // 2) if the user wants an overlay above the system wp
            Observable<Integer> overlayColor = settingsManager.requestUpdates(Theme.overlay_color, Integer.class);

            disposableSet.add(
                    Observable.combineLatest(showSystemWp, bgColor, overlayColor, (showWp, bg, overlay) -> showWp ? overlay : bg)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(rootView::setBackgroundColor)
            );
        }

        // fix auto-scroll
        disposableSet.add(
                settingsManager.requestUpdates(Behavior.auto_scroll, Boolean.class)
                        // autoScroll == true
                        .filter(identityPredicate)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(autoScroll -> rootView.getViewTreeObserver().addOnGlobalLayoutListener(() ->
                                {
                                    int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                                    if (heightDiff > dpToPx(context, 200)) { // if more than 200 dp, it's probably a keyboard...
                                        if (terminalManager != null) terminalManager.scrollToEnd();
                                    }
                                })
                        )
        );

        // clear on lock
        disposableSet.add(
                settingsManager.requestUpdates(Behavior.clear_on_lock, Boolean.class)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(clLock -> clearOnLock = clLock)
        );

        // lock on db-tap / db-tap command
        Observable<Boolean> dbTapLock = settingsManager.requestUpdates(Behavior.double_tap_lock, Boolean.class);
        Observable<String> dbTapCmd = settingsManager.requestUpdates(Behavior.double_tap_cmd, String.class);
        disposableSet.add(
                Observable.combineLatest(dbTapLock, dbTapCmd, identity2())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(bipack -> {
                            lockOnDoubleTap = bipack.object1;
                            doubleTapCmd = bipack.object2;
                        })
                        .subscribe(bipack -> {
                            if (bipack.object1 == null && bipack.object2 == null) {
                                policy = null;
                                component = null;
                                gestureDetector = null;
                            } else {
                                gestureDetector = new GestureDetectorCompat(context, new GestureDetector.OnGestureListener() {
                                    @Override
                                    public boolean onDown(MotionEvent e) {
                                        return false;
                                    }

                                    @Override
                                    public void onShowPress(MotionEvent e) {
                                    }

                                    @Override
                                    public boolean onSingleTapUp(MotionEvent e) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                                        return false;
                                    }

                                    @Override
                                    public void onLongPress(MotionEvent e) {
                                    }

                                    @Override
                                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                                        return false;
                                    }
                                });
                                gestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {

                                    @Override
                                    public boolean onSingleTapConfirmed(MotionEvent e) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onDoubleTapEvent(MotionEvent e) {
                                        return true;
                                    }

                                    @Override
                                    public boolean onDoubleTap(MotionEvent e) {
                                        if (doubleTapCmd != null && doubleTapCmd.length() > 0) {
                                            String input = terminalManager.getInput();
                                            terminalManager.setInput(doubleTapCmd);
                                            terminalManager.simulateEnter();
                                            terminalManager.setInput(input);
                                        }

                                        if (lockOnDoubleTap) {
                                            boolean admin = policy.isAdminActive(component);

                                            if (!admin) {
                                                Intent i = requestAdmin(component, context.getString(R.string.admin_permission));
                                                context.startActivity(i);
                                            } else {
                                                policy.lockNow();
                                            }
                                        }

                                        return true;
                                    }
                                });
                            }
                        })
        );

        // display margins
        disposableSet.add(
                settingsManager.requestList(Ui.display_margin_mm, Float.class, 0f, ",")
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(list -> {
                            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                            rootView.setPadding(
                                    mmToPx(metrics, list[0]), mmToPx(metrics, list[1]),
                                    mmToPx(metrics, list[2]), mmToPx(metrics, list[3])
                            );
                        })
        );

        // setup touch listeners
        disposableSet.add(
                initViewsSingle.subscribe(list -> {
                    terminalView.setOnTouchListener(UIManager.this);
                    ((View) terminalView.getParent().getParent()).setOnTouchListener(UIManager.this);
                })
        );

        // apply bg rect to every view
        disposableSet.add(
                // this observable emits couples (view, stroke)
                initViewsSingle.flatMapObservable(Observable::fromIterable)
                        .map(view -> new Tuils.Pack3<>(view, bgColorsObservables[(int) view.getTag()],
                                bgRectColorsObservables[(int) view.getTag()]))
                        .flatMap(pack3 -> Observable.combineLatest(
                                Observable.just(pack3.object1),
                                strokeObservable,
                                pack3.object2 != null ? pack3.object2 : Observable.just(Color.nullColor),
                                pack3.object3 != null ? pack3.object3 : Observable.just(Color.nullColor),
                                identity4()
                        ))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pack4 -> applyBgRect(
                                // view
                                pack4.object1,
                                // bgRectColor
                                pack4.object4,
                                // bgColor
                                pack4.object3,
                                // stroke
                                pack4.object2[0].intValue(), pack4.object2[1].intValue()))
        );

        // apply margins and padding to every view
        disposableSet.add(
                initViewsSingle.flatMapObservable(Observable::fromIterable)
                        .map(view -> new BiPack<>(view, marginsPaddingObservables[(int) view.getTag()]))
                        // check that the corresponding Single exists
                        .filter(bipack -> bipack.object2 != null)
                        // extract the Float[] value from the single
                        .flatMap(bipack -> Observable.combineLatest(
                                Observable.just(bipack.object1),
                                bipack.object2,
                                identity2()
                        ))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(bipack -> {
                            applyMarginsAndPadding(bipack.object1,
                                    // padding
                                    Arrays.copyOfRange(bipack.object2, 0, 2),
                                    // margins
                                    Arrays.copyOfRange(bipack.object2, 2, 4));
                        })

        );

        // apply shadow to every view
        disposableSet.add(
                initViewsSingle.flatMapObservable(Observable::fromIterable)
                        .filter(view -> view instanceof TextView)
                        .map(view -> new BiPack<>(view, shadowColorsObservables[(int) view.getTag()]))
                        // check if the corresponding Single exists
                        .filter(bipack -> bipack.object2 != null)
                        // extract the Color value from the single
                        .flatMap(bipack -> Observable.combineLatest(
                                Observable.just(bipack.object1),
                                shadowObservable,
                                bipack.object2,
                                identity3()
                        ))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pack3 -> applyShadow((TextView) pack3.object1, pack3.object3, pack3.object2))

        );

        // hide/show the submit button
        disposableSet.add(
                Observable.combineLatest(initViewsSingle.toObservable(),
                        settingsManager.requestUpdates(Ui.show_enter_button, Boolean.class),
                        (list, showSubmit) -> showSubmit)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(showSubmit -> submitView.setVisibility(showSubmit ? View.VISIBLE : View.GONE))
        );

//        toolbar
        Observable<Boolean> showToolbarObservable = settingsManager.requestUpdates(Toolbar.show_toolbar, Boolean.class);

        // hide/show toolbar
        disposableSet.add(
                Observable.combineLatest(initViewsSingle.toObservable(), showToolbarObservable, (list, showToolbar)
                        -> showToolbar)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(showToolbar -> {
                                    if (showToolbar) {
                                        toolbarView.setVisibility(View.VISIBLE);

                                        if (hideToolbarNoInput) {
                                            inputView.addTextChangedListener(new SuggestionTextWatcher(suggestionsManager, (currentText, before) -> {

                                                if (currentText.length() == 0)
                                                    toolbarView.setVisibility(View.GONE);
                                                else if (before == 0)
                                                    toolbarView.setVisibility(View.VISIBLE);
                                            }));
                                        }
                                    } else toolbarView.setVisibility(View.GONE);
                                }
                        )
        );

        // sets the variable which decides whether the toolbar should be hidden when the input field is empty
        disposableSet.add(
                settingsManager.requestUpdates(Toolbar.hide_toolbar_no_input, Boolean.class)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(hide -> hideToolbarNoInput = hide)
        );

        terminalManager = new TerminalManager(initViewsSingle, context, mainPack, executer);

        // style suggestion container, regardless of its visibility
        HorizontalScrollView sv = rootView.findViewById(R.id.suggestions_container);
        sv.setFocusable(false);
        sv.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.clearFocus();
            }
        });

        disposableSet.add(
                settingsManager.requestUpdates(Suggestions.show_suggestions, Boolean.class)
                        .subscribe(show -> {
                            if (show) {
                                LinearLayout suggestionsView = rootView.findViewById(R.id.suggestions_group);
                                suggestionsManager = new SuggestionsManager(suggestionsView, mainPack, terminalManager);
                            } else {
                                suggestionsManager = null;
                                rootView.findViewById(R.id.suggestions_group).setVisibility(View.GONE);
                            }
                        })
        );

        // todo: improve
        int drawTimes = SettingsManager.getInt(Ui.text_redraw_times);
        if (drawTimes <= 0) drawTimes = 1;
        OutlineTextView.redrawTimes = drawTimes;
    }

    //    0 = ext hor
    //    1 = ext ver
    //    2 = int hor
    //    3 = int ver
    private static void applyBgRect(View v, Color bgRectColor, Color bgColor, int strokeWidth, int cornerRadius) {
        if (bgColor == Color.nullColor && bgRectColor == Color.nullColor) return;

        try {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.RECTANGLE);
            d.setCornerRadius(cornerRadius);

            if (bgRectColor != Color.nullColor) d.setStroke(strokeWidth, bgRectColor.intValue);
            if (bgColor != Color.nullColor) d.setColor(bgColor.intValue);

            v.setBackgroundDrawable(d);
        } catch (Exception e) {
            toFile(e);
            log(e);
        }
    }

    // padding[left/right, top/bottom]
    // margins[left/right, top/bottom]
    private static void applyMarginsAndPadding(View v, Float[] padding, Float[] margins) {
        v.setPadding(padding[0].intValue(), padding[1].intValue(), padding[0].intValue(), padding[1].intValue());

        ViewGroup.LayoutParams params = v.getLayoutParams();
        if (params instanceof RelativeLayout.LayoutParams) {
            ((RelativeLayout.LayoutParams) params).setMargins(margins[0].intValue(), margins[1].intValue(), margins[0].intValue(), margins[1].intValue());
        } else if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).setMargins(margins[0].intValue(), margins[1].intValue(), margins[0].intValue(), margins[1].intValue());
        }
    }

    // shadowParams[0] : xOffset
    // shadowParams[1] : yOffset
    // shadowParams[2] : radius
    private static void applyShadow(TextView v, Color color, Float[] shadowParams) {
        v.setShadowLayer(shadowParams[2], shadowParams[0], shadowParams[1], color.intValue);
        v.setTag(OutlineTextView.SHADOW_TAG);
    }

    public void dispose() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        disposableSet.dispose();
        disposableSet.clear();

        if (suggestionsManager != null) suggestionsManager.dispose();
        if (notesManager != null) notesManager.dispose(context);
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
        unregisterBatteryReceiver(context);

        cancelFont();

        unregisterLockReceiver();

        disposed = true;
    }

    public void openKeyboard() {
        terminalManager.requestInputFocus();
        imm.showSoftInput(terminalManager.getInputView(), InputMethodManager.SHOW_IMPLICIT);
//        terminalManager.scrollToEnd();
    }

    public void closeKeyboard() {
        imm.hideSoftInputFromWindow(terminalManager.getInputWindowToken(), 0);
    }

    public void onStart(boolean openKeyboardOnStart) {
        if (openKeyboardOnStart) openKeyboard();
    }

    public void setInput(String s) {
        if (s == null)
            return;

        terminalManager.setInput(s);
        terminalManager.focusInputEnd();
    }

    public void setHint(String hint) {
        terminalManager.setHint(hint);
    }

    public void resetHint() {
        terminalManager.setDefaultHint();
    }

    public void setOutputWithCategory(CharSequence s, int category) {
        terminalManager.setOutput(s, category);
    }

    public void setOutputWithColor(CharSequence output, int color) {
        terminalManager.setOutput(color, output);
    }

    public void disableSuggestions() {
        if (suggestionsManager != null) suggestionsManager.disable();
    }

    public void enableSuggestions() {
        if (suggestionsManager != null) suggestionsManager.enable();
    }

    public void onBackPressed() {
        terminalManager.onBackPressed();
    }

    public void focusTerminal() {
        terminalManager.requestInputFocus();
    }

    public void pause() {
        closeKeyboard();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return v.onTouchEvent(event);
    }

    public OnRedirectionListener buildRedirectionListener() {
        return new OnRedirectionListener() {
            @Override
            public void onRedirectionRequest(final RedirectCommand cmd) {
                ((Activity) context).runOnUiThread(() -> {
                    terminalManager.setHint(context.getString(cmd.getHint()));
                    disableSuggestions();
                });
            }

            @Override
            public void onRedirectionEnd(RedirectCommand cmd) {
                ((Activity) context).runOnUiThread(() -> {
                    terminalManager.setDefaultHint();
                    enableSuggestions();
                });
            }
        };
    }

    private void onLock() {
        if (clearOnLock) {
            terminalManager.clear();
        }
    }

}
