package gyuhwan;

import android.content.Context;
import android.os.BatteryManager;
import android.util.Log;

import java.util.PriorityQueue;
import java.util.Vector;

/**
 * Created by erslab-gh on 2018-11-09.
 */

public class VideoManager {
    videoInfo[] videos;
    String[] filename={"motion","car","oops","dance","moving","skate"};
    static private VideoManager instance=new VideoManager();

    private static class VideoManagerHolder{
        public static final VideoManager INSTANCE = new VideoManager();
    }


    public static VideoManager getInstance(){
        return VideoManagerHolder.INSTANCE;
    }


    private VideoManager(){
        videos=new videoInfo[filename.length];
        for(int i=0;i<videos.length;i++){
            videos[i]=new videoInfo(filename[i]);
        }

    }

    public videoInfo getVideoInfo(int idx){
        return videos[idx];
    }

}
