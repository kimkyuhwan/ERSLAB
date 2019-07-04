package gyuhwan;

import android.util.Log;

/**
 * Created by erslab-gh on 2017-09-29.
 */

public class PlayerClock {
    String TAG="player-clock";
    private static PlayerClock playerClock=new PlayerClock();
    private long frame;
    private long prevframe;
    private long maxBufferTime;
    private String ssimStd="0.99";
    private String selectFileName ="";
    private int selectKnapsackEnergyRate= 100; // 100%
    private float frameRate=0.0f;
    private boolean isErsMode=false;
    private int playMode=0;
    private int auto_count=0;
    private int addFrame=0;
    private int video_idx=0;

    public int getVideo_idx() {
        return video_idx;
    }

    public void setVideo_idx(int video_idx) {
        this.video_idx = video_idx;
    }

    public int getAuto_count() {
        return auto_count;
    }

    public void addAutoCount(){
        auto_count++;
    }

    public static PlayerClock getInstance(){
        return playerClock;
    }


    private int macro_video_index=-1;
    private int macro_mode_index=-1;
    private PlayerClock(){
        frame=0;
    }

    public int getMacro_video_index() {
        return macro_video_index;
    }

    public void setMacro_video_index(int macro_video_index) {
        this.macro_video_index = macro_video_index;
    }

    public void setMacro_mode_index(int macro_mode_index) {
        this.macro_mode_index = macro_mode_index;
    }

    public int getMacro_mode_index() {
        return macro_mode_index;
    }

    private static final int MODE_ERS=5;
    private static final int MODE_DASH=6;

    public int getAddFrame() {
        return addFrame;
    }

    public void setAddFrame(int addFrame) {
        this.addFrame = addFrame;
    }

    public long getFrame(){
        return frame;
    }
    public void setFrame(long frame){
        this.frame=frame;
        long loadingBufferFrame=this.frame-this.prevframe;
        long maxBufferFrame=(long)(((float)maxBufferTime/1000)*frameRate);
        Log.d(TAG,"Frame : "+frame+", loading Buffer Size : "+loadingBufferFrame+", maxBufferFrame : "+maxBufferFrame);
        this.prevframe=frame;
        if(prevframe!=0) this.frame+=(maxBufferFrame-loadingBufferFrame);
    }


    public int selectPlayMode(){

        if(Constants.PLAY_MODE_STATE==Constants.MODE_AUTO) {
            macro_mode_index++;
        }

        if(macro_mode_index<MODE_ERS) return 0;
        else if(macro_mode_index==MODE_ERS) return 1;
        else if(macro_mode_index==MODE_DASH) return 2;

        if(Constants.PLAY_MODE_STATE==Constants.MODE_AUTO) {
            macro_mode_index = -1;
            macro_video_index++;
            if(macro_video_index==6){
                macro_video_index=0;
                addAutoCount();
            }
        }
        return -1;
    }

    public int getPlayMode() {
        return playMode;
    }

    public void setPlayMode(int playMode) {
        this.playMode = playMode;
    }

    public void setPrevframe(long prevframe) {
        this.prevframe = prevframe;
    }

    public void InitPrevframe() {
        setPrevframe(0);
    }

    public void setMaxBufferTime(long maxBufferTime) {
        this.maxBufferTime = maxBufferTime;
    }

    public String getSsimStd() {
        return ssimStd;
    }

    public void setSsimStd(String ssimStd) {
        this.ssimStd = ssimStd;
    }

    public String getSelectFileName() {
        return selectFileName;
    }

    public String getKnapsackSelectFileName() {
        return selectFileName;
    }

    public void setSSIMSelectFileName(String selectFileName) {
        this.selectFileName = selectFileName+ssimStd+".txt";
        Log.d(TAG,"selectFileName : ssim ::"+selectFileName);
    }

    public int getSelectKnapsackEnergyRate() {
        return selectKnapsackEnergyRate;
    }

    public void setSelectKnapsackEnergyRate(String selectKnapsackEnergyRate) {
        this.selectKnapsackEnergyRate = Integer.parseInt(selectKnapsackEnergyRate);
    }

    public void setSelectKnapsackEnergyRate(int selectKnapsackEnergyRate) {
        this.selectKnapsackEnergyRate = selectKnapsackEnergyRate;
    }

    public void setKnapsackSelectFileName(String selectFileName) {
        this.selectFileName = "knapsack_"+selectFileName+"_"+selectKnapsackEnergyRate+".txt";
        Log.d(TAG,"selectFileName : knapsack ::"+"knapsack_"+selectFileName+"_"+selectKnapsackEnergyRate+".txt");
    }

    public float getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(float frameRate) {
        this.frameRate = frameRate;
        Log.d(TAG,"frameRate : "+frameRate);
    }

    public boolean isErsMode() {
        return isErsMode;
    }

    public void setErsMode(boolean ersMode) {
        isErsMode = ersMode;
    }
}
