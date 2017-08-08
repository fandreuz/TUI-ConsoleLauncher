package ohi.andre.consolelauncher.managers.music;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.broadcast.HeadsetBroadcast;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

public class MusicManager implements OnCompletionListener {

    public static final String[] MUSIC_EXTENSIONS = {".mp3", ".wav", ".ogg", ".flac"};

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
    public MusicManager(Context c, Outputable outputable) {
        try {
            this.mp = new MediaPlayer();
            this.mp.setOnCompletionListener(this);
            this.files = new ArrayList<>();

            this.outputable = outputable;

            c.registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

            boolean randomActive = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.random_play);
            fromMediastore = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.songs_from_mediastore);
            if (fromMediastore) {
                songsFolder = null;
            } else {
                String path = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.songs_folder);
                if (path == null || path.length() == 0 || path.equals("null")) {
                    return;
                }
                songsFolder = new File(path);
            }

            refresh(c);

            if (randomActive && files != null) {
                Collections.shuffle(files);
            }
        } catch (Exception e) {}
    }

    public boolean initPlayer() {
        return prepareSong(currentSongIndex);
    }

    public String getPath(String name) {
        if(files == null) return null;

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

    //	return listNames
    public List<String> getNames() {
        if(files == null) {
            return new ArrayList<>(0);
        }

        List<String> names = new ArrayList<>();

        for (File file : files) {
            names.add(file.getName());
        }

        Collections.sort(names);

        return names;
    }

    //	return paths
    public List<String> getPaths() {
        if(files == null) {
            return new ArrayList<>();
        }

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
            fillFromMediaStore(c, files);
        } else {
            fillFromDir(files, songsFolder);
        }
    }

    private static void fillFromMediaStore(final Context c, final List<File> files) {
        files.clear();

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = c.getContentResolver();

                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
                String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
                Cursor cur = cr.query(uri, null, selection, null, sortOrder);
                int count = 0;

                if(cur != null) {
                    count = cur.getCount();
                    if(count > 0) {
                        while(cur.moveToNext()) {
                            String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                            if(data != null) {
                                try {
                                    files.add(new File(data));
                                } catch (Exception e) {}
                            }
                        }
                    }

                    cur.close();
                }
            }
        }.start();
    }

    private static void fillFromDir(final List<File> files, final File dir) {
        files.clear();

        new Thread() {
            @Override
            public void run() {
                files.addAll(Tuils.getSongsInFolder(dir));
            }
        }.start();
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
