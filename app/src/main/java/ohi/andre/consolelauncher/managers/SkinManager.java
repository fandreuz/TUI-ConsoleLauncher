package ohi.andre.consolelauncher.managers;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Parcel;
import android.os.Parcelable;

import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;

public class SkinManager implements Parcelable {

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

    public static final int suggestionTextColorDefault = 0xff000000;
    public static final int aliasSuggestionBgDefault = 0xffFF5722;
    public static final int appSuggestionBgDefault = 0xff00897B;
    public static final int commandSuggestionsBgDefault = 0xff76FF03;
    public static final int songSuggestionBgDefault = 0xffEEFF41;
    public static final int contactSuggestionBgDefault = 0xff64FFDA;
    public static final int fileSuggestionBgDeafult = 0xff03A9F4;
    public static final int defaultSuggestionBgDefault = 0xffFFFFFF;

    public static final int defaultSize = 15;
    private static final int deviceScale = 3;
    private static final int textScale = 2;
    private static final int ramScale = 3;
    private static final int suggestionScale = 0;

    private int globalFontSize;

    private int deviceColor, inputColor, outputColor, ramColor, bgColor;

    private boolean useSystemWp, showSuggestions, systemFont, inputBottom, showSubmit;

    private String username = null;
    private boolean showUsernameAndDeviceWhenEmpty = true, showUsername = false, linuxAppearence = true, showPath = true;

    private int suggestionTextColor, defaulSuggestionColor, appSuggestionColor, aliasSuggestionColor, musicSuggestionColor, contactsSuggestionColor, commandSuggestionColor, fileSuggestionColor;
    private boolean multicolorSuggestions, transparentSuggestions;

    public SkinManager(PreferencesManager prefs) {
        systemFont = Boolean.parseBoolean(prefs.getValue(PreferencesManager.USE_SYSTEMFONT));
        inputBottom = Boolean.parseBoolean(prefs.getValue(PreferencesManager.INPUTFIELD_BOTTOM));
        showSubmit = Boolean.parseBoolean(prefs.getValue(PreferencesManager.SHOWSUBMIT));

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

        try {
            showUsernameAndDeviceWhenEmpty = Boolean.parseBoolean(prefs.getValue(PreferencesManager.SHOWUSERNAMEWHENINPUTEMPTY));
            if(showUsernameAndDeviceWhenEmpty) {
                showUsername = Boolean.parseBoolean(prefs.getValue(PreferencesManager.SHOWUSERNAME));
                if(showUsername) {
                    username = prefs.getValue(PreferencesManager.USERNAME);
                }

                showPath = Boolean.parseBoolean(prefs.getValue(PreferencesManager.SHOWPATH_SESSIONINFO));
            }
        } catch (Exception e) {
            showUsernameAndDeviceWhenEmpty = false;
        }

        try {
            linuxAppearence = Boolean.parseBoolean(prefs.getValue(PreferencesManager.LINUXAPPERARENCE));
        } catch (Exception e) {
            linuxAppearence = true;
        }

        showSuggestions = Boolean.parseBoolean(prefs.getValue(PreferencesManager.SHOWSUGGESTIONS));
        if (showSuggestions) {

            try {
                suggestionTextColor = Color.parseColor(prefs.getValue(PreferencesManager.SUGGESTIONTEXT_COLOR));
            } catch (Exception e) {
                suggestionTextColor = suggestionTextColorDefault;
            }

            try {
                transparentSuggestions = Boolean.parseBoolean(prefs.getValue(PreferencesManager.TRANSPARENT_SUGGESTIONS));
            } catch (Exception e) {
                transparentSuggestions = false;
            }

            try {
                multicolorSuggestions = Boolean.parseBoolean(prefs.getValue(PreferencesManager.USE_MULTICOLOR_SUGGESTIONS));
            } catch (Exception e) {
                multicolorSuggestions = false;
            }

            if(multicolorSuggestions) {
                transparentSuggestions = false;
            }

            if(transparentSuggestions && suggestionTextColor == bgColor) {
                suggestionTextColor = Color.GREEN;
            } else {
                try {
                    defaulSuggestionColor = Color.parseColor(prefs.getValue(PreferencesManager.DEFAULT_SUGGESTION_BG));
                } catch (Exception e) {
                    defaulSuggestionColor = defaultSuggestionBgDefault;
                }

                if(multicolorSuggestions) {
                    try {
                        appSuggestionColor = Color.parseColor(prefs.getValue(PreferencesManager.APP_SUGGESTION_BG));
                    } catch (Exception e) {
                        appSuggestionColor = appSuggestionBgDefault;
                    }

                    try {
                        contactsSuggestionColor = Color.parseColor(prefs.getValue(PreferencesManager.CONTACT_SUGGESTION_BG));
                    } catch (Exception e) {
                        contactsSuggestionColor = contactSuggestionBgDefault;
                    }

                    try {
                        commandSuggestionColor = Color.parseColor(prefs.getValue(PreferencesManager.COMMAND_SUGGESTION_BG));
                    } catch (Exception e) {
                        commandSuggestionColor = commandSuggestionsBgDefault;
                    }

                    try {
                        musicSuggestionColor = Color.parseColor(prefs.getValue(PreferencesManager.SONG_SUGGESTION_BG));
                    } catch (Exception e) {
                        musicSuggestionColor = songSuggestionBgDefault;
                    }

                    try {
                        fileSuggestionColor = Color.parseColor(prefs.getValue(PreferencesManager.FILE_SUGGESTION_BG));
                    } catch (Exception e) {
                        fileSuggestionColor = fileSuggestionBgDeafult;
                    }

                    try {
                        aliasSuggestionColor = Color.parseColor(prefs.getValue(PreferencesManager.ALIAS_SIGGESTION_BG));
                    } catch (Exception e) {
                        aliasSuggestionColor = aliasSuggestionBgDefault;
                    }
                }
            }
        }
    }

    public boolean getUseSystemFont() {
        return systemFont;
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

    public boolean getInputBottom() {
        return inputBottom;
    }

    public boolean getShowSubmit() {
        return showSubmit;
    }

    public ColorDrawable getSuggestionBg(Integer type) {
        if(transparentSuggestions) {
            type = 0;
        }

        if(transparentSuggestions) {
            return new ColorDrawable(Color.TRANSPARENT);
        } else if(!multicolorSuggestions) {
            return new ColorDrawable(defaulSuggestionColor);
        } else {
            switch (type) {
                case SuggestionsManager.Suggestion.TYPE_APP:
                    return new ColorDrawable(appSuggestionColor);
                case SuggestionsManager.Suggestion.TYPE_ALIAS:
                    return new ColorDrawable(aliasSuggestionColor);
                case SuggestionsManager.Suggestion.TYPE_COMMAND:
                    return new ColorDrawable(commandSuggestionColor);
                case SuggestionsManager.Suggestion.TYPE_CONTACT:
                    return new ColorDrawable(contactsSuggestionColor);
                case SuggestionsManager.Suggestion.TYPE_FILE:
                    return new ColorDrawable(fileSuggestionColor);
                case SuggestionsManager.Suggestion.TYPE_SONG:
                    return new ColorDrawable(musicSuggestionColor);
                default:
                    return new ColorDrawable(defaulSuggestionColor);
            }
        }
    }

    public int getSuggestionTextColor() {
        return suggestionTextColor;
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

    public String getUsername() {
        return username;
    }

    public boolean showUsernameAndDeviceWhenEmpty() {
        return showUsernameAndDeviceWhenEmpty;
    }

    public boolean linuxAppearence() {
        return linuxAppearence;
    }

    public boolean showUsername() {
        return showUsername;
    }

    public boolean showPath() {
        return showPath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.globalFontSize);
        dest.writeInt(this.deviceColor);
        dest.writeInt(this.inputColor);
        dest.writeInt(this.outputColor);
        dest.writeInt(this.ramColor);
        dest.writeInt(this.bgColor);
        dest.writeByte(this.useSystemWp ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showSuggestions ? (byte) 1 : (byte) 0);
        dest.writeByte(this.systemFont ? (byte) 1 : (byte) 0);
        dest.writeByte(this.inputBottom ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showSubmit ? (byte) 1 : (byte) 0);
        dest.writeString(this.username);
        dest.writeByte(this.showUsernameAndDeviceWhenEmpty ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showUsername ? (byte) 1 : (byte) 0);
        dest.writeByte(this.linuxAppearence ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showPath ? (byte) 1 : (byte) 0);
        dest.writeInt(this.suggestionTextColor);
        dest.writeInt(this.defaulSuggestionColor);
        dest.writeInt(this.appSuggestionColor);
        dest.writeInt(this.aliasSuggestionColor);
        dest.writeInt(this.musicSuggestionColor);
        dest.writeInt(this.contactsSuggestionColor);
        dest.writeInt(this.commandSuggestionColor);
        dest.writeInt(this.fileSuggestionColor);
        dest.writeByte(this.multicolorSuggestions ? (byte) 1 : (byte) 0);
        dest.writeByte(this.transparentSuggestions ? (byte) 1 : (byte) 0);
    }

    protected SkinManager(Parcel in) {
        this.globalFontSize = in.readInt();
        this.deviceColor = in.readInt();
        this.inputColor = in.readInt();
        this.outputColor = in.readInt();
        this.ramColor = in.readInt();
        this.bgColor = in.readInt();
        this.useSystemWp = in.readByte() != 0;
        this.showSuggestions = in.readByte() != 0;
        this.systemFont = in.readByte() != 0;
        this.inputBottom = in.readByte() != 0;
        this.showSubmit = in.readByte() != 0;
        this.username = in.readString();
        this.showUsernameAndDeviceWhenEmpty = in.readByte() != 0;
        this.showUsername = in.readByte() != 0;
        this.linuxAppearence = in.readByte() != 0;
        this.showPath = in.readByte() != 0;
        this.suggestionTextColor = in.readInt();
        this.defaulSuggestionColor = in.readInt();
        this.appSuggestionColor = in.readInt();
        this.aliasSuggestionColor = in.readInt();
        this.musicSuggestionColor = in.readInt();
        this.contactsSuggestionColor = in.readInt();
        this.commandSuggestionColor = in.readInt();
        this.fileSuggestionColor = in.readInt();
        this.multicolorSuggestions = in.readByte() != 0;
        this.transparentSuggestions = in.readByte() != 0;
    }

    public static final Creator<SkinManager> CREATOR = new Creator<SkinManager>() {
        @Override
        public SkinManager createFromParcel(Parcel source) {
            return new SkinManager(source);
        }

        @Override
        public SkinManager[] newArray(int size) {
            return new SkinManager[size];
        }
    };
}
