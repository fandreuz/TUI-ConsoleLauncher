package ohi.andre.consolelauncher.managers.music;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.InputOutputReceiver;
import ohi.andre.consolelauncher.tuils.Tuils;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    public static final int NOTIFY_ID=100001;

    public static final String SONGTITLE_KEY = "songTitle";

    private MediaPlayer player;
    private List<Song> songs;
    private int songPosn;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle="";
    private boolean shuffle=false;
    private Random rand;

    public void onCreate(){
        super.onCreate();
        songPosn=0;
        rand=new Random();
        player = new MediaPlayer();
        initMusicPlayer();
    }

    public void initMusicPlayer(){
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(List<Song> theSongs){
        songs=theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return super.onUnbind(intent);
    }

    public String playSong(){
        try {
            player.reset();
        } catch (Exception e) {}

        Song playSong = songs.get(songPosn);

        long id = playSong.getID();
        if(id == -1) {
            String path = playSong.getPath();
            try {
                player.setDataSource(path);
            } catch (IOException e) {
                Tuils.log(e);
                Tuils.toFile(e);
                return null;
            }
        } else {
            songTitle=playSong.getTitle();
            long currSong = playSong.getID();
            Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
            try {
                player.setDataSource(getApplicationContext(), trackUri);
            }
            catch(Exception e) {
                Tuils.log(e);
                Tuils.toFile(e);
                return null;
            }
        }
        player.prepareAsync();

        return playSong.getTitle();
    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition()>0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        startForeground(NOTIFY_ID, buildNotification(this.getApplicationContext(), songTitle));
    }

    public static Notification buildNotification(Context context, String songTitle) {
        Intent notIntent = new Intent(context, LauncherActivity.class);
        PendingIntent pendInt = PendingIntent.getActivity(context, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification not;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(pendInt)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String label = "cmd";
            RemoteInput remoteInput = new RemoteInput.Builder(InputOutputReceiver.TEXT)
                    .setLabel(label)
                    .build();

            Intent i = new Intent(InputOutputReceiver.ACTION_CMD);
            i.putExtra(InputOutputReceiver.WAS_KEY, InputOutputReceiver.WAS_MUSIC_SERVICE);

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.mipmap.ic_launcher, label,
                    PendingIntent.getBroadcast(context.getApplicationContext(), 10, i, PendingIntent.FLAG_UPDATE_CURRENT))
                    .addRemoteInput(remoteInput)
                    .build();

            builder.addAction(action);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) not = builder.build();
        else not = builder.getNotification();

        return not;
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void stop() {
        try {
            player.stop();
        } catch (Exception e) {}

        try {
            player.release();
        } catch (Exception e) {}

        setSong(0);
    }

    public void playPlayer() {
        player.start();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    public String playPrev(){
        songPosn--;
        if(songPosn<0) songPosn=songs.size()-1;
        return playSong();
    }

    public String playNext(){
        if(shuffle){
            int newSong = songPosn;
            while(newSong==songPosn){
                newSong=rand.nextInt(songs.size());
            }
            songPosn=newSong;
        }
        else{
            songPosn++;
            if(songPosn>=songs.size()) songPosn=0;
        }

        return playSong();
    }

    public int getSongIndex() {
        return songPosn;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    public void setShuffle(boolean shuffle){
        this.shuffle = shuffle;
    }

}