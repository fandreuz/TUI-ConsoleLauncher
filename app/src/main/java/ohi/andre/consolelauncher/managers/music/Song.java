package ohi.andre.consolelauncher.managers.music;

import java.io.File;

import it.andreuzzi.comparestring2.StringableObject;

/**
 * Created by francescoandreuzzi on 17/08/2017.
 */

public class Song implements StringableObject  {

    private long id;
    private String title, path, lowercaseTitle;

    public Song(long songID, String songTitle) {
        id = songID;
        title = songTitle;
        this.lowercaseTitle = title.toLowerCase();
    }

    public Song(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf(".");
        name = name.substring(0,dot);

        this.title = name;
        this.path = file.getAbsolutePath();
        this.id = -1;
    }

    public long getID() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getLowercaseString() {
        return lowercaseTitle;
    }

    @Override
    public String getString() {
        return title;
    }
}

