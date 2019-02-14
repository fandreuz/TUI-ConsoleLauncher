package ohi.andre.consolelauncher.tuils;

import android.support.v4.content.FileProvider;

import ohi.andre.consolelauncher.BuildConfig;

public class GenericFileProvider extends FileProvider {
    public static final String PROVIDER_NAME = BuildConfig.APPLICATION_ID + ".FILE_PROVIDER";
}
