package gyuhwan;

import android.util.Log;

import com.google.android.exoplayer2.util.SystemClock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Vector;

/**
 * Created by erslab-gh on 2018-11-09.
 */

class Connection {

    private int requestVideoNum;
    private int currentRequestSegmentIdx;
    private double powerLimit;
    private double totalUsedPower;
    private double totalSSIM;
    private long totalTime;

    private int prevSelectedVersion;
    private ArrayList<pq_data> sortedVec;
    private int versionSet[];
    videoInfo currentVideo;

    class pq_data implements Comparable<pq_data>{
        public double r, power;
        public int seg,ver;

        public pq_data(double r, double power, int seg, int ver) {
            this.r = r;
            this.power = power;
            this.seg = seg;
            this.ver = ver;
        }

        @Override
        public int compareTo(pq_data o) {
            if(this.r==o.r){
                if(this.power==o.power){
                    return (this.seg == o.seg) ? 0 : 1;
                }
                return this.power<o.power ? 0 : 1;
            }
            return this.r < o.r ? 0 : 1;
        }
    };

    public Connection(int _requestVideoNum, double _powerRate) {
        requestVideoNum = _requestVideoNum;
        currentRequestSegmentIdx = 0;
        totalUsedPower = 0;
        totalSSIM = 0;
        totalTime=0;
        currentVideo=VideoManager.getInstance().getVideoInfo(requestVideoNum);
        Log.d("DEBUGYU-Heuristic","Power Rate : "+_powerRate);
        powerLimit = currentVideo.getTotalPower()*(_powerRate/100);
        long start=System.nanoTime();
        initVersionSet();
        initVector();
        long end=System.nanoTime();
        Log.d("DEBUGYU-Heuristic","init total = "+(end-start));
        totalTime+=(end-start);
        execute();
        double minVersion=0;
        double maxVersion=0;
        for(int i=0;i<currentVideo.getNumberOfSegment();i++){
            minVersion+=currentVideo.getPowerOfSegmentVersion(i,0);
            maxVersion+=currentVideo.getPowerOfSegmentVersion(i,currentVideo.getNumberOfVersion()-1);
        }
        Log.d("DEBUGYU-HELL","min = "+minVersion +" , max = "+maxVersion);
    }

    public void initVersionSet(){
        int nSeg = currentVideo.getNumberOfSegment();
        versionSet=new int[nSeg];
    }

    public void initVector(){
        long start=System.nanoTime();
        sortedVec=new ArrayList<>();
        int nSeg = currentVideo.getNumberOfSegment();
        int nVersion = currentVideo.getNumberOfVersion();

        for (int seg = 0; seg<nSeg; seg++) {
            double basicPower = currentVideo.getPowerOfSegmentVersion(currentRequestSegmentIdx+seg, 0);
            double basicSSIM = currentVideo.getSSIMOfSegmentVersion(currentRequestSegmentIdx+seg, 0);
            for (int ver = 1; ver<nVersion; ver++) {
                double power = currentVideo.getPowerOfSegmentVersion(seg, ver);
                double ssim = currentVideo.getSSIMOfSegmentVersion( seg, ver);
                double r = (ssim-basicSSIM) / (power-basicPower);
                if(r>0) {
                    sortedVec.add(new pq_data(r, power, seg, ver));
                }
            }
        }
        Collections.sort(sortedVec, new Comparator<pq_data>() {
            @Override
            public int compare(pq_data pq_data, pq_data o) {
                if(pq_data.r==o.r){
                    if(pq_data.power==o.power){
                        return (pq_data.seg == o.seg) ? 0 : 1;
                    }
                    return pq_data.power<o.power ? 0 : 1;
                }
                return pq_data.r < o.r ? 0 : 1;
            }
        });
        long end=System.nanoTime();
        Log.d("DEBUGYU-Heuristic","init Priority total = "+(end-start));

    }

    public void execute(){
        Log.d("DEBUGYU-TIME-TOTAL-H","execute : "+currentRequestSegmentIdx);
        int position=currentRequestSegmentIdx;
        long start=System.nanoTime();

        int nSeg = currentVideo.getNumberOfSegment() - currentRequestSegmentIdx;
        ArrayList<pq_data> nextVec = new ArrayList<>();
        for(int i=position;i<nSeg;i++){
            versionSet[i]=0;
        }
        double currentPower = totalUsedPower;
        double currentSSIM = 0;
        for (int seg = 0; seg<nSeg; seg++) {
            currentPower += currentVideo.getPowerOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
            currentSSIM += currentVideo.getSSIMOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
        }

        int idx = 0;
        int cnt = sortedVec.size();
        while (idx<cnt) {
            pq_data curData = sortedVec.get(idx++);
            double R = curData.r;
            double power = curData.power;
            int SegmentIdx = curData.seg;
            int versionIdx = curData.ver;
            double SSIM = R * power;
            if(SegmentIdx>currentRequestSegmentIdx){
                nextVec.add(curData);
            }
            if(currentPower<powerLimit) {
                double selectPower = currentVideo.getPowerOfSegmentVersion(SegmentIdx , versionSet[SegmentIdx]);
                double selectSSIM = currentVideo.getSSIMOfSegmentVersion(SegmentIdx, versionSet[SegmentIdx]);

                double nextPower = currentPower + power - selectPower;
                double nextSSIM = currentSSIM + SSIM - selectSSIM;

                if (currentPower < nextPower && nextPower <= powerLimit) {
                    versionSet[SegmentIdx] = versionIdx;
                    currentPower = nextPower;
                    currentSSIM = nextSSIM;
                }
            }
        }
        for(int i=0;i<versionSet.length;i++){
            Log.d("DEBUGYU-VersionSet",i+" "+versionSet[i]);
        }
        Log.d("DEBUGYU-Connection","current Power = "+currentPower+" Power Limit : "+powerLimit);
        sortedVec=nextVec;
        long end=System.nanoTime();
        Log.d("DEBUGYU-Heuristic","execute total = "+(end-start));
        totalTime+=(end-start);

    }

    public int requestCurrentVideoSegmentVersion() {
        if(currentRequestSegmentIdx>=versionSet.length){
            return 0;
        }
        return versionSet[currentRequestSegmentIdx++];
    }

    public void setPrevSelectVesion(int ver) {
        if(currentRequestSegmentIdx==(currentVideo.getNumberOfSegment()-1)){
            Log.d("DEBUGYU-TIME-TOTAL-H"," video " + currentVideo.getVideoName()+" total time = "+totalTime);
        }
        prevSelectedVersion = currentVideo.getNumberOfVersion()-1 - ver;
        totalUsedPower += currentVideo.getPowerOfSegmentVersion(currentRequestSegmentIdx - 1, prevSelectedVersion);
        totalSSIM += currentVideo.getSSIMOfSegmentVersion(currentRequestSegmentIdx - 1, prevSelectedVersion);
        Log.d("DEBUGYU-DATA","version set = "+prevSelectedVersion+" totalUsedPower = "+totalUsedPower+", totalSSIM = "+totalSSIM);
    }
};
