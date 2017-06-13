package ohi.andre.consolelauncher.managers;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;

public class SkinManager implements Parcelable {

    public static final int SYSTEM_WALLPAPER = -1;

    public static final int SUGGESTION_PADDING_VERTICAL = 15;
    public static final int SUGGESTION_PADDING_HORIZONTAL = 15;
    public static final int SUGGESTION_MARGIN = 20;

    private static final int deviceScale = 3;
    private static final int textScale = 2;
    private static final int ramScale = 3;
    private static final int suggestionScale = 0;

    public int globalFontSize;

    public String deviceName;
    public int deviceColor, inputColor, outputColor, ramColor, bgColor, overlayColor;

    public boolean useSystemWp, showSuggestions, systemFont, inputBottom, showSubmit;

    public String username = null;
    public boolean showUsernameAndDeviceWhenEmpty = true, showUsername = false, showDeviceInSessionInfo = false, linuxAppearence = true, showPath = true;

    private int suggDefaultText, suggDefaultBg, suggAliasText, suggAliasBg, suggSongText, suggSongBg, suggContactText, suggContactBg, suggAppText, suggAppBg, suggCmdText, suggCmdBg, suggFileText, suggFileBg;
    private boolean transparentSuggestions;

    public SkinManager() {
        systemFont = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.system_font);
        inputBottom = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.input_bottom);
        showSubmit = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_enter_button);

        globalFontSize = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.font_size);

        useSystemWp = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.system_wallpaper);
        if (useSystemWp) {
            bgColor = SYSTEM_WALLPAPER;
            overlayColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.overlay_color);
        }
        else bgColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.bg_color);

        deviceColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.device_color);
        ramColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.ram_color);
        inputColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.input_color);
        outputColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.output_color);

        deviceName = XMLPrefsManager.get(String.class, XMLPrefsManager.Ui.deviceName);
        if (deviceName.length() == 0 || deviceName.equals("null")) {
            deviceName = Build.DEVICE;
        }

        showUsernameAndDeviceWhenEmpty = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_ssninfo);
        if(showUsernameAndDeviceWhenEmpty) {
            showUsername = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_username_ssninfo);
            if(showUsername) {
                username = XMLPrefsManager.get(String.class, XMLPrefsManager.Ui.username);
            }

            showDeviceInSessionInfo = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_devicename_ssninfo);
            showPath = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_path_ssninfo);
        }

        linuxAppearence = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.linux_like);

        showSuggestions = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Suggestions.enabled);
        if (showSuggestions) {

            suggDefaultText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.default_text_color);
            suggDefaultBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.default_bg_color);
            transparentSuggestions = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Suggestions.transparent);

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

    public ColorDrawable getSuggestionBg(Integer type) {
        if(transparentSuggestions) {
            return new ColorDrawable(Color.TRANSPARENT);
        } else {
            switch (type) {
                case SuggestionsManager.Suggestion.TYPE_APP:
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
        int choosen;
        switch (type) {
            case SuggestionsManager.Suggestion.TYPE_APP:
                choosen = suggAppText;
            case SuggestionsManager.Suggestion.TYPE_ALIAS:
                choosen = suggAliasText;
            case SuggestionsManager.Suggestion.TYPE_COMMAND:
                choosen = suggCmdText;
            case SuggestionsManager.Suggestion.TYPE_CONTACT:
                choosen = suggContactText;
            case SuggestionsManager.Suggestion.TYPE_FILE:
                choosen = suggFileText;
            case SuggestionsManager.Suggestion.TYPE_SONG:
                choosen = suggSongText;
            default:
                choosen = suggDefaultText;
        }

        if(choosen == -1) choosen = suggDefaultText;
        return choosen;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.globalFontSize);
        dest.writeString(this.deviceName);
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
        dest.writeByte(this.showDeviceInSessionInfo ? (byte) 1 : (byte) 0);
        dest.writeByte(this.linuxAppearence ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showPath ? (byte) 1 : (byte) 0);
        dest.writeInt(this.suggDefaultText);
        dest.writeInt(this.suggDefaultBg);
        dest.writeInt(this.suggAliasText);
        dest.writeInt(this.suggAliasBg);
        dest.writeInt(this.suggSongText);
        dest.writeInt(this.suggSongBg);
        dest.writeInt(this.suggContactText);
        dest.writeInt(this.suggContactBg);
        dest.writeInt(this.suggAppText);
        dest.writeInt(this.suggAppBg);
        dest.writeInt(this.suggCmdText);
        dest.writeInt(this.suggCmdBg);
        dest.writeInt(this.suggFileText);
        dest.writeInt(this.suggFileBg);
        dest.writeByte(this.transparentSuggestions ? (byte) 1 : (byte) 0);
    }

    protected SkinManager(Parcel in) {
        this.globalFontSize = in.readInt();
        this.deviceName = in.readString();
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
        this.showDeviceInSessionInfo = in.readByte() != 0;
        this.linuxAppearence = in.readByte() != 0;
        this.showPath = in.readByte() != 0;
        this.suggDefaultText = in.readInt();
        this.suggDefaultBg = in.readInt();
        this.suggAliasText = in.readInt();
        this.suggAliasBg = in.readInt();
        this.suggSongText = in.readInt();
        this.suggSongBg = in.readInt();
        this.suggContactText = in.readInt();
        this.suggContactBg = in.readInt();
        this.suggAppText = in.readInt();
        this.suggAppBg = in.readInt();
        this.suggCmdText = in.readInt();
        this.suggCmdBg = in.readInt();
        this.suggFileText = in.readInt();
        this.suggFileBg = in.readInt();
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
