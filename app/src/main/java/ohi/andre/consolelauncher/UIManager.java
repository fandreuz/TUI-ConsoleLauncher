package ohi.andre.consolelauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Typeface;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.RedirectCommand;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.SkinManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionRunnable;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
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
    private MainPack info;

    private InputMethodManager imm;
    private CommandExecuter trigger;
    private TerminalManager mTerminalAdapter;

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

    private boolean showSuggestions;
    private LinearLayout suggestionsView;
    private SuggestionViewDecorer suggestionViewDecorer;
    private SuggestionRunnable suggestionRunnable;
    private LinearLayout.LayoutParams suggestionViewParams;
    private SuggestionsManager suggestionsManager;
    private boolean navigatingWithSpace = false;

    private TextView terminalView;
    private Thread lastSuggestionThread;
    private Handler activityHandler;
    private Runnable removeAllSuggestions = new Runnable() {
        @Override
        public void run() {
            suggestionsView.removeAllViews();
        }
    };

    private String doubleTapCmd;
    private boolean lockOnDbTap;

    protected TextWatcher textWatcher = new TextWatcher() {

        int nOfSpace = -1;
        String originalText;

        boolean call = true;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int st, int b, int c) {
            if (suggestionsView == null || suggestionsManager == null || !showSuggestions || !call) {
                return;
            }

//            if(st == s.length() - 1 && b == 0 && c == 1 && s.subSequence(st, s.length()).toString().equals(Tuils.SPACE) && !navigatingWithSpace) {
//                nOfSpace++;
//                originalText = s.toString();
//            } else if(!navigatingWithSpace) {
//                nOfSpace = -1;
//                originalText = null;
//                navigatingWithSpace = false;
//            }
//
//            if(nOfSpace == suggestionsView.getChildCount() + 1) {
//                nOfSpace = -2;
//                navigatingWithSpace = false;
//            }
//
//            if(nOfSpace >= 0) {
//                if(nOfSpace == 1 && suggestionsView.getChildCount() == 1) {
//                    nOfSpace = -1;
//                    originalText = null;
//                    navigatingWithSpace = false;
//                } else {
//                    for(int count = 0; count < suggestionsView.getChildCount(); count++) {
//                        SuggestionsManager.Suggestion suggestion = (SuggestionsManager.Suggestion) suggestionsView.getChildAt(count).getTag(R.id.suggestion_id);
//                        if(originalText.trim().endsWith(suggestion.text)) {
//                            nOfSpace = -1;
//                            originalText = null;
//                            navigatingWithSpace = false;
//                            break;
//                        }
//                        if(count == suggestionsView.getChildCount() - 1) return;
//                    }
//                }
//            }

            String text = s.toString();
            int lastSpace = text.lastIndexOf(Tuils.SPACE);

            String lastWord = text.substring(lastSpace != -1 ? lastSpace + 1 : 0);
            String before = text.substring(0, lastSpace != -1 ? lastSpace + 1 : 0);

            requestSuggestion(before, lastWord);
        }

        @Override
        public void afterTextChanged(Editable s) {
//            if(nOfSpace == -2) {
//                s.replace(0,s.length(),originalText);
//                originalText = null;
//                return;
//            }
//
//            if(nOfSpace > 0 && s.length() > 0 && call) {
//                if(nOfSpace == 1) {
//                    call = false;
//                    s.replace(s.length() - 1, s.length(), Tuils.EMPTYSTRING);
//                    call = true;
//                }
//
//                navigatingWithSpace = true;
//
//                call = false;
//                s.replace(s.length() - 1, s.length(), Tuils.EMPTYSTRING);
//                call = true;
//
////                int count = suggestionsView.getChildCount();
//                int index = nOfSpace - 1;
////                if(nOfSpace <= count) {
////                    index = nOfSpace - 1;
////                }
////                else {
////                    index = nOfSpace % (count + 1) - 1;
////                }
//
//                call = false;
//                if(index != -1) {
//                    View view = suggestionsView.getChildAt(index);
//                    SuggestionsManager.Suggestion suggestion = (SuggestionsManager.Suggestion) view.getTag(R.id.suggestion_id);
//
//                    String text = suggestion.getText() + Tuils.SPACE;
//
//                    if(originalText.length() < s.length() && suggestion.type == SuggestionsManager.Suggestion.TYPE_PERMANENT) {
//                        s.replace(originalText.length(), s.length(), text);
//                    }  else {
//                        s.replace(0, s.length(), text);
//                    }
//                } else {
//                    Log.e("andre", "4");
//                    s.replace(0, s.length(), originalText);
//                    navigatingWithSpace = false;
//                }
//                call = true;
//            }
        }
    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SuggestionsManager.Suggestion suggestion = (SuggestionsManager.Suggestion) v.getTag(R.id.suggestion_id);
            boolean execOnClick = suggestion.exec;

            String text = suggestion.getText();
            if(suggestion.type == SuggestionsManager.Suggestion.TYPE_PERMANENT) {
                mTerminalAdapter.setInput(mTerminalAdapter.getInput() + text);
            } else {
                mTerminalAdapter.setInput(text);
            }

            if (execOnClick) {
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

        lastSuggestionThread = new StoppableThread() {
            @Override
            public void run() {
                super.run();

                final SuggestionsManager.Suggestion[] suggestions = suggestionsManager.getSuggestions(info, before, lastWord);

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

    protected UIManager(ExecutePack info, Context context, final ViewGroup rootView, final CommandExecuter tri, DevicePolicyManager mgr, ComponentName name,
                        MainPack mainPack) {

        rootView.setOnTouchListener(this);

        policy = mgr;
        component = name;

        mContext = context;
        this.info = (MainPack) info;

        trigger = tri;

        final Typeface lucidaConsole = Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");

        imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        skinManager = new SkinManager();

        this.info.skinManager = skinManager;

        if (!skinManager.useSystemWp) {
            rootView.setBackgroundColor(skinManager.bgColor);
        } else {
            rootView.setBackgroundColor(skinManager.overlayColor);
        }

        ram = (TextView) rootView.findViewById(R.id.ram_tv);
        TextView deviceInfo = (TextView) rootView.findViewById(R.id.deviceinfo_tv);
        boolean showRam = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_ram);
        if (showRam) {
            ram.setTextColor(skinManager.ramColor);
            ram.setTextSize(skinManager.getRamSize());
            ram.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);

            memory = new ActivityManager.MemoryInfo();
            activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

            handler = new Handler();
            handler.postDelayed(ramRunnable, RAM_DELAY);
        } else {
            ram.setVisibility(View.GONE);
            ram = null;
        }

        boolean showDevice = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_device_name);
        if (showDevice) {
            String deviceName = skinManager.deviceName;

            deviceInfo.setText(deviceName);
            deviceInfo.setTextColor(skinManager.deviceColor);
            deviceInfo.setTextSize(skinManager.getDeviceSize());

            deviceInfo.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);
        } else {
            deviceInfo.setVisibility(View.GONE);
        }

        final boolean inputBottom = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.input_bottom);
        int layoutId = inputBottom ? R.layout.input_down_layout : R.layout.input_up_layout;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inputOutputView = inflater.inflate(layoutId, null);
        rootView.addView(inputOutputView);

        terminalView = (TextView) inputOutputView.findViewById(R.id.terminal_view);
        terminalView.setOnTouchListener(this);
        ((View) terminalView.getParent()).setOnTouchListener(this);

        final EditText inputView = (EditText) inputOutputView.findViewById(R.id.input_view);
        inputView.setOnTouchListener(this);

        TextView prefixView = (TextView) inputOutputView.findViewById(R.id.prefix_view);

        ImageButton submitView = (ImageButton) inputOutputView.findViewById(R.id.submit_tv);
        boolean showSubmit = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_enter_button);
        if (!showSubmit) {
            submitView.setVisibility(View.GONE);
            submitView = null;
        }

//        toolbar
        boolean showToolbar = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Toolbar.enabled);
        ImageButton backView = null;
        ImageButton nextView = null;
        ImageButton deleteView = null;
        ImageButton pasteView = null;

        if(!showToolbar) {
            inputOutputView.findViewById(R.id.tools_view).setVisibility(View.GONE);
        } else {
            backView = (ImageButton) inputOutputView.findViewById(R.id.back_view);
            nextView = (ImageButton) inputOutputView.findViewById(R.id.next_view);
            deleteView = (ImageButton) inputOutputView.findViewById(R.id.delete_view);
            pasteView = (ImageButton) inputOutputView.findViewById(R.id.paste_view);
        }

        if (skinManager.showSuggestions) {
            showSuggestions = true;

            HorizontalScrollView sv = (HorizontalScrollView) rootView.findViewById(R.id.suggestions_container);
            sv.setFocusable(false);
            sv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus) {
                        v.clearFocus();
                    }
                }
            });

            suggestionsView = (LinearLayout) rootView.findViewById(R.id.suggestions_group);

            inputView.addTextChangedListener(textWatcher);

            suggestionsManager = new SuggestionsManager();

            this.suggestionViewDecorer = new SuggestionViewDecorer() {
                @Override
                public TextView getSuggestionView(Context context) {
                    TextView textView = new TextView(mContext);
                    textView.setOnClickListener(clickListener);

                    textView.setFocusable(false);
                    textView.setLongClickable(false);
                    textView.setClickable(true);

                    textView.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);
                    textView.setTextSize(skinManager.getSuggestionSize());

                    textView.setPadding(SkinManager.SUGGESTION_PADDING_HORIZONTAL, SkinManager.SUGGESTION_PADDING_VERTICAL,
                            SkinManager.SUGGESTION_PADDING_HORIZONTAL, SkinManager.SUGGESTION_PADDING_VERTICAL);

                    textView.setLines(1);
                    textView.setMaxLines(1);
                    return textView;
                }
            };
        } else {
            showSuggestions = false;
            rootView.findViewById(R.id.suggestions_group).setVisibility(View.GONE);
            this.textWatcher = null;
            this.clickListener = null;
        }

        lockOnDbTap = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.double_tap_closes);
        doubleTapCmd = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.double_tap_cmd);
        if(!lockOnDbTap && doubleTapCmd == null) {
            policy = null;
            component = null;
            det = null;
        } else initDetector();

        mTerminalAdapter = new TerminalManager(terminalView, inputView, prefixView, submitView, backView, nextView, deleteView, pasteView, skinManager, context, mainPack);
        mTerminalAdapter.setInputListener(new OnNewInputListener() {
            @Override
            public void onNewInput(String input) {
                if(suggestionsView != null) {
                    suggestionsView.removeAllViews();
                }
                trigger.exec(input, null);
            }
        });
        if(XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.donation_message)) {
            mTerminalAdapter.addMessager(new TerminalManager.Messager(20, context.getString(R.string.rate_donate_text)));
        }
    }

    public void openKeyboard() {
        mTerminalAdapter.requestInputFocus();
        imm.showSoftInput(mTerminalAdapter.getInputView(), InputMethodManager.SHOW_IMPLICIT);
        mTerminalAdapter.scrollToEnd();
    }

    public void closeKeyboard() {
        imm.hideSoftInputFromWindow(mTerminalAdapter.getInputWindowToken(), 0);
    }

    public void onStart() {
        openKeyboard();
        mTerminalAdapter.scrollToEnd();
    }

    public void setInput(String s) {
        if (s == null)
            return;

        mTerminalAdapter.setInput(s);
        mTerminalAdapter.focusInputEnd();
    }

    public void setHint(String hint) {
        mTerminalAdapter.setHint(hint);
    }

    public void resetHint() {
        mTerminalAdapter.setDefaultHint();
    }

    public void setOutput(String string, boolean fromUser) {
        mTerminalAdapter.setOutput(string, fromUser);
    }

    public void setOutput(String s, int color, boolean fromUser) {
        mTerminalAdapter.setOutput(s, color, fromUser);
    }

    public void disableSuggestions() {
        if(suggestionsView != null) {
            showSuggestions = false;
            suggestionsView.setVisibility(View.GONE);
        }
    }

    public void enableSuggestions() {
        if(suggestionsView != null) {
            showSuggestions = true;
            suggestionsView.setVisibility(View.VISIBLE);
        }
    }

    public void onBackPressed() {
        mTerminalAdapter.onBackPressed();
    }

    //    update ram
    public void updateRamDetails() {
        ram.setText("free RAM: " + Tuils.ramDetails(activityManager, memory));
    }

    public void focusTerminal() {
        mTerminalAdapter.requestInputFocus();
    }

    public void scrollToEnd() {
        mTerminalAdapter.scrollToEnd();
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
                if(doubleTapCmd != null && doubleTapCmd.length() > 0) {
                    String input = mTerminalAdapter.getInput();
                    mTerminalAdapter.setInput(doubleTapCmd);
                    mTerminalAdapter.simulateEnter();
                    mTerminalAdapter.setInput(input);
                }

                if(lockOnDbTap) {
                    boolean admin = policy.isAdminActive(component);

                    if (!admin) {
                        Tuils.requestAdmin((Activity) mContext, component, mContext.getString(R.string.adminrequest_label));
                    } else {
                        policy.lockNow();
                    }
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

    public OnRedirectionListener buildRedirectionListener() {
        return new OnRedirectionListener() {
            @Override
            public void onRedirectionRequest(final RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTerminalAdapter.setHint(mContext.getString(cmd.getHint()));
                        disableSuggestions();
                    }
                });
            }

            @Override
            public void onRedirectionEnd(RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTerminalAdapter.setDefaultHint();
                        enableSuggestions();
                    }
                });
            }
        };
    }

    public interface SuggestionNavigator {
        boolean isNavigating();
        void onEnter();
    }

    public interface OnNewInputListener {
        void onNewInput(String input);
    }
}

