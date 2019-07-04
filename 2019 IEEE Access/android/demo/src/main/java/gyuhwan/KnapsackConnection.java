package gyuhwan;

import android.util.Log;
import android.util.Pair;

import java.util.PriorityQueue;
import java.util.Vector;

public class KnapsackConnection {
    final int INF = 1987654321;
    private int requestVideoNum;
    private int currentRequestSegmentIdx;
    private long totalTime;
    private double powerLimit;
    private double totalUsedPower;
    private double totalSSIM;
    private int prevSelectedVersion;
    videoInfo currentVideo;
    double dp_table[][][];
    boolean visited[][][];
    int parentResolution[][][];
    int parentPower[][][];
    int versionSet[];
    public KnapsackConnection(int _requestVideoNum, double _powerRate) {
        requestVideoNum = _requestVideoNum;
        currentRequestSegmentIdx = 0;
        totalUsedPower = 0;
        totalSSIM = 0;
        totalTime=0;
        currentVideo=VideoManager.getInstance().getVideoInfo(requestVideoNum);
        versionSet=new int[currentVideo.getNumberOfSegment()];
        Log.d("DEBUGYU-Connection","Power Rate : "+_powerRate);
        powerLimit = currentVideo.getTotalPower()*(_powerRate/100);
        execute();
    }

    public void initTable(){
        dp_table=new double[(int)powerLimit][currentVideo.getNumberOfSegment()][currentVideo.getNumberOfVersion()];
        visited=new boolean[(int)powerLimit][currentVideo.getNumberOfSegment()][currentVideo.getNumberOfVersion()];
        parentResolution=new int[(int)powerLimit][currentVideo.getNumberOfSegment()][currentVideo.getNumberOfVersion()];
        parentPower=new int[(int)powerLimit][currentVideo.getNumberOfSegment()][currentVideo.getNumberOfVersion()];
    }

    public double getMaxSSIMValue(int consumed_power, int frame, int resolution){
        if (consumed_power > powerLimit)
            return -INF;
        if (frame == currentVideo.getNumberOfSegment() - 1) return currentVideo.getPowerOfSegmentVersion( frame, resolution);
        double ret = dp_table[consumed_power][frame][resolution];
        boolean isVisited = visited[consumed_power][frame][resolution];
        if (isVisited) return ret;
        visited[consumed_power][frame][resolution]=true;
        dp_table[consumed_power][frame][resolution] = currentVideo.getSSIMOfSegmentVersion(frame,resolution);
        double maxValue = 0;
        for (int r = 0; r <= 4; r++) {
            int next_power = consumed_power + (int)currentVideo.getPowerOfSegmentVersion(frame +1, resolution);
            double value = getMaxSSIMValue(next_power, frame + 1, r);
            if (maxValue < value) {
                maxValue = value;
                parentResolution[consumed_power][frame][resolution]= r;
                parentPower[consumed_power][frame][resolution] = next_power;
            }
        }
        return dp_table[consumed_power][frame][resolution] += maxValue;
    }

    public int requestCurrentVideoSegmentVersion() {
        if(currentRequestSegmentIdx>=currentVideo.getNumberOfSegment()){ return 0; }
        int current_resolution = versionSet[currentRequestSegmentIdx++];
        return current_resolution;
    }

    public void execute(){
        Log.d("DEBUGYU-TIME-TOTAL-DP","execute : "+currentRequestSegmentIdx);
        long start=System.nanoTime();
        Log.d("DEBUGYU_TIME","start = "+start);
        initTable();

        getMaxSSIMValue((int)totalUsedPower,currentRequestSegmentIdx,0);
        long end=System.nanoTime();
        Log.d("DEBUGYU_TIME","end = "+end);
        Log.d("DEBUGYU_TIME_ms","total = "+(end-start)+"ms");
        int currentPower = parentPower[(int)totalUsedPower][currentRequestSegmentIdx][0];
        int currentResolution = parentResolution[(int)totalUsedPower][currentRequestSegmentIdx][0];
        versionSet[currentRequestSegmentIdx]=currentResolution;
        for(int i=currentRequestSegmentIdx+1;i<currentVideo.getNumberOfSegment();i++){
            int currentVersion = parentResolution[currentPower][i][currentResolution];
            currentPower =  parentPower[currentPower][i][currentResolution];
            currentResolution = currentVersion;
            versionSet[i]= currentVersion;
        }
        totalTime+=(end-start);
    }

    public void setPrevSelectVesion(int ver) {
        if(currentRequestSegmentIdx==(currentVideo.getNumberOfSegment()-1)){
            Log.d("DEBUGYU-TIME-TOTAL-DP"," video " + currentVideo.getVideoName()+" total time = "+totalTime);
        }
        prevSelectedVersion = currentVideo.getNumberOfVersion()-1 - ver;
        totalUsedPower += currentVideo.getPowerOfSegmentVersion(currentRequestSegmentIdx - 1, prevSelectedVersion);
        totalSSIM += currentVideo.getSSIMOfSegmentVersion(currentRequestSegmentIdx - 1, prevSelectedVersion);
    }
}
