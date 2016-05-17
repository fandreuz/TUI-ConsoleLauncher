package ohi.andre.consolelauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.managers.PreferencesManager;
import ohi.andre.consolelauncher.managers.SkinManager;
import ohi.andre.consolelauncher.managers.SuggestionsManager;
import ohi.andre.consolelauncher.tuils.TerminalAdapter;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.OnNewInputListener;
import ohi.andre.consolelauncher.tuils.interfaces.SuggestionViewDecorer;
import ohi.andre.consolelauncher.tuils.stuff.TrashInterfaces;

public class UIManager implements OnTouchListener {

    private final int RAM_DELAY = 3000;
    public Handler handler;

    protected Context mContext;
    protected LinearLayout suggestionsView;

    private DevicePolicyManager policy;
    private ComponentName component;
    private GestureDetector det;
    private ExecInfo info;

    private InputMethodManager imm;

    private CommandExecuter trigger;

    private TerminalAdapter mTerminalAdapter;

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

    //    suggestions stuff
    private SuggestionViewDecorer suggestionViewDecorer;
    private LinearLayout.LayoutParams suggestionViewParams;
    private Thread lastSuggestionThread;
    //    input looker for suggestions
    protected TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int st, int b, int c) {
            if (suggestionsView == null)
                return;

            if (s.length() == 0)
                return;

            String text = s.toString();
            int lastSpace = text.lastIndexOf(" ");

            String lastWord = text.substring(lastSpace != -1 ? lastSpace + 1 : 0);
            String before = text.substring(0, lastSpace != -1 ? lastSpace + 1 : 0);

            requestSuggestion(before, lastWord);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
    //    clicklistener for suggestions
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String suggestedText = ((TextView) v).getText().toString();

            String inputText = mTerminalAdapter.getInput();

            int lastSpace = inputText.lastIndexOf(" ");
            String lastWord = inputText.substring(lastSpace != -1 ? lastSpace + 1 : 0);
            String before = inputText.substring(0, lastSpace + 1);

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
                builder.append(before);
                builder.append(suggestedText);
            }

            mTerminalAdapter.setInput(builder.toString());

            mTerminalAdapter.focusInputEnd();
        }
    };

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
        final SkinManager mSkin = new SkinManager(prefsMgr, lucidaConsole);

        if (!mSkin.getUseSystemWp())
            rootView.setBackgroundColor(mSkin.getBgColor());

        ram = (TextView) rootView.findViewById(R.id.ram_tv);
        TextView deviceInfo = (TextView) rootView.findViewById(R.id.deviceinfo_tv);
        boolean showRam = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.SHOWRAM));
        if (showRam) {
            ram.setTextColor(mSkin.getRamColor());
            ram.setTextSize(mSkin.getRamSize());
            ram.setTypeface(mSkin.getGlobalTypeface());

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
            deviceInfo.setTextColor(mSkin.getDeviceColor());
            deviceInfo.setTextSize(mSkin.getDeviceSize());

            deviceInfo.setTypeface(mSkin.getGlobalTypeface());
        } else {
            deviceInfo.setVisibility(View.GONE);
            deviceInfo = null;
        }

        boolean inputBottom = Boolean.parseBoolean(prefsMgr.getValue(PreferencesManager.INPUTFIELD_BOTTOM));
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

        if (mSkin.getShowSuggestions()) {
            suggestionsView = (LinearLayout) rootView.findViewById(R.id.suggestions_group);
            inputView.addTextChangedListener(textWatcher);
            this.suggestionViewDecorer = new SuggestionViewDecorer() {
                @Override
                public TextView getSuggestionView(Context context) {
                    TextView textView = new TextView(mContext);
                    textView.setOnClickListener(clickListener);

                    textView.setTypeface(mSkin.getGlobalTypeface());

                    textView.setTextSize(mSkin.getSuggestionSize());
                    textView.setTextColor(mSkin.getSuggestionColor());
                    textView.setPadding(SkinManager.SUGGESTION_PADDING_HORIZONTAL, SkinManager.SUGGESTION_PADDING_VERTICAL,
                            SkinManager.SUGGESTION_PADDING_HORIZONTAL, SkinManager.SUGGESTION_PADDING_VERTICAL);
                    if (mSkin.getTransparentSuggestions())
                        textView.setBackgroundColor(Color.TRANSPARENT);
                    else
                        textView.setBackgroundColor(mSkin.getSuggestionBg());

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
        } else
            initDetector();

        mTerminalAdapter = new TerminalAdapter(terminalView, inputView, prefixView, submitView, mSkin, getHint(prefsMgr));
        mTerminalAdapter.setInputListener(new OnNewInputListener() {
            @Override
            public void onNewInput(String input) {
                trigger.exec(input, mTerminalAdapter.getCurrentOutputId());
            }
        });

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
                parent.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, terminalHeight));
            }
        });
    }

    //    call this to trigger a suggestion change
    private void requestSuggestion(final String before, final String lastWord) {

        if (suggestionViewParams == null) {
            suggestionViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            suggestionViewParams.setMargins(SkinManager.SUGGESTION_MARGIN, 0, SkinManager.SUGGESTION_MARGIN, 0);
            suggestionViewParams.gravity = Gravity.CENTER_VERTICAL;
        }

        if (lastSuggestionThread != null && lastSuggestionThread.isAlive())
            lastSuggestionThread.interrupt();

        lastSuggestionThread = new Thread() {
            @Override
            public void run() {
                super.run();

                final String[] suggestions = SuggestionsManager.getSuggestions(info, before, lastWord);

                if (suggestions.length == 0) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            suggestionsView.removeAllViews();
                        }
                    });
                    return;
                }

                if (Thread.interrupted())
                    return;

                final TextView[] recycledViews = new TextView[suggestionsView.getChildCount()];
                for (int count = 0; count < recycledViews.length; count++)
                    recycledViews[count] = (TextView) suggestionsView.getChildAt(count);

                if (Thread.interrupted())
                    return;

                final int n = suggestions.length - recycledViews.length;
                final TextView[] toAdd;
                final TextView[] toRecycle;
                if (n == 0) {
                    toRecycle = recycledViews;
                    toAdd = null;
                } else if (n > 0) {
                    toRecycle = recycledViews;
                    toAdd = new TextView[n];
                    for (int count = 0; count < toAdd.length; count++)
                        toAdd[count] = suggestionViewDecorer.getSuggestionView(mContext);
                } else if (n < 0) {
                    toAdd = null;
                    toRecycle = new TextView[suggestions.length];
                    System.arraycopy(recycledViews, 0, toRecycle, 0, toRecycle.length);
                } else {
                    return;
                }

                if (Thread.interrupted())
                    return;

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (n < 0) {
                            for (int count = toRecycle.length; count < suggestionsView.getChildCount(); count++) {
                                suggestionsView.removeViewAt(count);
                            }
                        }

                        for (int count = 0; count < suggestions.length; count++) {
                            String s = suggestions[count];
                            if (count < toRecycle.length) {
                                toRecycle[count].setText(s);
                            } else if (toAdd != null && count < toAdd.length) {
                                toAdd[count].setText(s);
                                suggestionsView.addView(toAdd[count], suggestionViewParams);
                            }
                        }
                    }
                });
            }
        };

        lastSuggestionThread.start();
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
    }

    //   accessor for input
    public void setInput(String s) {
        if (s == null || s.length() == 0)
            return;

        mTerminalAdapter.setInput(s);
        mTerminalAdapter.focusInputEnd();
    }

    //   accessor for output
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
                boolean admin = policy.isAdminActive(component);

                if (!admin)
                    Tuils.requestAdmin((Activity) mContext, component,
                            mContext.getString(R.string.adminrequest_label));
                else
                    policy.lockNow();

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

