package gyuhwan;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by erslab-gh on 2018-11-09.
 */

public class videoInfo {

    private String videoName;
    private int numberOfSegment;
    private int numberOfVersion;
    private Vector<Vector<Double> > powerTable;
    private Vector<Vector<Double> > ssimTable;
    private double totalPower;


    public videoInfo(String videoName) {
        this.videoName = videoName;
        powerTable=new Vector<>();
        ssimTable=new Vector<>();
        read();
    }

    void setTotalPower() {
        totalPower = 0;
        for (int i = 0; i < numberOfSegment; i++) {
            totalPower += powerTable.get(i).get(numberOfVersion - 1);
        }
    }

    double getTotalPower() {
        return totalPower;
    }

    void read() {
        //	cout << videoName << endl;
        String power_filename = videoName + "_power_table.txt";
        String ssim_filename = videoName + "_ssim_table.txt";
        read(power_filename, powerTable);
        read(ssim_filename, ssimTable);
        setTotalPower();
    }

    void read(String fileUrl, Vector<Vector<Double> > ret){
        readThread readThread=new readThread(fileUrl,ret);
        readThread.start();

        try {
            readThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class readThread extends Thread{
        private String fileUrl="";
        Vector<Vector<Double> > vec;

        readThread(){
            fileUrl="";
        }
        readThread(String fileUrl, Vector<Vector<Double> > ret){
            this.fileUrl=fileUrl;
            vec=ret;
        }
        @Override
        public void run() {
            readTable(fileUrl,vec);
        }
    }
    void readTable(String name, Vector<Vector<Double> > ret) {
        //	cout << name << endl;
        int cnt=0;
        String txtpath = "http://165.246.43.96/v1/table/"+name;
        Log.d("DEBUGYU","path : "+txtpath);
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(txtpath);
        try {
            HttpResponse httpResponse = httpClient.execute(getRequest);
            if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                Log.d("DEBUGYU","SC NOT OK ");
            } else {
                InputStream inputStream = httpResponse.getEntity().getContent();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String temp="";
                    Vector<Double> vec=new Vector<Double>();
                    for(int i=0;i<=line.length();i++){
                        if(i==line.length() || line.charAt(i)==' ' || line.charAt(i)=='\t'){
                            if(temp.isEmpty()) continue;
                            Log.d("DEBUGYU","value : "+temp);
                            vec.add(Double.parseDouble(temp));
                            temp="";
                        }
                        else{
                            temp+=line.charAt(i);
                        }
                    }
                    ret.add(vec);
                    cnt++;
                }
                inputStream.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }/*
        for(int i=0;i<KnapSackOutputList.size();i++){
            Log.d("DDDDDD","index "+i+" : "+KnapSackOutputList.get(i));
        }
        ifstream infile;
        infile.open(name);
        int cnt = 0;
        if (!infile) {
            cout << "could not open file" << endl;
            return;
        }
        while (!infile.eof()) {
            ret.add(new Vector<Double>());
            .push_back(vector<double>(5));
            for (int r = 0; r < 5; r++) {
                infile >> ret.get(cnt).add()
                //	cout << ret[cnt][r] << ' ';
            }
            cnt++;
        }*/
        for(int i=0;i<ret.size();i++){
            for(int j=0;j<ret.get(i).size();j++){
                Log.d("DEBUGYU",""+i+","+j+" : "+ret.get(i).get(j));
            }
        }
        numberOfSegment = cnt;
        numberOfVersion = 5;
    }
    final String getVideoName() {return videoName;}
	final int getNumberOfSegment() {
        return numberOfSegment;
    }
    final int getNumberOfVersion() {
        return numberOfVersion;
    }
    final double getPowerOfSegmentVersion(int seg, int ver) {
        return powerTable.get(seg).get(ver);
    }
    final double getSSIMOfSegmentVersion(int seg, int ver) {
        return ssimTable.get(seg).get(ver);
    }
}
