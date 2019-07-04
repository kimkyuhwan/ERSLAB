/*
	Last Update : 2018-11-09
	Writer : GyuHwan Kim

	wireless power monitoring (wi-fi)이 가능해야함.
	클라이언트는 서버로 현재 소모된 총 power값, Power Limit, 현재 video, segment idx를 보내줘야 함.
	limit_power - total_used_power => restPower

	NodeJS로 구현.
	room으로 소켓 통신할 경우 소모된 power만 보내면 됨

	현재 코드에서 구현할 내용은
	Greedy Algorithm
	video, segment, version => Power[video][segment][ver], SSIM[video][segment][ver];
	SSIM High, Power Lowa
	Idx, Power, SSIM
	pq안에 현재 받아야 할 idx부터 마지막 idx까지 넣으면 됨.

	SSIM_Table과 Power_Table은 makeRoom에서 구해놓도록 설정해놓을 것임.

*/

#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <algorithm>
#include <queue>
#include <ctime>
using namespace std;

#define NUMBER_OF_VIDEO 5
#define NUMBER_OF_STATE 4
#define NUMBER_OF_VERSION 5

#define STATE_LOW 0
#define STATE_MEDIUM 1
#define STATE_HIGH 2
#define STATE_RANDOM 3

#define BSA_H 0
#define BSA_DP 1 
#define DASH 2

double ENERGY_RATE=0.80;
// 100 ~ 500
// 500 ~ 3000
// 3000 ~ 5000
const int BASE_BITRATE[3] = {100, 500, 3000};
const int BITRATE_RANGE[3] = {40000, 250000, 200000};

int state = STATE_HIGH;

string filename[NUMBER_OF_VIDEO] = { "motion","car","oops","dance","moving"};

typedef pair< pair<double, double>, pair<int, int> > pq_data; // (SSIM/power) , power, seg,ver

pq_data makeData(double R, double power, int seg, int ver) {
	return make_pair(make_pair(R, power), make_pair(seg, ver));
}

class videoInfo {
private:
	string videoName;
	int numberOfSegment;
	int numberOfVersion;
	vector<vector<double> > powerTable;
	vector<vector<double> > ssimTable;
	vector<vector<double> > bitrateTable;
	double totalPower;

public:
	videoInfo() {

	}

	videoInfo(string videoName) {
		this->videoName = videoName;
		read();
	}

	void setTotalPower() {
		totalPower = 0;
		for (int i = 0; i < numberOfSegment; i++) {
			int maxPowerVersion = 0;
			for (int j = 1; j < numberOfVersion; j++) {
				if (powerTable[i][maxPowerVersion] < powerTable[i][j]) {
					maxPowerVersion = j;
				}
			}
			totalPower += powerTable[i][maxPowerVersion];
		}
	}

	double getTotalPower() {
		return totalPower;
	}

	void read() {
	//	cout << videoName << endl;
		string power_filename = videoName + "_power_table.txt";
		string ssim_filename = videoName + "_ssim_table.txt";
		string bitrate_filename = videoName + "_bitrate_table.txt";
		readTable(power_filename, powerTable);
		readTable(ssim_filename, ssimTable);
		readTable(bitrate_filename, bitrateTable);
		setTotalPower();
	}
	void readTable(string name, vector<vector<double> > &ret) {
	//	cout << name << endl;
		ifstream infile;
		infile.open(name);
		int cnt = 0;
		if (!infile) {
			cout << "could not open file" << endl;
			return;
		}
		while (!infile.eof()) {
			ret.push_back(vector<double>(5));
			for (int r = 0; r < 5; r++) {
				infile >> ret[cnt][r];
			//	cout << ret[cnt][r] << ' ';
			}
			sort(ret[cnt].begin(), ret[cnt].end());
			cnt++;
		}
		numberOfSegment = cnt;
		numberOfVersion = 5;
	}
	const int getNumberOfSegment() {
		return numberOfSegment;
	}
	const int getNumberOfVersion() {
		return numberOfVersion;
	}
	const double getPowerOfSegmentVersion(int seg, int ver) {
		return powerTable[seg][ver];
	}
	const double getSSIMOfSegmentVersion(int seg, int ver) {
		return ssimTable[seg][ver];
	}
	const double getBitrateOfSegmentVersion(int seg, int ver) {
		return bitrateTable[seg][ver];
	}
};

vector<videoInfo> videos;
class Connection {
private:
	int requestVideoNum;
	int currentRequestSegmentIdx;
	double powerLimit;
	double totalUsedPower;
	double totalSSIM;
	int prevSelectedVersion;
	int algoType;
	vector<pair<double, int> > result;
	videoInfo currentVideo;
public:
	Connection(int _requestVideoNum, double _powerLimit,int _algoType) {
		requestVideoNum = _requestVideoNum;
		currentVideo = videos[requestVideoNum];
		powerLimit = _powerLimit;
		currentRequestSegmentIdx = 0;
		totalUsedPower = 0;
		totalSSIM = 0;
		algoType = _algoType;
	}
	int greedyAlgorithm() {
		vector<int> versionSet;
		priority_queue<pq_data> pq;

		int nSeg = currentVideo.getNumberOfSegment() - currentRequestSegmentIdx;
		int nVersion = currentVideo.getNumberOfVersion();
		versionSet.resize(nSeg, 0);

		double currentPower = totalUsedPower;
		double currentSSIM = 0;

		for (int seg = 0; seg<nSeg; seg++) {
			currentPower += currentVideo.getPowerOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
			currentSSIM += currentVideo.getSSIMOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
		}
		for (int seg = 0; seg<nSeg; seg++) {
			double basic_power= currentVideo.getPowerOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
			double basic_ssim = currentVideo.getSSIMOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
			for (int ver = 1; ver<nVersion; ver++) {
				
				double power = currentVideo.getPowerOfSegmentVersion(currentRequestSegmentIdx + seg, ver);
				double ssim = currentVideo.getSSIMOfSegmentVersion(currentRequestSegmentIdx + seg, ver);
				
				double r = (ssim-basic_ssim) / (power-basic_power);
			
				if(r>0)
					pq.push(makeData(r, (power - basic_power), seg, ver));
			}
		}
		while (!pq.empty() && currentPower<powerLimit) {
			pq_data curData = pq.top();
			pq.pop();
			double R = curData.first.first;
			double power = curData.first.second;
			
			int SegmentIdx = curData.second.first;
			int versionIdx = curData.second.second;
			double basic_power = currentVideo.getPowerOfSegmentVersion(currentRequestSegmentIdx + SegmentIdx, 0);
			double basic_ssim = currentVideo.getSSIMOfSegmentVersion(currentRequestSegmentIdx + SegmentIdx, 0);
			double SSIM = R * power;
			power += basic_power;

			SSIM += basic_ssim;
			double selectPower = currentVideo.getPowerOfSegmentVersion(SegmentIdx + currentRequestSegmentIdx, versionSet[SegmentIdx]);
			double selectSSIM = currentVideo.getSSIMOfSegmentVersion(SegmentIdx + currentRequestSegmentIdx, versionSet[SegmentIdx]);

			double nextPower = currentPower + power - selectPower;
			double nextSSIM = currentSSIM + SSIM - selectSSIM;

			if (selectSSIM < SSIM &&  nextPower <= powerLimit) {
				double selectSSIM = currentVideo.getSSIMOfSegmentVersion(SegmentIdx + currentRequestSegmentIdx, versionSet[SegmentIdx]);
				versionSet[SegmentIdx] = versionIdx;
				currentPower = nextPower;
				currentSSIM = nextSSIM;
			}

		}
		return versionSet.front();
	}

	int maximumVersion() {
		return NUMBER_OF_VERSION - 1;
	}


	int requestCurrentVideoSegmentVersion() {
		if (algoType == BSA_H) {
			return greedyAlgorithm();
		}
		else if (algoType == BSA_DP) {
			return 0;
		}
		else if (algoType == DASH) {
			return maximumVersion();
		}
	}

	void setPrevSelectVesion(int ver) {
		prevSelectedVersion = ver;
		totalUsedPower += videos[requestVideoNum].getPowerOfSegmentVersion(currentRequestSegmentIdx, prevSelectedVersion);
		totalSSIM += videos[requestVideoNum].getSSIMOfSegmentVersion(currentRequestSegmentIdx, prevSelectedVersion);
		currentRequestSegmentIdx++;
	}
	void print() {
		puts("Select Version");
		for (int i = 0; i < result.size(); i++) {
			cout << result[i].second << "	";
		}
		puts("");
		puts("Bitrate");
		for (int i = 0; i < result.size(); i++) {
			cout << result[i].first << "	";
		}
		puts("");

		cout << "Power Limit : " << powerLimit << endl;
		cout << "Total Used Power : " << totalUsedPower << endl;
		cout << "Total SSIM : " << totalSSIM << endl;
	}

	double getBitrate(int seg, int ver) {
		return currentVideo.getBitrateOfSegmentVersion(seg, ver);
	}
	int selectVersionByBitrate(int currentVer, double bitrate) {
		int selectVersion = 0;
		for (int ver = 0; ver <= currentVer; ver++) {
			if (getBitrate(currentRequestSegmentIdx,ver) <= bitrate) {
				selectVersion = ver;
			}
		}
		result.push_back({ bitrate,selectVersion });
		return selectVersion;
	}
};

void init() {
	for (int i = 0; i<NUMBER_OF_VIDEO; i++) {
		videos.push_back(videoInfo(filename[i]));
	}
}

double getRandomBitrate() {
	return (double)((rand()*rand()) % BITRATE_RANGE[state]) / 100 + BASE_BITRATE[state];
}
//  ~ 200, 200 
int main() {
	init();

	srand(time(0));

	clock_t start, end;
	
	for (int v = 0; v < NUMBER_OF_VIDEO; v++) {
		int current_video = v;
		for (int s = 0; s < NUMBER_OF_STATE; s++) {
			
			ENERGY_RATE = 0.80;
			vector<Connection> vec;
			for (int i = 0; i < 4; i++) {
				ENERGY_RATE += 0.05;
				Connection greedyConnection = Connection(current_video, videos[current_video].getTotalPower()*ENERGY_RATE, BSA_H);
				
				state = s;
				string _filename = filename[v] + "_ver_State2_" + to_string(s) + "_"+to_string(i)+".txt";
				freopen(_filename.c_str(), "w", stdout);
				start = clock();
				for (int i = 0; i < videos[current_video].getNumberOfSegment(); i++) {
					if (s == STATE_RANDOM) {
						state = rand() % 3;
					}
					double bitrate = getRandomBitrate();
					
					int version = greedyConnection.requestCurrentVideoSegmentVersion();
					int selectVersion = greedyConnection.selectVersionByBitrate(version, bitrate);
					greedyConnection.setPrevSelectVesion(selectVersion);
					//printf("segment : %d, selectVersion %d\n", i, selectVersion);
				}
				greedyConnection.print();
				end = clock();
				cout << end - start << "ms" << endl;
			}
		//	Connection bitrateConnection = Connection(current_video, videos[current_video].getTotalPower()*ENERGY_RATE, DASH);
		//	vec.push_back(bitrateConnection);
			
			
			
		}
	}
	
	puts("AA");
}