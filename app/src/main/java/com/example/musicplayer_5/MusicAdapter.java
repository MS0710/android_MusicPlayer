package com.example.musicplayer_5;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class MusicAdapter extends BaseAdapter {
    private Context context;
    private List<Song> list;

    public MusicAdapter(Context context,List<Song> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if (view == null){
            viewHolder = new ViewHolder();
            view = View.inflate(context,R.layout.music_list,null);
            viewHolder.txt_songName = (TextView)view.findViewById(R.id.txt_songName);
            viewHolder.textMusicSinger = (TextView) view.findViewById(R.id.music_item_singer);
            viewHolder.textMusicTime = (TextView) view.findViewById(R.id.music_item_time);
            view.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.txt_songName.setText(list.get(i).song.toString());
        //viewHolder.textMusicSinger.setText(list.get(i).singer.toString());
        viewHolder.textMusicTime.setText(toTime(list.get(i).duration));
        return view;
    }

    class ViewHolder{
        TextView txt_songName;
        TextView textMusicSinger;
        TextView textMusicTime;
    }

    public String toTime(int time) {
        time /= 1000;
        int minute = time / 60;
        int hour = minute / 60;
        int second = time % 60;
        minute %= 60;
        return String.format("%02d:%02d", minute, second);
    }
}
