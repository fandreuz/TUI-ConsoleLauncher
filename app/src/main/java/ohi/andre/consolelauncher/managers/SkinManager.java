package ohi.andre.consolelauncher.managers;

import android.graphics.Color;
import android.graphics.Typeface;

public class SkinManager {

    public static final int SYSTEM_WALLPAPER = -1;

    public static final int SUGGESTION_PADDING_VERTICAL = 15;
    public static final int SUGGESTION_PADDING_HORIZONTAL = 15;
    public static final int SUGGESTION_MARGIN = 20;
    //	default
    public static final int deviceDefault = 0xffff9800;
    public static final int inputDefault = 0xff00ff00;
    public static final int outputDefault = 0xffffffff;
    public static final int ramDefault = 0xfff44336;
    public static final int bgDefault = 0xff000000;
    public static final int suggestionColorDefault = 0xff000000;
    public static final int suggestionBgDefault = 0xffffffff;
    public static final int defaultSize = 15;
    private static final int deviceScale = 3;
    private static final int textScale = 2;
    private static final int ramScale = 3;
    private static final int suggestionScale = 0;
    private Typeface globalTypeface;
    private int globalFontSize;
    private int deviceColor;
    private int inputColor;
    private int outputColor;
    private int ramColor;
    private int bgColor;
    private boolean useSystemWp;
    private boolean showSuggestions;
    private int suggestionColor;
    private int suggestionBg;
    private boolean transparentSuggestions;

    public SkinManager(PreferencesManager prefs, Typeface lucidaConsole) {

        boolean systemFont = Boolean.parseBoolean(prefs.getValue(PreferencesManager.USE_SYSTEMFONT));
        globalTypeface = systemFont ? Typeface.DEFAULT : lucidaConsole;

        try {
            globalFontSize = Integer.parseInt(prefs.getValue(PreferencesManager.FONTSIZE));
        } catch (Exception e) {
            globalFontSize = SkinManager.defaultSize;
        }

        try {
            useSystemWp = Boolean.parseBoolean(prefs.getValue(PreferencesManager.USE_SYSTEMWP));
            if (useSystemWp)
                bgColor = SYSTEM_WALLPAPER;
            else
                bgColor = Color.parseColor(prefs.getValue(PreferencesManager.BG));
        } catch (Exception e) {
            bgColor = bgDefault;
        }

        try {
            deviceColor = Color.parseColor(prefs.getValue(PreferencesManager.DEVICE));
        } catch (Exception e) {
            deviceColor = deviceDefault;
        }

        try {
            inputColor = Color.parseColor(prefs.getValue(PreferencesManager.INPUT));
        } catch (Exception e) {
            inputColor = inputDefault;
        }

        try {
            outputColor = Color.parseColor(prefs.getValue(PreferencesManager.OUTPUT));
        } catch (Exception e) {
            outputColor = outputDefault;
        }

        try {
            ramColor = Color.parseColor(prefs.getValue(PreferencesManager.RAM));
        } catch (Exception e) {
            ramColor = ramDefault;
        }

        showSuggestions = Boolean.parseBoolean(prefs.getValue(PreferencesManager.SHOWSUGGESTIONS));
        if (showSuggestions) {
            try {
                suggestionColor = Color.parseColor(prefs.getValue(PreferencesManager.SUGGESTION_COLOR));
            } catch (Exception e) {
                suggestionColor = suggestionColorDefault;
            }

            try {
                suggestionBg = Color.parseColor(prefs.getValue(PreferencesManager.SUGGESTION_BG));
            } catch (Exception e) {
                suggestionBg = suggestionBgDefault;
            }

            try {
                transparentSuggestions = Boolean.parseBoolean(prefs.getValue(PreferencesManager.TRANSPARENT_SUGGESTIONS));
            } catch (Exception e) {
                transparentSuggestions = false;
            }
        }
    }

    public Typeface getGlobalTypeface() {
        return globalTypeface;
    }

    public int getGlobalFontSize() {
        return globalFontSize;
    }

    public int getDeviceColor() {
        return deviceColor;
    }

    public int getInputColor() {
        return inputColor;
    }

    public int getOutputColor() {
        return outputColor;
    }

    public int getRamColor() {
        return ramColor;
    }

    public int getBgColor() {
        return bgColor;
    }

    public boolean getUseSystemWp() {
        return useSystemWp;
    }

    public boolean getShowSuggestions() {
        return showSuggestions;
    }

    public int getSuggestionBg() {
        return suggestionBg;
    }

    public int getSuggestionColor() {
        return suggestionColor;
    }

    public boolean getTransparentSuggestions() {
        return transparentSuggestions;
    }

    public int getDeviceSize() {
        return globalFontSize - deviceScale;
    }

    public int getTextSize() {
        return globalFontSize - textScale;
    }

    public int getRamSize() {
        return globalFontSize - ramScale;
    }

    public int getSuggestionSize() {
        return globalFontSize - suggestionScale;
    }


//	public void setupSubmit(Button submit) {
//		submit.setBackgroundColor(android.R.color.transparent);
//
//		submit.setTextColor(input);
//		submit.setTextSize(fontSize + SkinManager.submitScale);
//	}
//
}
