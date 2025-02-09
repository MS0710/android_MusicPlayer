package com.example.musicplayer_5;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerService extends Service {

    private String TAG = "MediaPlayerService";

    MyBinder myBinder = new MyBinder();
    MediaPlayer mediaPlayer = null;
    static MediaPlayer mlastPlayer;
    String path = "";
    int position = 0;
    int total_music_cunt = 0;

    NotificationManager notiMgr;
    Notification.Builder noti;
    private int id = 100;
    private static final String CHANNEL_ID="channel_id";
    public static final String  CHANEL_NAME="chanel_name";
    private static final String NOTIFICATION_BUTTON_PLAY= "notification_button_play";
    private static final String NOTIFICATION_BUTTON_PRE= "notification_button_pre";
    private static final String NOTIFICATION_BUTTON_NEXT= "notification_button_next";

    private static final String UPDATE_INFO = "update_info";

    private List<Song> list;

    public MediaPlayerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        if (myBinder != null){
            return myBinder;
        }
        return null;
    }

    public class MyBinder extends Binder {

        public MediaPlayerService getService(){
            return MediaPlayerService.this;
        }

        public void play(){
            if (mediaPlayer !=null){
                mediaPlayer.start();
                updateNotification();
            }
        }
        public void pause(){
            if(mediaPlayer!=null && mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                updateNotification();
            }
        }

        public boolean isplaying(){
            if (mediaPlayer.isPlaying()){
                return true;
            }else {
                return false;
            }
        }

        public void change_song(int now_position){
            prepare(list.get(now_position).path);
            mediaPlayer.start();
            position = now_position;
            updateNotification();
        }

        public void seekTo(int time){
            mediaPlayer.seekTo(time);
        }

        public int get_CurrentPosition(){
            return mediaPlayer.getCurrentPosition();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        myBinder = new MyBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        list = new ArrayList<Song>();
        list = MusicUtils.getMusicData(this);
        position = intent.getIntExtra("position",0);
        total_music_cunt = intent.getIntExtra("total_music_cunt",0);;
        path = list.get(position).path;
        if (mlastPlayer == null){
            prepare(path);
            mediaPlayer.start();
            updateNotification();
        }else{
            mediaPlayer = mlastPlayer;
        }

        IntentFilter fliter = new IntentFilter();
        fliter.addAction(NOTIFICATION_BUTTON_PLAY);
        fliter.addAction(NOTIFICATION_BUTTON_PRE);
        fliter.addAction(NOTIFICATION_BUTTON_NEXT);
        registerReceiver(onClickReceiver,fliter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(mediaPlayer!=null&&mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;
        }
        CleanNotification();
        super.onDestroy();
    }

    void prepare(String path){
        mediaPlayer = new MediaPlayer();
        if (mlastPlayer !=null){
            mlastPlayer.stop();
            mlastPlayer.release();
        }
        mlastPlayer = mediaPlayer;
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateNotification(){
        notiMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channaltest = null;
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            channaltest = new NotificationChannel(CHANNEL_ID,CHANEL_NAME,NotificationManager.IMPORTANCE_DEFAULT);
            channaltest.enableLights(true);
            channaltest.setLightColor(Color.GREEN);
            channaltest.setShowBadge(false);
            notiMgr.createNotificationChannel(channaltest);
        }

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            noti = new Notification.Builder(this,CHANNEL_ID);
            noti.setSmallIcon(android.R.drawable.btn_star_big_on);
            RemoteViews rv = new RemoteViews(getPackageName(),R.layout.notification_layout);
            Intent intent = new Intent(NOTIFICATION_BUTTON_PLAY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,intent,0);
            Intent intent_pre = new Intent(NOTIFICATION_BUTTON_PRE);
            PendingIntent pendingIntent_pre = PendingIntent.getBroadcast(this,0,intent_pre,0);
            Intent intent_next = new Intent(NOTIFICATION_BUTTON_NEXT);
            PendingIntent pendingIntent_next = PendingIntent.getBroadcast(this,0,intent_next,0);
            rv.setOnClickPendingIntent(R.id.btn_noti_play,pendingIntent);
            rv.setOnClickPendingIntent(R.id.btn_noti_pre,pendingIntent_pre);
            rv.setOnClickPendingIntent(R.id.btn_noti_next,pendingIntent_next);
            if (mediaPlayer.isPlaying()){
                rv.setImageViewResource(R.id.btn_noti_play,R.drawable.pause);
            }else {
                rv.setImageViewResource(R.id.btn_noti_play,R.drawable.play);
            }
            rv.setImageViewResource(R.id.btn_noti_pre,R.drawable.pre);
            rv.setImageViewResource(R.id.btn_noti_next,R.drawable.next);
            rv.setTextViewText(R.id.txt_noti_SongName,list.get(position).song);
            noti.setContent(rv);
            notiMgr.notify(id,noti.build());
        }
    }

    public void CleanNotification(){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.deleteNotificationChannel(CHANNEL_ID);
        }
    }

    BroadcastReceiver onClickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NOTIFICATION_BUTTON_PLAY) && intent!=null){
                Log.d(TAG, "onReceive: NOTIFICATION_BUTTON_PLAY");
                if (!mediaPlayer.isPlaying()){
                    mediaPlayer.start();
                }else {
                    mediaPlayer.pause();
                }
            }
            if (intent.getAction().equals(NOTIFICATION_BUTTON_PRE) && intent!=null){
                Log.d(TAG, "onReceive: NOTIFICATION_BUTTON_PRE");
                if (position == 0){
                    position = total_music_cunt;
                    prepare(list.get(position).path);
                    mediaPlayer.start();
                }else {
                    position--;
                    prepare(list.get(position).path);
                    mediaPlayer.start();
                }
            }
            if (intent.getAction().equals(NOTIFICATION_BUTTON_NEXT) && intent!=null){
                Log.d(TAG, "onReceive: NOTIFICATION_BUTTON_NEXT");
                if (position == total_music_cunt){
                    position = 0;
                    prepare(list.get(position).path);
                    mediaPlayer.start();
                }else {
                    position++;
                    prepare(list.get(position).path);
                    mediaPlayer.start();
                }
            }
            updateNotification();
            Intent intent_sentBroadcast = new Intent();
            intent_sentBroadcast.setAction(UPDATE_INFO);
            intent_sentBroadcast.putExtra("position",position);
            sendBroadcast(intent_sentBroadcast);

        }
    };
}
