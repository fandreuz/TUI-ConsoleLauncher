package ohi.andre.consolelauncher.managers.music;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.MediaController;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 17/08/2017.
 */

public class MusicManager2 implements MediaController.MediaPlayerControl {

    public static final String[] MUSIC_EXTENSIONS = {".mp3", ".wav", ".ogg", ".flac"};

    final int WAITING_NEXT = 10, WAITING_PREVIOUS = 11, WAITING_PLAY = 12, WAITING_LISTEN = 13;

    Context mContext;

    List<Song> songs;

    MusicService musicSrv;
    boolean musicBound=false;
    Intent playIntent;

    boolean playbackPaused=true, stopped = true;

    Thread loader;

    int waitingMethod = 0;
    String savedParam;

    BroadcastReceiver headsetBroadcast;

    public MusicManager2(Context c) {
        mContext = c;
        updateSongs();

        headsetBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getIntExtra("state", 0) == 0) pause();
            }
        };

        String action;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            action = AudioManager.ACTION_HEADSET_PLUG;
        } else {
            action = Intent.ACTION_HEADSET_PLUG;
        }

        mContext.getApplicationContext().registerReceiver(headsetBroadcast, new IntentFilter(action));

        init();
    }

    public void init() {
        playIntent = new Intent(mContext, MusicService.class);
        mContext.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        mContext.startService(playIntent);
    }

    public void refresh() {
        destroy();
        updateSongs();
    }

    public void destroy() {
        if(musicSrv != null && musicBound) {
            musicSrv.stop();
            mContext.unbindService(musicConnection);
            mContext.stopService(playIntent);
            musicSrv = null;
        }

        try {
            mContext.getApplicationContext().unregisterReceiver(headsetBroadcast);
        } catch (Exception e) {
            Tuils.log(e);
        }

        musicBound = false;
        playbackPaused = true;
        stopped = true;
    }

    public String playNext() {
        if(!musicBound) {
            init();
            waitingMethod = WAITING_NEXT;

            return null;
        }

        playbackPaused=false;
        stopped = false;

        return musicSrv.playNext();
    }

    public String playPrev() {
        if(!musicBound) {
            init();
            waitingMethod = WAITING_PREVIOUS;

            return null;
        }

        playbackPaused = false;
        stopped = false;

        return musicSrv.playPrev();
    }

    @Override
    public void pause() {
        if(musicSrv == null || playbackPaused) return;

        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    public String play() {
        if(!musicBound) {
            init();
            waitingMethod = WAITING_PLAY;

            return null;
        }

        if(stopped) {
            musicSrv.playSong();
            playbackPaused = false;
            stopped = false;
        } else if(playbackPaused) {
            playbackPaused = false;
            musicSrv.playPlayer();
        } else pause();

        return null;
    }

    public String lsSongs() {
        if(songs.size() == 0) return "[]";

        List<String> ss = new ArrayList<>();
        for(Song s : songs) {
            ss.add(s.getTitle());
        }

        Collections.sort(ss);
        Tuils.addPrefix(ss, Tuils.DOUBLE_SPACE);
        Tuils.insertHeaders(ss, false);

        return Tuils.toPlanString(ss, Tuils.NEWLINE);
    }

    public void updateSongs() {
        loader = new StoppableThread() {
            @Override
            public void run() {
                try {
                    if(songs == null) songs = new ArrayList<>();
                    else songs.clear();

                    if(XMLPrefsManager.getBoolean(Behavior.songs_from_mediastore)) {
                        ContentResolver musicResolver = mContext.getContentResolver();
                        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
                        if(musicCursor!=null && musicCursor.moveToFirst()){
                            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                            do {
                                long thisId = musicCursor.getLong(idColumn);
                                String thisTitle = musicCursor.getString(titleColumn);
                                songs.add(new Song(thisId, thisTitle));
                            }
                            while (musicCursor.moveToNext());
                        }
                        musicCursor.close();
                    } else {
                        String path = XMLPrefsManager.get(Behavior.songs_folder);
                        if(path.length() == 0) return;

                        File file;
                        if(path.startsWith(File.separator)) {
                            file = new File(path);
                        } else {
                            file = new File(XMLPrefsManager.get(Behavior.home_path), path);
                        }

                        if(file.exists() && file.isDirectory()) songs.addAll(Tuils.getSongsInFolder(file));
                    }
                } catch (Exception e) {
                    Tuils.toFile(e);
                }

                synchronized (songs) {
                    songs.notify();
                }
            }
        };
        loader.start();
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            musicSrv = binder.getService();
            musicSrv.setShuffle(XMLPrefsManager.getBoolean(Behavior.random_play));

            if(loader.isAlive()) {
                synchronized (songs) {
                    try {
                        songs.wait();
                    } catch (InterruptedException e) {}
                }
            }

            musicSrv.setList(songs);
            musicBound = true;

            switch (waitingMethod) {
                case WAITING_NEXT:
                    playNext();
                    break;
                case WAITING_PREVIOUS:
                    playPrev();
                    break;
                case WAITING_PLAY:
                    play();
                    break;
                case WAITING_LISTEN:
                    select(savedParam);
                    break;
            }

            waitingMethod = 0;
            savedParam = null;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return -1;
    }

    @Override
    public int getDuration() {
        if(musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return -1;
    }

    public int getSongIndex() {
        if(musicSrv != null) return musicSrv.getSongIndex();
        return -1;
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    public void stop() {
        destroy();
    }

    public Song get(int index) {
        if(index < 0 || index >= songs.size()) return null;
        return songs.get(index);
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    public void select(String song) {
        if(!musicBound) {
            init();
            waitingMethod = WAITING_LISTEN;
            savedParam = song;

            return;
        }

        int i = -1;
        for(int index = 0; index < songs.size(); index++) {
            if(songs.get(index).getTitle().equals(song)) i = index;
        }

        if(i == -1) {
            return;
        }

        musicSrv.setSong(i);
        musicSrv.playSong();
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }
}
