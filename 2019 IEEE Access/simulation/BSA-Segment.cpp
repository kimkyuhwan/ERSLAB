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

#define NUMBER_OF_VIDEO 6
#define NUMBER_OF_STATE 4
#define NUMBER_OF_VERSION 5

#define STATE_LOW 0
#define STATE_MEDIUM 1
#define STATE_HIGH 2
#define STATE_RANDOM 3

#define BSA_H 0
#define BSA_DP 1 
#define DASH 2

#define INF 987654321

double ENERGY_RATE=0.80;
// 100 ~ 500
// 500 ~ 3000
// 3000 ~ 5000
const int BASE_BITRATE[3] = {100, 500, 3000};
const int BITRATE_RANGE[3] = {40000, 250000, 200000};

int state = STATE_HIGH;

string filename[NUMBER_OF_VIDEO] = { "motion","dance","moving", "hongkong","walking","football"};

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
		for (int i = 1; i < numberOfSegment; i++) {
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
		if (!infile) {
			cout << "could not open file" << endl;
			return;
		}
		int cnt = 1;
		ret.clear();
		ret.push_back(vector<double>(5));
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

double minSSIMValue[3][4][4]; // E_ratio, Network
double maxSSIMValue[3][4][4]; // E_ratio, Network
double totalSSIMValue[3][4][4]; // E_ratio, Network
double totalEnergy[3][4][4]; // E_ratio, Network
int cntSSIMValue[3][4][4];
int cntEnergy[3][4][4];

vector<videoInfo> videos;
class Connection {
private:
	int requestVideoNum;
	int currentRequestSegmentIdx;
	double powerLimit;
	double totalUsedPower;
	double totalSSIM;
	double worstSSIM;
	double bestSSIM;
	int prevSelectedVersion;
	int algoType;
	vector<pair<double, int> > result;
	videoInfo currentVideo;

	vector<vector<vector<double> > > dp_table;// [consumed_power][frame][resolution]; // resolution => 5, consumed_power, frame => each video diffent
	vector<vector<vector<int> > > visited; // Check if you visited dp table.
	vector<vector<vector<pair<int,int> > > > parent;

public:
	Connection(int _requestVideoNum, double _powerLimit,int _algoType) {
		requestVideoNum = _requestVideoNum;
		currentVideo = videos[requestVideoNum];
		powerLimit = _powerLimit;
		currentRequestSegmentIdx = 1;
		totalUsedPower = 0;
		totalSSIM = 0;
		worstSSIM = 1000;
		bestSSIM = 0;
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

	void initKnapsackTable() {
		dp_table.clear();
		visited.clear();
		parent.clear();
		int power = powerLimit+10;
		initTable(dp_table, power);
		initTable(visited, power);
		initTable(parent, power);
	}
	
	template <typename T>
	void initTable(vector<vector<vector<T> > > &vec, int size) {
		vec.resize(size);
		for (int i = 0; i < vec.size(); i++) {
			vec[i].resize(currentVideo.getNumberOfSegment());
			for (int j = 0; j < vec[i].size(); j++) {
				vec[i][j].resize(currentVideo.getNumberOfVersion() + 1);
			}
		}
	}

	double getMaxSSIMValue_Knapsack(int consumed_power, int frame, int resolution) {
 		if (consumed_power >= powerLimit )
			return -INF;
		if (frame == currentVideo.getNumberOfSegment() - 1) {
			return currentVideo.getSSIMOfSegmentVersion(frame , resolution);//ssim_table[frame][resolution];
		}
		double &ret = dp_table[consumed_power][frame][resolution];
		int &isVisited = visited[consumed_power][frame][resolution];
		if (isVisited) return ret;
		isVisited = true;
		ret = currentVideo.getSSIMOfSegmentVersion(frame, resolution); // ssim_table[frame][resolution];
		double maxValue = 0;
		for (int r = 0; r <= 4; r++) {
			int next_power = consumed_power + currentVideo.getPowerOfSegmentVersion(frame + 1, r); //power_table[frame + 1][r];
			double value = getMaxSSIMValue_Knapsack(next_power, frame + 1, r);
			if (maxValue < value) {
				maxValue = value;
				parent[consumed_power][frame][resolution].first = r;
				parent[consumed_power][frame][resolution].second = next_power;
			}
		}
		return ret += maxValue;
	}

	int knapsackAlgorithm() {
		initKnapsackTable();
		double ssim=getMaxSSIMValue_Knapsack(totalUsedPower, currentRequestSegmentIdx - 1, 0);
		int current_resolution = parent[totalUsedPower][currentRequestSegmentIdx-1][0].first;
		return current_resolution;
	}

	int maximumVersion() {
		return NUMBER_OF_VERSION - 1;
	}


	int requestCurrentVideoSegmentVersion() {
		if (algoType == BSA_H) {
			return greedyAlgorithm();
		}
		else if (algoType == BSA_DP) {
			return knapsackAlgorithm();
		}
		else if (algoType == DASH) {
			return maximumVersion();
		}
	}

	void setPrevSelectVesion(int ver) {
		prevSelectedVersion = ver;
		totalUsedPower += videos[requestVideoNum].getPowerOfSegmentVersion(currentRequestSegmentIdx, prevSelectedVersion);
		totalSSIM += videos[requestVideoNum].getSSIMOfSegmentVersion(currentRequestSegmentIdx, prevSelectedVersion);
		worstSSIM = min(worstSSIM, videos[requestVideoNum].getSSIMOfSegmentVersion(currentRequestSegmentIdx, prevSelectedVersion));
		bestSSIM = max(bestSSIM, videos[requestVideoNum].getSSIMOfSegmentVersion(currentRequestSegmentIdx, prevSelectedVersion));
		currentRequestSegmentIdx++;
	}
	void print(int algotype, int ratio, int network) {
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
		cout << "Average Energy : " << (totalUsedPower / 90) << endl;
		cout << "Total SSIM : " << totalSSIM << endl;
		cout << "Average SSIM : " << (totalSSIM / result.size()) << endl;
		cout << "Worst SSIM : " << worstSSIM << endl;
		cout << "Best SSIM : " << bestSSIM << endl;

		/*
		double minSSIMValue[4][4]; // E_ratio, Network
double maxSSIMValue[4][4]; // E_ratio, Network
double totalSSIMValue[4][4]; // E_ratio, Network
double totalEnergy[4][4]; // E_ratio, Network
		*/
		minSSIMValue[algotype][ratio][network] = min(minSSIMValue[algotype][ratio][network], worstSSIM);
		maxSSIMValue[algotype][ratio][network] = max(maxSSIMValue[algotype][ratio][network], bestSSIM);
		totalSSIMValue[algotype][ratio][network] += totalSSIM;
		cntSSIMValue[algotype][ratio][network] += result.size();
		totalEnergy[algotype][ratio][network] += totalUsedPower;
		cntEnergy[algotype][ratio][network] += 90;
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

	for (int i = 0; i < 3; i++) {
		for (int j = 0; j < 4; j++) {
			for (int k = 0; k < 4; k++)
				minSSIMValue[i][j][k] = 10000000;
		}
	}
}

void printSSIMTable() {

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
			
			vector<double> bitrate_set;
			int numOfSegment = videos[v].getNumberOfSegment();
			bitrate_set.resize(numOfSegment);
			state = s;
			for (int b = 0; b < numOfSegment; b++) {
				if (s == STATE_RANDOM) {
					state = rand() % 3;
				}
				bitrate_set[b] = getRandomBitrate();
			}
			for (int algoType = 1; algoType <=1; algoType++) {
				ENERGY_RATE = 0.80;
				for (int i = 0; i < 4; i++) {
					ENERGY_RATE += 0.05;
					Connection greedyConnection = Connection(current_video, videos[current_video].getTotalPower()*ENERGY_RATE, algoType);
					string _filename = "0314_2_"+filename[v]+"_"+to_string(algoType) + "_ver_State_" + to_string(s) + "_" + to_string(i) + ".txt";
					freopen(_filename.c_str(), "w", stdout);
					start = clock();
					for (int j = 0; j < videos[current_video].getNumberOfSegment()-1; j++) {

						double bitrate = bitrate_set[j];
						int version = greedyConnection.requestCurrentVideoSegmentVersion();
						int selectVersion = greedyConnection.selectVersionByBitrate(version, bitrate);
						greedyConnection.setPrevSelectVesion(selectVersion);
						//printf("segment : %d, selectVersion %d\n", i, selectVersion);
					}
					greedyConnection.print(algoType,i,s);
					end = clock();
					cout << end - start << "ms" << endl;
				}
			}
		//	Connection bitrateConnection = Connection(current_video, videos[current_video].getTotalPower()*ENERGY_RATE, DASH);
		//	vec.push_back(bitrateConnection);
			
			
			
		}
	}

	/*
	// TABLE 2
	freopen("input_table_2.txt", "w", stdout);
	for (int i = 0; i < 3; i++) {
		puts("BSA =+=!");
		int ratio = 85;
		for (int j = 0; j < 4; j++) {
			printf("%d\n", ratio);
			for (int k = 0; k < 4; k++) {
				printf("Average SSIM : %lf\n", (totalSSIMValue[i][j][k] / cntSSIMValue[i][j][k]));
				printf("Worst SSIM : %lf\n", minSSIMValue[i][j][k]);
				printf("Best SSIM : %lf\n", maxSSIMValue[i][j][k]);
				printf("Average Energy : %lf\n", (totalEnergy[i][j][k] / cntEnergy[i][j][k]));
			}
		}
	}
	*/
}