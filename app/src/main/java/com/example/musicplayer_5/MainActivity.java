package com.example.musicplayer_5;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";

    private ListView lv_musiclist;
    private List<Song> list;
    private MusicAdapter musicAdapter;
    private LinearLayout lin_layout;
    private Button btn_pre,btn_play,btn_next;
    private SeekBar seekBar;
    private MyTextView txt_playing_name;

    private boolean service_status = false;
    private boolean layout_status = false;
    private int now_position = 0;
    private static final int UPDATE_UI = 0;
    private static final String UPDATE_INFO = "update_info";

    Intent serviceIntent;
    private MediaPlayerService.MyBinder musicControl;
    private MyBroadcastReceiver receiver;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case UPDATE_UI:
                    try {
                        seekBar.setProgress(musicControl.get_CurrentPosition());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    handler.sendEmptyMessageDelayed(UPDATE_UI,500);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
    }

    private void requestPermission(){
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else {
            initview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this, "權限確認", Toast.LENGTH_SHORT).show();
                    initview();
                }else {
                    Toast.makeText(MainActivity.this, "拒絕權限無法使用", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;

        }
    }

    private void initview(){
        lv_musiclist = (ListView)findViewById(R.id.lv_musiclist);
        list = new ArrayList<Song>();
        list = MusicUtils.getMusicData(this);
        musicAdapter = new MusicAdapter(this,list);
        lv_musiclist.setAdapter(musicAdapter);
        lv_musiclist.setOnItemClickListener(onItemClick);

        lin_layout = (LinearLayout)findViewById(R.id.lin_layout);

        btn_pre = (Button)findViewById(R.id.btn_pre);
        btn_play = (Button)findViewById(R.id.btn_play);
        btn_next = (Button)findViewById(R.id.btn_next);
        btn_pre.setOnClickListener(onClick);
        btn_play.setOnClickListener(onClick);
        btn_next.setOnClickListener(onClick);

        seekBar = (SeekBar)findViewById(R.id.seekbar);
        txt_playing_name = (MyTextView)findViewById(R.id.txt_playing_name);

        IntentFilter intent = new IntentFilter();
        intent.addAction(UPDATE_INFO);
        if (receiver == null){
            receiver = new MyBroadcastReceiver();
            registerReceiver(receiver,intent);
        }
    }

    private View.OnClickListener onClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btn_pre:
                    if (now_position == 0){
                        now_position = musicAdapter.getCount()-1;
                        musicControl.change_song(now_position);
                    }else {
                        now_position--;
                        musicControl.change_song(now_position);
                    }
                    seekBar.setMax(list.get(now_position).duration);
                    txt_playing_name.setText(list.get(now_position).song);
                    break;
                case R.id.btn_play:
                    if (musicControl.isplaying()){
                        musicControl.pause();
                        btn_play.setText("PLAY");
                    }else {
                        musicControl.play();
                        btn_play.setText("PAUSE");
                        handler.sendEmptyMessage(UPDATE_UI);
                    }
                    break;
                case R.id.btn_next:
                    if (now_position == (musicAdapter.getCount()-1)){
                        now_position = 0;
                        musicControl.change_song(now_position);
                    }else {
                        now_position++;
                        musicControl.change_song(now_position);
                    }
                    seekBar.setMax(list.get(now_position).duration);
                    txt_playing_name.setText(list.get(now_position).song);
                    break;

            }

        }
    };

    private AdapterView.OnItemClickListener onItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            Toast.makeText(MainActivity.this, "onItemClick", Toast.LENGTH_SHORT).show();
            Log.d(TAG, list.get(position).path);
            if (!layout_status){
                lin_layout.setVisibility(View.VISIBLE);
                layout_status = true;
            }
            if (!service_status){
                connectService(position);
            }else {
                musicControl.change_song(position);
            }
            now_position = position;
            seekBar.setMax(list.get(now_position).duration);
            seekBar.setOnSeekBarChangeListener(seekBarListener);
            handler.sendEmptyMessage(UPDATE_UI);
            btn_play.setText("PAUSE");
            txt_playing_name.setText(list.get(now_position).song);
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (b){
                musicControl.seekTo(i);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void connectService(int position){
        serviceIntent = new Intent(MainActivity.this,MediaPlayerService.class);
        serviceIntent.putExtra("position",position);
        serviceIntent.putExtra("total_music_cunt",(musicAdapter.getCount()-1));
        startService(serviceIntent);
        bindService(serviceIntent,mConnection,BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            musicControl = (MediaPlayerService.MyBinder) service;
            service_status = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicControl = null;
            service_status = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (musicControl != null) {
            handler.sendEmptyMessage(UPDATE_UI);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        if (mConnection != null){
            stopService(serviceIntent);
            unbindService(mConnection);
            service_status = false;
            Log.d(TAG, "DetailsActivity :unbindService ");
        }
        unregisterReceiver(receiver);
        layout_status = false;
        super.onDestroy();
    }

    public class MyBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UPDATE_INFO)){
                Toast.makeText(MainActivity.this, "UPDATE_INFO", Toast.LENGTH_SHORT).show();
                now_position = intent.getIntExtra("position",0);
                Log.d(TAG, "MyBroadcastReceiver:= "+now_position);
                seekBar.setMax(list.get(now_position).duration);
                txt_playing_name.setText(list.get(now_position).song);
                if (musicControl.isplaying()){
                    btn_play.setText("PAUSE");
                }else {
                    btn_play.setText("PLAY");
                }
            }
        }
    }
}
