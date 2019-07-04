package gyuhwan;

import android.os.Handler;
import android.provider.SyncStateContract;

import org.json.JSONException;
import org.json.JSONObject;

import static gyuhwan.Constants.PLAYMODE_KNAPSACK;

/**
 * Created by erslab-gh on 2017-12-12.
 */

public class HttpManager {
    private String serverFileName[]={"motion","car","oops","dance","moving","truck","hongkong","walking","football"};

    private static HttpManager Instance=new HttpManager();
    private HttpManager() {}
    public static HttpManager getInstance(){
        return Instance;
    }

   public void Start() throws JSONException {
        JSONObject send_data=new JSONObject();
        ReqHTTPJSONThread thread = new ReqHTTPJSONThread(Constants.REQ_START, send_data);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException interex) {
            interex.printStackTrace();
        }
        String result = thread.handler.getMsg();
        JSONObject result_data = null;
        int result_code = 0;
        try {
            result_data = new JSONObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private Runnable mRunnable ;
    public void End() throws JSONException {
        mRunnable=new Runnable() {
            @Override
            public void run() {

                if(PlayerClock.getInstance().getPlayMode()==PLAYMODE_KNAPSACK){
                    try {
                        Save();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else if(PlayerClock.getInstance().getMacro_mode_index()!=-1) {
                    try {
                        Save();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        JSONObject send_data=new JSONObject();
        ReqHTTPJSONThread thread = new ReqHTTPJSONThread(Constants.REQ_END, send_data);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException interex) {
            interex.printStackTrace();
        }
        String result = thread.handler.getMsg();
        JSONObject result_data = null;
        try {
            result_data = new JSONObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int result_code = 0;
        result_code=result_data.getInt("code");
        if (result_code == Constants.RESULT_CODE_SUCCESS) {
            Handler mHandler = new Handler();
            mHandler.postDelayed(mRunnable, 500);
        }
    }
    public void Save() throws JSONException {
        JSONObject send_data=new JSONObject();
        String savedata=serverFileName[PlayerClock.getInstance().getMacro_video_index()]+"_"+PlayerClock.getInstance().getMacro_mode_index()+"_"+PlayerClock.getInstance().getSsimStd()+PlayerClock.getInstance().getAuto_count();
        if(PlayerClock.getInstance().getPlayMode()==PLAYMODE_KNAPSACK){
            savedata=serverFileName[PlayerClock.getInstance().getMacro_video_index()]+"_"+PlayerClock.getInstance().getSelectKnapsackEnergyRate()+PlayerClock.getInstance().getAuto_count();
        }
        send_data.put("filename",savedata);
        ReqHTTPJSONThread thread = new ReqHTTPJSONThread(Constants.REQ_SAVE, send_data);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException interex) {
            interex.printStackTrace();
        }
        String result = thread.handler.getMsg();
        JSONObject result_data = null;

        try {
            result_data = new JSONObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
