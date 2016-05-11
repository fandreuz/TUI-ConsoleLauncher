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

public class MusicManager implements OnCompletionListener {

    public static final boolean USE_SCROLL_COMPARE = true;

    private File songFolder;
    private MediaPlayer mp;

    private int currentSongIndex = 0;
    private File currentSong = null;

    private boolean randomActive;

    //	headset broadcast
    private BroadcastReceiver headsetReceiver = new HeadsetBroadcast(new Runnable() {
        @Override
        public void run() {
            onHeadsetUnplugged();
        }
    });

    //	constructor
    public MusicManager(Context c, PreferencesManager preferencesManager) {
        this.mp = new MediaPlayer();
        this.mp.setOnCompletionListener(this);

        c.registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        randomActive = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.PLAY_RANDOM));
        songFolder = new File(preferencesManager.getValue(PreferencesManager.SONGSFOLDER));
    }

    public boolean initPlayer() {
        return prepareSong(currentSongIndex);
    }

    //	return a song by incomplete name
    public String getSong(String s, int minRate) {
        return Compare.getOneSimilarString(getNames(), s, minRate, USE_SCROLL_COMPARE);
    }

    //	return the path by complete name
    public String getPath(String name) {
        File file = new File(songFolder, name);
        if (!file.exists())
            return null;
        return file.getAbsolutePath();
    }

    //	return names
    public List<String> getNames() {
        List<File> songs = Tuils.getSongsInFolder(songFolder);
        List<String> names = new ArrayList<>();

        for (File file : songs)
            names.add(file.getName());

        Collections.sort(names);

        return names;
    }

    //	return paths
    public List<String> getPaths() {
        List<File> songs = Tuils.getSongsInFolder(songFolder);
        List<String> paths = new ArrayList<>();

        for (File file : songs)
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
        if (songFolder == null)
            return false;

        List<String> songs = getPaths();
        if (randomActive) {
            Random random = new Random();
            int totalSongs = songs.size();

            int newSong;
            do {
                newSong = random.nextInt(totalSongs);
            } while (songIndex == newSong);
            songIndex = newSong;
        } else {
            if (songIndex >= songs.size())
                songIndex -= songs.size();
            else if (songIndex < 0)
                songIndex += songs.size();

            currentSongIndex = songIndex;
        }

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

    @Override
    public void onCompletion(MediaPlayer mp) {
        next();
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
