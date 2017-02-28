package ohi.andre.consolelauncher.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.broadcast.HeadsetBroadcast;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

public class MusicManager implements OnCompletionListener {

    public static final String[] MUSIC_EXTENSIONS = {".mp3", ".wav", ".ogg", ".flac"};

    public static final boolean USE_SCROLL_COMPARE = true;

    private List<File> files;
    private MediaPlayer mp;

    private int currentSongIndex = 0;
    private File currentSong = null;

    private boolean fromMediastore;
    File songsFolder;

    private Outputable outputable;

    //	headset broadcast
    private BroadcastReceiver headsetReceiver = new HeadsetBroadcast(new Runnable() {
        @Override
        public void run() {
            onHeadsetUnplugged();
        }
    });

    //	constructor
    public MusicManager(Context c, PreferencesManager preferencesManager, Outputable outputable) {
        this.mp = new MediaPlayer();
        this.mp.setOnCompletionListener(this);

        this.outputable = outputable;

        c.registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        boolean randomActive = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.PLAY_RANDOM));
        fromMediastore = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.FROM_MEDIASTORE));
        if(fromMediastore) {
            songsFolder = null;
        } else {
            songsFolder = new File(preferencesManager.getValue(PreferencesManager.SONGSFOLDER));
        }

        refresh(c);

        if(randomActive && files != null) {
            Collections.shuffle(files);
        }
    }

    public boolean initPlayer() {
        return prepareSong(currentSongIndex);
    }

    //	return a song by incomplete name
    public String getSong(String s, int minRate) {
        return Compare.similarString(getNames(), s, minRate, USE_SCROLL_COMPARE);
    }

    //	return the path by complete name
    public String getPath(String name) {
        int count = 0;
        File file = files.get(count);
        while(!file.getName().equals(name)) {
            if(count == files.size()) {
                return null;
            }
            file = files.get(++count);
        }
        return file.getAbsolutePath();
    }

    //	return names
    public List<String> getNames() {
        List<String> names = new ArrayList<>();

        for (File file : files) {
            names.add(file.getName());
        }

        Collections.sort(names);

        return names;
    }

    //	return paths
    public List<String> getPaths() {

        List<String> paths = new ArrayList<>();

        for (File file : files)
            paths.add(file.getAbsolutePath());

        return paths;
    }

    public boolean isPlaying() {
        try {
            return mp.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean prepareSong(int songIndex) {
        if (files == null) {
            return false;
        }

        List<String> songs = getPaths();
        if(songs == null || songs.size() == 0) {
            return false;
        }

        if (songIndex >= songs.size())
            songIndex -= songs.size();
        else if (songIndex < 0) {
            songIndex += songs.size();
        }

        currentSongIndex = songIndex;

        return prepareSong(songs.get(songIndex));
    }

    private boolean prepareSong(String path) {
        if (path == null || path.length() == 0)
            return false;

        currentSong = new File(path);

        try {
            mp.reset();
            mp.setDataSource(path);
            mp.prepare();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean jukebox(String path) {
        if (!prepareSong(path))
            return false;

        mp.start();
        return true;
    }

    public String next() throws IllegalStateException {
        if (!prepareSong(currentSongIndex + 1))
            return null;

        mp.start();
        return currentSong.getName();
    }

    public String prev() throws IllegalStateException {
        if (!prepareSong(currentSongIndex - 1))
            return null;

        mp.start();
        return currentSong.getName();
    }

    public void stop() {
        if (mp.isPlaying())
            mp.stop();
    }

    public String trackInfo() {
        int total = mp.getDuration() / 1000;
        int position = mp.getCurrentPosition() / 1000;
        return currentSong.getName() +
                Tuils.NEWLINE + (total / 60) + "." + (total % 60) + " / " + (position / 60) + "." + (position % 60) +
                " (" + (100 * position / total) + "%)";
    }

    public void refresh(Context c) {
        if(fromMediastore) {
            files = Tuils.getMediastoreSongs(c);
        } else {
            files = Tuils.getSongsInFolder(songsFolder);
        }

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        outputable.onOutput(next());
    }

    public void onHeadsetUnplugged() {
        if (mp != null && mp.isPlaying())
            mp.pause();
    }

    public void destroy(Context context) {
        this.stop();
        context.unregisterReceiver(headsetReceiver);
    }
}
