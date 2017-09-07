package ohi.andre.consolelauncher.commands.tuixt;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandGroup;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 19/01/2017.
 */

public class TuixtActivity extends Activity {

    private final String FIRSTACCESS_KEY = "firstAccess";

    public static final int BACK_PRESSED = 2;

    private long lastEnter;

    public static String PATH = "path";

    public static String ERROR_KEY = "error";

    private EditText inputView;
    private EditText fileView;
    private TextView outputView;

    private TuixtPack pack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Typeface lucidaConsole = Typeface.createFromAsset(getAssets(), "lucida_console.ttf");
        final LinearLayout rootView = new LinearLayout(this);

        final Intent intent = getIntent();

        String path = intent.getStringExtra(PATH);
        if(path == null) {
            Uri uri = intent.getData();
            File file = new File(uri.getPath());
            path = file.getAbsolutePath();
        }

        final File file = new File(path);

        CommandGroup group = new CommandGroup(this, "ohi.andre.consolelauncher.commands.tuixt.raw");

        try {
            XMLPrefsManager.create(this);
        } catch (Exception e) {
            finish();
        }

        if (!XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.system_wallpaper)) {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(XMLPrefsManager.Theme.bg_color));
        } else {
            setTheme(R.style.Custom_SystemWP);
        }

        final boolean inputBottom = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.input_bottom);
        int layoutId = inputBottom ? R.layout.tuixt_view_input_down : R.layout.tuixt_view_input_up;

        LayoutInflater inflater = getLayoutInflater();
        View inputOutputView = inflater.inflate(layoutId, null);
        rootView.addView(inputOutputView);

        fileView = (EditText) inputOutputView.findViewById(R.id.file_view);
        inputView = (EditText) inputOutputView.findViewById(R.id.input_view);
        outputView = (TextView) inputOutputView.findViewById(R.id.output_view);

        TextView prefixView = (TextView) inputOutputView.findViewById(R.id.prefix_view);

        ImageButton submitView = (ImageButton) inputOutputView.findViewById(R.id.submit_tv);
        boolean showSubmit = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_enter_button);
        if (!showSubmit) {
            submitView.setVisibility(View.GONE);
            submitView = null;
        }

        String prefix = XMLPrefsManager.get(XMLPrefsManager.Ui.input_prefix);

        Typeface t = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.system_font) ? Typeface.DEFAULT : lucidaConsole;
        int ioSize = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.input_output_size);
        int outputColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.output_color);
        int inputColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.input_color);

        prefixView.setTypeface(t);
        prefixView.setTextColor(inputColor);
        prefixView.setTextSize(ioSize);
        prefixView.setText(prefix.endsWith(Tuils.SPACE) ? prefix : prefix + Tuils.SPACE);

        if (submitView != null) {
            submitView.setColorFilter(XMLPrefsManager.getColor(XMLPrefsManager.Theme.enter_color));
            submitView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewInput();
                }
            });
        }

        fileView.setTypeface(t);
        fileView.setTextSize(ioSize);
        fileView.setTextColor(outputColor);
        fileView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    outputView.setVisibility(View.GONE);
                    outputView.setText(Tuils.EMPTYSTRING);
                }

                return false;
            }
        });

        outputView.setTypeface(t);
        outputView.setTextSize(ioSize);
        outputView.setTextColor(outputColor);
        outputView.setMovementMethod(new ScrollingMovementMethod());
        outputView.setVisibility(View.GONE);

        inputView.setTypeface(t);
        inputView.setTextSize(ioSize);
        inputView.setTextColor(inputColor);
        inputView.setHint(Tuils.getHint(path));

        inputView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        inputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                physical enter
                if(actionId == KeyEvent.ACTION_DOWN) {
                    if(lastEnter == 0) {
                        lastEnter = System.currentTimeMillis();
                    } else {
                        long difference = System.currentTimeMillis() - lastEnter;
                        lastEnter = System.currentTimeMillis();
                        if(difference < 350) {
                            return true;
                        }
                    }
                }

                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.ACTION_DOWN) {
                    onNewInput();
                }
                return true;
            }
        });

        setContentView(rootView);

//
//
//        end setup part, now start

        pack = new TuixtPack(group, file, this, fileView);

        fileView.setText(getString(R.string.tuixt_reading) + Tuils.SPACE + path);
        new Thread() {

            @Override
            public void run() {
                super.run();

                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));

                    final StringBuilder builder = new StringBuilder();
                    String line, lastLine = null;
                    while( (line = reader.readLine()) != null) {
                        if(lastLine != null) {
                            builder.append(Tuils.NEWLINE);
                        }
                        builder.append(line);
                        lastLine = line;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                fileView.setText(builder.toString());
                            } catch (OutOfMemoryError e) {
                                fileView.setText(Tuils.EMPTYSTRING);
                                Toast.makeText(TuixtActivity.this, R.string.tuixt_error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e("andre", "", e);
                    intent.putExtra(ERROR_KEY, e.toString());
                    setResult(1, intent);
                    finish();
                }
            }
        }.start();

        SharedPreferences preferences = getPreferences(0);
        boolean firstAccess = preferences.getBoolean(FIRSTACCESS_KEY, true);
        if (firstAccess) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRSTACCESS_KEY, false);
            editor.commit();

            inputView.setText("help");
            inputView.setSelection(inputView.getText().length());
        }
    }

    @Override
    public void onBackPressed() {
        setResult(BACK_PRESSED);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();

        FileManager.writeOn(pack.editFile, fileView.getText().toString());
        this.finish();
    }

    private void onNewInput() {
        try {
            String input = inputView.getText().toString();
            inputView.setText(Tuils.EMPTYSTRING);

            input = input.trim();
            if(input.length() == 0) {
                return;
            }

            outputView.setVisibility(View.VISIBLE);

            Command command = CommandTuils.parse(input, pack, false);
            if(command == null) {
                outputView.setText(R.string.output_commandnotfound);
                return;
            }

            String output = command.exec(getResources(), pack);
            if(output != null) {
                outputView.setText(output);
            }
        } catch (Exception e) {
            outputView.setText(e.toString());
        }
    }
}
