package ohi.andre.consolelauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.managers.MusicManager;
import ohi.andre.consolelauncher.managers.PreferencesManager;
import ohi.andre.consolelauncher.managers.SkinManager;
import ohi.andre.consolelauncher.managers.SuggestionsManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.tuils.ShellUtils;
import ohi.andre.consolelauncher.tuils.SuggestionRunnable;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.OnNewInputListener;
import ohi.andre.consolelauncher.tuils.interfaces.SuggestionViewDecorer;
import ohi.andre.consolelauncher.tuils.stuff.TrashInterfaces;

public class UIManager implements OnTouchListener {

    private final int RAM_DELAY = 3000;
    public Handler handler;

    protected Context mContext;

    private SkinManager skinManager;

    private DevicePolicyManager policy;
    private ComponentName component;
    private GestureDetector det;
    private ExecInfo info;

    private InputMethodManager imm;
    private CommandExecuter trigger;
    private TerminalManager mTerminalAdapter;

    private int lastMeasuredHeight;

    private TextView ram;

    private ActivityManager.MemoryInfo memory;
    private ActivityManager activityManager;
    private Runnable ramRunnable = new Runnable() {
        @Override
        public void run() {
            if (handler != null) {
                updateRamDetails();
                handler.postDelayed(this, RAM_DELAY);
            }
        }
    };

    private LinearLayout suggestionsView;
    private SuggestionViewDecorer suggestionViewDecorer;
    private LinearLayout.LayoutParams suggestionViewParams;
    private Thread lastSuggestionThread;
    private SuggestionRunnable suggestionRunnable;
    private Handler activityHandler;
    private Runnable removeAllSuggestions = new Runnable() {
        @Override
        public void run() {
            suggestionsView.removeAllViews();
        }
    };

    boolean doubleTapSU = false;

    protected TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int st, int b, int c) {
            if (suggestionsView == null)
                return;

            String text = s.toString();
            int lastSpace = text.lastIndexOf(Tuils.SPACE);

            String lastWord = text.substring(lastSpace != -1 ? lastSpace + 1 : 0);
            String before = text.substring(0, lastSpace != -1 ? lastSpace + 1 : 0);

            requestSuggestion(before, lastWord);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private boolean executeOnSuggestionClick;
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String suggestedText = ((TextView) v).getText().toString();

            String inputText = mTerminalAdapter.getInput();

            int lastSpace = inputText.lastIndexOf(Tuils.SPACE);
            String lastWord = inputText.substring(lastSpace != -1 ? lastSpace + 1 : 0);
            String before = inputText.substring(0, lastSpace + 1);

            boolean execOnClick = (boolean) v.getTag(R.id.exec_on_click_id);
            int suggestionType = (int) v.getTag(R.id.suggestion_type_id);

            StringBuilder builder = new StringBuilder();
            if (suggestedText.equals(File.separator)) {
                builder.append(before);
                builder.append(lastWord);
                builder.append(suggestedText);
            } else if (lastWord.contains(File.separator)) {
                int lastSlashIndex = inputText.lastIndexOf(File.separator);
                before = inputText.substring(0, lastSlashIndex + 1);
                builder.append(before);
                builder.append(suggestedText);
            } else {
                if(!suggestedText.contains(Tuils.SPACE)) {
                    builder.append(before);
                    builder.append(suggestedText);
                } else {
                    String[] suggestParts = suggestedText.split(Tuils.SPACE);
                    String[] inputParts = inputText.split(Tuils.SPACE);

                    boolean useScrollCompare;
                    int minRate;
                    switch (suggestionType) {
                        case SuggestionsManager.Suggestion.TYPE_APP:
                            useScrollCompare = AppsManager.USE_SCROLL_COMPARE;
                            minRate = SuggestionsManager.MIN_APPS_RATE;
                            break;
                        case SuggestionsManager.Suggestion.TYPE_SONG:
                            useScrollCompare = MusicManager.USE_SCROLL_COMPARE;
                            minRate = SuggestionsManager.MIN_SONGS_RATE;
                            break;
                        case SuggestionsManager.Suggestion.TYPE_CONTACT:
                            useScrollCompare = ContactManager.USE_SCROLL_COMPARE;
                            minRate = SuggestionsManager.MIN_CONTACTS_RATE;
                            break;
                        case SuggestionsManager.Suggestion.TYPE_FILE:
                            useScrollCompare = FileManager.USE_SCROLL_COMPARE;
                            minRate = SuggestionsManager.MIN_FILE_RATE;
                            break;
                        default:
                            builder.append(before);
                            builder.append(suggestedText);
                            return;
                    }

                    int count;
                    for(count = 0; count < inputParts.length; count++) {
                        int rate = useScrollCompare ? Compare.scrollComparison(inputParts[count], suggestParts[0]) :
                                Compare.linearComparison(inputParts[count], suggestParts[0]);

                        if(rate >= minRate) {
                            break;
                        }
                    }

                    List<String> finalText = new ArrayList<>(Arrays.asList(inputParts));
                    for(int c = 0; c < suggestParts.length; c++) {
                        if(finalText.size() > c + count) {
                            finalText.set(c + count, suggestParts[c]);
                        } else {
                            finalText.add(suggestParts[c]);
                        }
                    }

                    builder.append(Tuils.toPlanString(finalText, Tuils.SPACE));
                }
            }

            mTerminalAdapter.setInput(builder.toString());

            if (executeOnSuggestionClick && execOnClick) {
                mTerminalAdapter.simulateEnter();
            } else {
                mTerminalAdapter.focusInputEnd();
            }
        }
    };

    private void requestSuggestion(final String before, final String lastWord) {

        if (suggestionViewParams == null) {
            suggestionViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            suggestionViewParams.setMargins(SkinManager.SUGGESTION_MARGIN, 0, SkinManager.SUGGESTION_MARGIN, 0);
            suggestionViewParams.gravity = Gravity.CENTER_VERTICAL;
        }

        if(suggestionRunnable == null) {
            suggestionRunnable = new SuggestionRunnable(skinManager, suggestionsView, suggestionViewParams, (HorizontalScrollView) suggestionsView.getParent());
        }

        if(activityHandler == null) {
            Field field;
            try {
                field = mContext.getClass().getSuperclass().getDeclaredField("mHandler");
                field.setAccessible(true);
                activityHandler = (Handler) field.get(mContext);
            }
            catch (Exception e) {}
        }

        if (lastSuggestionThread != null) {
            lastSuggestionThread.interrupt();
            suggestionRunnable.interrupt();
            if(activityHandler != null) {
                activityHandler.removeCallbacks(suggestionRunnable);
            }
        }

        lastSuggestionThread = new Thread() {
            @Override
            public void run() {
                super.run();

                final SuggestionsManager.Suggestion[] suggestions = SuggestionsManager.getSuggestions(info, before, lastWord);

                if(suggestions.length == 0) {
                    ((Activity) mContext).runOnUiThread(removeAllSuggestions);
                    return;
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                final TextView[] existingViews = new TextView[suggestionsView.getChildCount()];
                for (int count = 0; count < existingViews.length; count++) {
                    existingViews[count] = (TextView) suggestionsView.getChildAt(count);
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                int n = suggestions.length - existingViews.length;
                TextView[] toAdd = null;
                TextView[] toRecycle = null;
                if (n == 0) {
                    toRecycle = existingViews;
                    toAdd = null;
                } else if (n > 0) {
                    toRecycle = existingViews;
                    toAdd = new TextView[n];
                    for (int count = 0; count < toAdd.length; count++) {
                        toAdd[count] = suggestionViewDecorer.getSuggestionView(mContext);
                    }
                } else if (n < 0) {
                    toAdd = null;
                    toRecycle = new TextView[suggestions.length];
                    System.arraycopy(existingViews, 0, toRecycle, 0, toRecycle.length);
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                suggestionRunnable.setN(n);
                suggestionRunnable.setSuggestions(suggestions);
                suggestionRunnable.setToAdd(toAdd);
                suggestionRunnable.setToRecycle(toRecycle);
                suggestionRunnable.reset();
                ((Activity) mContext).runOnUiThread(suggestionRunnable);
            }
        };

        lastSuggestionThread.start();
    }

    protected UIManager(ExecInfo info, Context context, final ViewGroup rootView, final CommandExecuter tri, DevicePolicyManager mgr, ComponentName name,
                        PreferencesManager prefsMgr) {

        rootView.setOnTouchListener(this);

        policy = mgr;
        component = name;

        mContext = context;
        this.info = info;

        trigger = tri;

        imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        Typeface lucidaConsole = Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
        skinManager = new SkinManager(prefsMgr, lucidaConsole);

        if (!skinManager.getUseSystemWp()) {
            rootView.setBackgroundColor(skinManager.getBgColor());
        }

        ram = (TextView) rootView.findViewById(R.id.ram_tv);
        TextView deviceInfo = (TextView) rootView.findViewById(R.id.deviceinfo_tv);
        boolean showRam = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.SHOWRAM));
        if (showRam) {
            ram.setTextColor(skinManager.getRamColor());
            ram.setTextSize(skinManager.getRamSize());
            ram.setTypeface(skinManager.getGlobalTypeface());

            memory = new ActivityManager.MemoryInfo();
            activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

            handler = new Handler();
            handler.postDelayed(ramRunnable, RAM_DELAY);
        } else {
            ram.setVisibility(View.GONE);
            ram = null;
        }

        boolean showDevice = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.SHOWDEVICE));
        if (showDevice) {
            String deviceName = getDeviceName(prefsMgr);

            deviceInfo.setText(deviceName);
            deviceInfo.setTextColor(skinManager.getDeviceColor());
            deviceInfo.setTextSize(skinManager.getDeviceSize());

            deviceInfo.setTypeface(skinManager.getGlobalTypeface());
        } else {
            deviceInfo.setVisibility(View.GONE);
            deviceInfo = null;
        }

        final boolean inputBottom = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.INPUTFIELD_BOTTOM));
        int layoutId = inputBottom ? R.layout.input_down_layout : R.layout.input_up_layout;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inputOutputView = inflater.inflate(layoutId, null);
        rootView.addView(inputOutputView);

        final TextView terminalView = (TextView) inputOutputView.findViewById(R.id.terminal_view);
        terminalView.setOnTouchListener(this);
        ((View) terminalView.getParent()).setOnTouchListener(this);

        final EditText inputView = (EditText) inputOutputView.findViewById(R.id.input_view);
        inputView.setOnTouchListener(this);

        TextView prefixView = (TextView) inputOutputView.findViewById(R.id.prefix_view);

        TextView submitView = (TextView) inputOutputView.findViewById(R.id.submit_tv);
        boolean showSubmit = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.SHOWSUBMIT));
        if (!showSubmit) {
            submitView.setVisibility(View.GONE);
            submitView = null;
        }

        if (skinManager.getShowSuggestions()) {
            executeOnSuggestionClick = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.EXECUTE_ON_SUGGESTION_CLICK));

            suggestionsView = (LinearLayout) rootView.findViewById(R.id.suggestions_group);
            inputView.addTextChangedListener(textWatcher);

            this.suggestionViewDecorer = new SuggestionViewDecorer() {
                @Override
                public TextView getSuggestionView(Context context) {
                    TextView textView = new TextView(mContext);
                    textView.setOnClickListener(clickListener);

                    textView.setTypeface(skinManager.getGlobalTypeface());
                    textView.setTextColor(skinManager.getSuggestionTextColor());
                    textView.setTextSize(skinManager.getSuggestionSize());

                    textView.setPadding(SkinManager.SUGGESTION_PADDING_HORIZONTAL, SkinManager.SUGGESTION_PADDING_VERTICAL,
                            SkinManager.SUGGESTION_PADDING_HORIZONTAL, SkinManager.SUGGESTION_PADDING_VERTICAL);

                    textView.setLines(1);
                    textView.setMaxLines(1);
                    return textView;
                }
            };
        } else {
            rootView.findViewById(R.id.suggestions_group).setVisibility(View.GONE);
            this.textWatcher = null;
            this.clickListener = null;
        }

        boolean closeOnDbTap = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.DOUBLETAP));
        if (!closeOnDbTap) {
            policy = null;
            component = null;
            det = null;
        } else {
            doubleTapSU = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.DOUBLETAP_SU));
            initDetector();
        }

        mTerminalAdapter = new TerminalManager(terminalView, inputView, prefixView, submitView, skinManager, getHint(prefsMgr), inputBottom);
        mTerminalAdapter.setInputListener(new OnNewInputListener() {
            @Override
            public void onNewInput(String input) {
                if(suggestionsView != null) {
                    suggestionsView.removeAllViews();
                }
                trigger.exec(input, mTerminalAdapter.getCurrentOutputId());
            }
        });
//        if(Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.SHOW_DONATE_MESSAGE))) {
//            mTerminalAdapter.addMessager(new TerminalManager.Messager(65, context.getString(R.string.rate_donate_text)));
//        }

        ViewTreeObserver observer = rootView.getViewTreeObserver();
        final TextView device = deviceInfo;
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootHeight = rootView.getMeasuredHeight();
                if (rootHeight != 0 && rootHeight == lastMeasuredHeight)
                    return;

                lastMeasuredHeight = rootHeight;

                int deviceHeight = device != null ? device.getMeasuredHeight() : 0;
                int ramHeight = ram != null ? ram.getMeasuredHeight() : 0;
                int inputHeight = inputView.getMeasuredHeight();
                int suggestionHeight = suggestionsView != null ? suggestionsView.getMeasuredHeight() : 0;
                int terminalHeight = rootHeight - deviceHeight - ramHeight - inputHeight - suggestionHeight;

                View parent = (View) terminalView.getParent();
                parent.setLayoutParams(inputBottom ? new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, terminalHeight) :
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, terminalHeight));
            }
        });
    }

    private void openKeyboard() {
        mTerminalAdapter.requestInputFocus();
        imm.showSoftInput(mTerminalAdapter.getInputView(), InputMethodManager.SHOW_IMPLICIT);
    }

    private void closeKeyboard() {
        imm.hideSoftInputFromWindow(mTerminalAdapter.getInputWindowToken(), 0);
    }

    public void onStart() {
        mTerminalAdapter.requestInputFocus();
        openKeyboard();
        mTerminalAdapter.scrollToEnd();
    }

    public void setInput(String s) {
        if (s == null)
            return;

        mTerminalAdapter.setInput(s);
        mTerminalAdapter.focusInputEnd();
    }

    public void setOutput(String string, int id) {
        mTerminalAdapter.setOutput(string, id);
    }

    //    get device name
    private String getDeviceName(PreferencesManager preferencesManager) {
        String name = preferencesManager.getValue(PreferencesManager.DEVICENAME);
        if (name.length() == 0 || name.equals("null"))
            return Build.MODEL;
        else
            return name;
    }

    //    update ram
    public void updateRamDetails() {
        ram.setText("RAM: " + Tuils.ramDetails(activityManager, memory));
    }

    public void focusTerminal() {
        mTerminalAdapter.requestInputFocus();
    }

    //	 get hint for input
    private String getHint(PreferencesManager preferencesManager) {
        boolean showUsername = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.SHOWUSERNAME));

        String hint = "";
        if (showUsername) {
            hint = preferencesManager.getValue(PreferencesManager.USERNAME);
            if (hint == null || hint.length() == 0) {
                String email = Tuils.getUsername(mContext);
                if (email != null) {
                    if (email.endsWith("@gmail.com"))
                        email = email.substring(0, email.length() - 10);
                    return "user@".concat(email);
                } else
                    return Tuils.getSDK();
            } else
                hint = "user@".concat(hint);
        }
        return hint;
    }

    //	 init detector for double tap
    private void initDetector() {
        det = new GestureDetector(mContext, TrashInterfaces.trashGestureListener);

        det.setOnDoubleTapListener(new OnDoubleTapListener() {

            boolean hadSU = false;

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if(doubleTapSU) {
                    hadSU = Tuils.verifyRoot();
                    doubleTapSU = hadSU;
                }

                if(hadSU) {
                    ShellUtils.execCommand("input keyevent 26", true, null);
                } else {
                    boolean admin = policy.isAdminActive(component);

                    if (!admin)
                        Tuils.requestAdmin((Activity) mContext, component, mContext.getString(R.string.adminrequest_label));
                    else
                        policy.lockNow();
                }

                return true;
            }
        });
    }

    protected boolean verifyDoubleTap(MotionEvent event) {
        return det != null && det.onTouchEvent(event);
    }

    //	 on pause
    public void pause() {
        closeKeyboard();
    }

    public void clear() {
        mTerminalAdapter.clear();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (verifyDoubleTap(event))
            return true;

        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return v.onTouchEvent(event);

        if (v.getId() == R.id.input_view) {
            openKeyboard();
            return true;
        } else
            return v.onTouchEvent(event);
    }

}

