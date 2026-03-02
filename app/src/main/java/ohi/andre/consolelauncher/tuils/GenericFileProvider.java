package ohi.andre.consolelauncher.tuils;

import androidx.core.content.FileProvider;

import ohi.andre.consolelauncher.BuildConfig;

public class GenericFileProvider extends FileProvider {
    public static final String PROVIDER_NAME = BuildConfig.APPLICATION_ID + ".FILE_PROVIDER";
}
