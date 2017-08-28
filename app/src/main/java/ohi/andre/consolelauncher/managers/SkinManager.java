package ohi.andre.consolelauncher.managers;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;

public class SkinManager implements Parcelable {

    public static final int SUGGESTION_PADDING_VERTICAL = 15;
    public static final int SUGGESTION_PADDING_HORIZONTAL = 15;
    public static final int SUGGESTION_MARGIN = 20;

    private static final int suggestionScale = 0;

    private static final int textScale = 3;

    public static final int COLOR_NOT_SET = 16777216;

    public int globalFontSize;

    public String deviceName;
    public String userFontPath;
    public int deviceColor, inputColor, outputColor, ramColor, bgColor, overlayColor, toolbarColor, toolbarBg, enter_color, time_color, battery_color_high, battery_color_medium, battery_color_low,
            storageColor;

    public boolean useSystemWp, showSuggestions, systemFont, inputBottom, showSubmit, manyColorsBattery;

    public String username = null, prefix = null, ssnInfoFormat = null;

    private int suggDefaultText, suggDefaultBg, suggAliasText, suggAliasBg, suggSongText, suggSongBg, suggContactText, suggContactBg, suggAppText, suggAppBg, suggCmdText, suggCmdBg, suggFileText, suggFileBg;
    private boolean transparentSuggestions;

    public SkinManager() {
        systemFont = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.system_font);
        userFontPath = XMLPrefsManager.get(String.class, XMLPrefsManager.Ui.user_font_file);
        inputBottom = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.input_bottom);
        showSubmit = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_enter_button);

        manyColorsBattery = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.enable_battery_status);

        globalFontSize = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.font_size);

        useSystemWp = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.system_wallpaper);
        overlayColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.overlay_color);
        bgColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.bg_color);

        deviceColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.device_color);
        ramColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.ram_color);
        inputColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.input_color);
        outputColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.output_color);
        toolbarColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.toolbar_color);
        toolbarBg = XMLPrefsManager.getColor(XMLPrefsManager.Theme.toolbar_bg);
        enter_color = XMLPrefsManager.getColor(XMLPrefsManager.Theme.enter_color);
        time_color = XMLPrefsManager.getColor(XMLPrefsManager.Theme.time_color);
        storageColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.storage_color);
        battery_color_high = XMLPrefsManager.getColor(XMLPrefsManager.Theme.battery_color_high);
        if(manyColorsBattery) {
            battery_color_medium = XMLPrefsManager.getColor(XMLPrefsManager.Theme.battery_color_medium);
            battery_color_low = XMLPrefsManager.getColor(XMLPrefsManager.Theme.battery_color_low);
        }

        prefix = XMLPrefsManager.get(String.class, XMLPrefsManager.Ui.input_prefix);

        deviceName = XMLPrefsManager.get(String.class, XMLPrefsManager.Ui.deviceName);
        if (deviceName.length() == 0 || deviceName.equals("null")) {
            deviceName = Build.DEVICE;
        }

        username = XMLPrefsManager.get(String.class, XMLPrefsManager.Ui.username);

        ssnInfoFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.session_info_format);

        showSuggestions = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Suggestions.show_suggestions);
        if (showSuggestions) {

            suggDefaultText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.default_text_color);
            suggDefaultBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.default_bg_color);
            transparentSuggestions = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Suggestions.transparent_suggestions);

            if(transparentSuggestions && suggDefaultText == bgColor) {
                suggDefaultText = Color.GREEN;
            } else {
                suggAppText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.apps_text_color);
                suggAppBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.apps_bg_color);

                suggAliasText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.alias_text_color);
                suggAliasBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.alias_bg_color);

                suggCmdText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.cmd_text_color);
                suggCmdBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.cmd_bg_color);

                suggContactText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.contact_text_color);
                suggContactBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.contact_bg_color);

                suggFileText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.file_text_color);
                suggFileBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.file_bg_color);

                suggSongText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.song_text_color);
                suggSongBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.song_bg_color);
            }
        }
    }

    protected SkinManager(Parcel in) {
        globalFontSize = in.readInt();
        userFontPath = in.readString();
        deviceName = in.readString();
        deviceColor = in.readInt();
        inputColor = in.readInt();
        outputColor = in.readInt();
        ramColor = in.readInt();
        bgColor = in.readInt();
        overlayColor = in.readInt();
        toolbarColor = in.readInt();
        toolbarBg = in.readInt();
        enter_color = in.readInt();
        time_color = in.readInt();
        battery_color_high = in.readInt();
        battery_color_medium = in.readInt();
        battery_color_low = in.readInt();
        useSystemWp = in.readByte() != 0;
        showSuggestions = in.readByte() != 0;
        systemFont = in.readByte() != 0;
        inputBottom = in.readByte() != 0;
        showSubmit = in.readByte() != 0;
        manyColorsBattery = in.readByte() != 0;
        username = in.readString();
        ssnInfoFormat = in.readString();
        suggDefaultText = in.readInt();
        suggDefaultBg = in.readInt();
        suggAliasText = in.readInt();
        suggAliasBg = in.readInt();
        suggSongText = in.readInt();
        suggSongBg = in.readInt();
        suggContactText = in.readInt();
        suggContactBg = in.readInt();
        suggAppText = in.readInt();
        suggAppBg = in.readInt();
        suggCmdText = in.readInt();
        suggCmdBg = in.readInt();
        suggFileText = in.readInt();
        suggFileBg = in.readInt();
        transparentSuggestions = in.readByte() != 0;
        prefix = in.readString();
    }

    public static final Creator<SkinManager> CREATOR = new Creator<SkinManager>() {
        @Override
        public SkinManager createFromParcel(Parcel in) {
            return new SkinManager(in);
        }

        @Override
        public SkinManager[] newArray(int size) {
            return new SkinManager[size];
        }
    };

    public Typeface getUserFont(Typeface defaultTypeface) {
        try
        {
            File userFontFile = new File(userFontPath);
            if (userFontFile.exists())
            {
                if (userFontFile.isFile())
                {
                    return Typeface.createFromFile(userFontPath);
                }
                else
                    throw new Exception("The specified file is not a file. Using built-in font.");
            }
            else
                throw new Exception("The specified file is not found. Using built-in font.");
        }
        catch(Exception ex)
        {
            return defaultTypeface;
        }
    }

    public ColorDrawable getSuggestionBg(Integer type) {
        if(transparentSuggestions) {
            return new ColorDrawable(Color.TRANSPARENT);
        } else {
            switch (type) {
                case SuggestionsManager.Suggestion.TYPE_APP:case SuggestionsManager.Suggestion.TYPE_APPGROUP:
                    return new ColorDrawable(suggAppBg);
                case SuggestionsManager.Suggestion.TYPE_ALIAS:
                    return new ColorDrawable(suggAliasBg);
                case SuggestionsManager.Suggestion.TYPE_COMMAND:
                    return new ColorDrawable(suggCmdBg);
                case SuggestionsManager.Suggestion.TYPE_CONTACT:
                    return new ColorDrawable(suggContactBg);
                case SuggestionsManager.Suggestion.TYPE_FILE:
                    return new ColorDrawable(suggFileBg);
                case SuggestionsManager.Suggestion.TYPE_SONG:
                    return new ColorDrawable(suggSongBg);
                default:
                    return new ColorDrawable(suggDefaultBg);
            }
        }
    }

    public int getSuggestionTextColor(Integer type) {
        int chosen;

        switch (type) {
            case SuggestionsManager.Suggestion.TYPE_APP:
                chosen = suggAppText;
                break;
            case SuggestionsManager.Suggestion.TYPE_ALIAS:
                chosen = suggAliasText;
                break;
            case SuggestionsManager.Suggestion.TYPE_COMMAND:
                chosen = suggCmdText;
                break;
            case SuggestionsManager.Suggestion.TYPE_CONTACT:
                chosen = suggContactText;
                break;
            case SuggestionsManager.Suggestion.TYPE_FILE:
                chosen = suggFileText;
                break;
            case SuggestionsManager.Suggestion.TYPE_SONG:
                chosen = suggSongText;
                break;
            default:
                chosen = suggDefaultText;
                break;
        }

        if(chosen == COLOR_NOT_SET) chosen = suggDefaultText;
        return chosen;
    }

    public int getTextSize() {
        return globalFontSize - textScale;
    }

    public int getSuggestionSize() {
        return globalFontSize - suggestionScale;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(globalFontSize);
        dest.writeString(userFontPath);
        dest.writeString(deviceName);
        dest.writeInt(deviceColor);
        dest.writeInt(inputColor);
        dest.writeInt(outputColor);
        dest.writeInt(ramColor);
        dest.writeInt(bgColor);
        dest.writeInt(overlayColor);
        dest.writeInt(toolbarColor);
        dest.writeInt(toolbarBg);
        dest.writeInt(enter_color);
        dest.writeInt(time_color);
        dest.writeInt(battery_color_high);
        dest.writeInt(battery_color_medium);
        dest.writeInt(battery_color_low);
        dest.writeByte((byte) (useSystemWp ? 1 : 0));
        dest.writeByte((byte) (showSuggestions ? 1 : 0));
        dest.writeByte((byte) (systemFont ? 1 : 0));
        dest.writeByte((byte) (inputBottom ? 1 : 0));
        dest.writeByte((byte) (showSubmit ? 1 : 0));
        dest.writeByte((byte) (manyColorsBattery ? 1 : 0));
        dest.writeString(username);
        dest.writeString(ssnInfoFormat);
        dest.writeInt(suggDefaultText);
        dest.writeInt(suggDefaultBg);
        dest.writeInt(suggAliasText);
        dest.writeInt(suggAliasBg);
        dest.writeInt(suggSongText);
        dest.writeInt(suggSongBg);
        dest.writeInt(suggContactText);
        dest.writeInt(suggContactBg);
        dest.writeInt(suggAppText);
        dest.writeInt(suggAppBg);
        dest.writeInt(suggCmdText);
        dest.writeInt(suggCmdBg);
        dest.writeInt(suggFileText);
        dest.writeInt(suggFileBg);
        dest.writeByte((byte) (transparentSuggestions ? 1 : 0));
        dest.writeString(prefix);
    }
}
