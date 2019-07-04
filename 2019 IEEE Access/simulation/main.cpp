#include <bits/stdc++.h>
using namespace std;
#define VIDEO_SIZE 6
#define INF 1987654321
typedef pair<pair<double, double>, pair<int, int> > Data;

const string filename[9] = { "motion","dance","moving","hongkong","walking","football","car","oops","truck" };
// Constants
enum Video : int {
	Motion, Dance, Moving, Hongkong, Walking, Football, Car, Oops, Truck
};
enum Resolution : int {
	_240p, _360p, _480p, _720p, _1080p
};

class VideoInfo {
private:
	string videoName;
	int numberOfSegment;
	int numberOfVersion;
	vector<vector<double> > powerTable;
	vector<vector<double> > ssimTable;
	double totalPower;

public:
	VideoInfo(const int videoIndex) {
		this->videoName = filename[videoIndex];
		read();
	}

	VideoInfo(string videoName) {
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
};
vector<VideoInfo> videos;
class Heuristic {
private:
	int frame_length;
	VideoInfo *video;
	int requestVideoNum;
	int currentRequestSegmentIdx;
	double powerLimit;
	double totalUsedPower;
	double totalSSIM;
	int prevSelectedVersion;
	vector<Data> sortedVec;
	vector<int> versionSet;
public:
	Heuristic(int videoIdx) {
		video = &videos[videoIdx];
		frame_length = video->getNumberOfSegment();
	}

	void initSortedVector() {
		sortedVec.clear();
		int nSeg = video->getNumberOfSegment();
		int nVersion = video->getNumberOfVersion();

		for (int seg = 0; seg < nSeg; seg++) {
			double basic_power = video->getPowerOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
			double basic_ssim = video->getSSIMOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
			for (int ver = 1; ver < nVersion; ver++) {
				double power = video->getPowerOfSegmentVersion(seg, ver);
				double ssim = video->getSSIMOfSegmentVersion(seg, ver);
				double r = (ssim - basic_ssim) / (power - basic_power);
				Data temp = { { -r,power },{ seg,ver } };

				if (r > 0)
					sortedVec.push_back(temp);
			}
		}
		sort(sortedVec.begin(), sortedVec.end());
	}

	void initValue() {
		currentRequestSegmentIdx = 0;
		totalUsedPower = 0;
		totalSSIM = 0;
		versionSet.assign(frame_length, 0);
		initSortedVector();
	}

	void execute(int p_limit) {
		initValue();
		powerLimit = (video->getTotalPower()*p_limit)/100+(1e-9);
		executeAlgorithm();
		for (int i = 0; i < frame_length; i++) {
			int calculatedVersion = requestCurrentVideoSegmentVersion();
			int selectVersion = calculatedVersion;
			/* network에 따라 selectVersion이 바뀔 수 있음 */
			setPrevSelectVesion(selectVersion);
			if (selectVersion != calculatedVersion) {
				executeAlgorithm();
			}
		} 
		print();
	}

	void executeAlgorithm() {
		int position = currentRequestSegmentIdx;
		int nSeg = video->getNumberOfSegment() - currentRequestSegmentIdx;
		vector<Data> nextVec;
		for (int i = position; i<nSeg; i++) {
			versionSet[i] = 0;
		}
		double currentPower = totalUsedPower;
		double currentSSIM = 0;
		for (int seg = 0; seg<nSeg; seg++) {
			currentPower += video->getPowerOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
			currentSSIM += video->getSSIMOfSegmentVersion(currentRequestSegmentIdx + seg, 0);
		}

		int idx = 0;
		int cnt = sortedVec.size();
		while (idx<cnt) {
			Data curData = sortedVec[idx++];
			double R = -curData.first.first;
			double power = curData.first.second;
			int SegmentIdx = curData.second.first;
			int versionIdx = curData.second.second;
			double SSIM = R * power;
			if (SegmentIdx>currentRequestSegmentIdx) {
				nextVec.push_back(curData);
			}
			if (currentPower<powerLimit) {
				double selectPower = video->getPowerOfSegmentVersion(SegmentIdx, versionSet[SegmentIdx]);
				double selectSSIM = video->getSSIMOfSegmentVersion(SegmentIdx, versionSet[SegmentIdx]);

				double nextPower = currentPower + power - selectPower;
				double nextSSIM = currentSSIM + SSIM - selectSSIM;

				if (currentPower < nextPower && nextPower <= powerLimit) {
					versionSet[SegmentIdx] = versionIdx;
					currentPower = nextPower;
					currentSSIM = nextSSIM;
				}
			}
		}
		sortedVec = nextVec;
		double ssim_value = 0;
		double power_value = 0;
		for (int i = 0; i < frame_length; i++) {
			ssim_value += video->getSSIMOfSegmentVersion(i, versionSet[i]);
			power_value += video->getPowerOfSegmentVersion(i, versionSet[i]);
		}
		puts("");
	}

	int requestCurrentVideoSegmentVersion() {
		if (currentRequestSegmentIdx >= versionSet.size()) {
			return 0;
		}
		return versionSet[currentRequestSegmentIdx++];
	}

	void setPrevSelectVesion(int ver) {
		//prevSelectedVersion = video->getNumberOfVersion() - 1 - ver;
		totalUsedPower += video->getPowerOfSegmentVersion(currentRequestSegmentIdx - 1, ver);
		totalSSIM += video->getSSIMOfSegmentVersion(currentRequestSegmentIdx - 1, ver);
	}

	void print() {
		printf("[BSA-H]\nSSIM VALUE is %lf\n Average SSIM VALUE is %lf\nPower is %lf\nLimit Power is %lf\n", totalSSIM, totalSSIM / frame_length, totalUsedPower, powerLimit);
	}

};
/*
class Knapsack {
private:
	int frame_length;
	VideoInfo *video;
	int requestVideoNum;
	int currentRequestSegmentIdx;
	double powerLimit;
	double totalUsedPower;
	double totalSSIM;
	int prevSelectedVersion;
	vector<int> versionSet;
	vector<vector<vector<double> > > dp_table;// [consumed_power][frame][resolution]; // resolution => 5, consumed_power, frame => each video diffent
	vector<vector<vector<int> > > visited; // Check if you visited dp table.
	vector<vector<vector<pair<int, int> > > > parent;
public:
	Knapsack(int videoIdx) {
		video = &videos[videoIdx];
		frame_length = video->getNumberOfSegment();
	}

	void initValue() {
		currentRequestSegmentIdx = 0;
		totalUsedPower = 0;
		totalSSIM = 0;
		versionSet.assign(frame_length, 0);
	}

	void execute(int p_limit) {
		initValue();
		powerLimit = (video->getTotalPower()*p_limit) / 100 + (1e-9);
		executeAlgorithm();
		for (int i = 0; i < frame_length; i++) {
			int calculatedVersion = requestCurrentVideoSegmentVersion();
			int selectVersion = calculatedVersion;
			// network에 따라 selectVersion이 바뀔 수 있음
			setPrevSelectVesion(selectVersion);
			if (selectVersion != calculatedVersion) {
				executeAlgorithm();
			}
		}
		print();
	}

	template <typename T>
	void initTable(vector<vector<vector<T> > > &vec, int size) {
		vec.resize(size);
		for (int i = 0; i < vec.size(); i++) {
			vec[i].resize(video->getNumberOfSegment());
			for (int j = 0; j < vec[i].size(); j++) {
				vec[i][j].resize(video->getNumberOfVersion() + 1);
			}
		}
	}
	void initKnapsackTable() {
		dp_table.clear();
		visited.clear();
		parent.clear();
		int power = powerLimit + 10;
		initTable(dp_table, power);
		initTable(visited, power);
		initTable(parent, power);
	}

	double getMaxSSIMValue_Knapsack(int consumed_power, int frame, int resolution) {
		if (consumed_power >= powerLimit)
			return -INF;
		if (frame == video->getNumberOfSegment()) {
			return 0;//ssim_table[frame][resolution];
		}
		double &ret = dp_table[consumed_power][frame][resolution];
		int &isVisited = visited[consumed_power][frame][resolution];
		if (isVisited) return ret;
		isVisited = true;
		ret = video->getSSIMOfSegmentVersion(frame, resolution); // ssim_table[frame][resolution];
		double maxValue = 0;
		for (int r = 0; r <= 4; r++) {
			int next_power = consumed_power + video->getPowerOfSegmentVersion(frame, r); //power_table[frame + 1][r];
			double value = getMaxSSIMValue_Knapsack(next_power, frame + 1, r);
			if (maxValue < value) {
				maxValue = value;
				parent[consumed_power][frame][resolution].first = r;
				parent[consumed_power][frame][resolution].second = next_power;
			}
		}
		return ret += maxValue;
	}

	
	void executeAlgorithm() {
		initKnapsackTable();
		double ssim = getMaxSSIMValue_Knapsack(0, 0, 0);
		int current_resolution = parent[0][0][0].first;
		int current_power = parent[0][0][0].second;
		double consumed_power = 0;
		versionSet[0] = current_resolution;
		int prev_resolution, prev_power;
		for (int i = 0; i < video->getNumberOfSegment()-1; i++) {
			prev_resolution = current_resolution;
			prev_power = current_power;
			consumed_power += video->getPowerOfSegmentVersion(i, current_resolution);
			current_resolution = parent[prev_power][i + 1][prev_resolution].first;
			current_power = parent[prev_power][i + 1][prev_resolution].second;
			versionSet[i+1]=current_resolution;			

		}
	}

	int requestCurrentVideoSegmentVersion() {
		if (currentRequestSegmentIdx >= versionSet.size()) {
			return 0;
		}
		return versionSet[currentRequestSegmentIdx++];
	}

	void setPrevSelectVesion(int ver) {
		totalUsedPower += video->getPowerOfSegmentVersion(currentRequestSegmentIdx - 1, ver);
		totalSSIM += video->getSSIMOfSegmentVersion(currentRequestSegmentIdx - 1, ver);
	}

	void print() {
		printf("[BSA-DP]\nSSIM VALUE is %lf\n Average SSIM VALUE is %lf\nPower is %lf\nLimit Power is %lf\n", totalSSIM, totalSSIM / frame_length, totalUsedPower, powerLimit);
	}
};
*/
class CBS {
private:
	int frame_length;
	VideoInfo *video;
public:
	CBS(int videoIdx) {
		video = &videos[videoIdx];
		frame_length = video->getNumberOfSegment();
	}
	void execute(int p_limit) {
		vector<int> resolution = vector<int>(frame_length, Resolution::_1080p);
		double current_power = 0;

		for (int i = 0; i < frame_length; i++) {
			current_power += video->getPowerOfSegmentVersion(i, Resolution::_1080p);
		}
		double power_limit = (current_power*p_limit)/100 + (1e-9);
		int frame_idx = 0;
		while (current_power > power_limit) {
			if (resolution[frame_idx] == 0) break;
			int prev_resolution = resolution[frame_idx];
			current_power -= video->getPowerOfSegmentVersion(frame_idx, prev_resolution) - video->getPowerOfSegmentVersion(frame_idx, prev_resolution - 1);
			resolution[frame_idx]--;
			frame_idx = (frame_idx + 1) % frame_length;
		}
		double ssim_value = 0;
		double power_value = 0;
		for (int i = 0; i < frame_length; i++) {
			ssim_value += video->getSSIMOfSegmentVersion(i, resolution[i]);
			power_value += video->getPowerOfSegmentVersion(i, resolution[i]);
		}
		printf("[CBS]\nSSIM VALUE is %lf\n Average SSIM VALUE is %lf\nPower is %lf\nLimit Power is %lf\n", ssim_value, ssim_value / frame_length, current_power, power_limit);
	}
};

int main() {
	freopen("output.txt", "w", stdout);
	for (int i = 0; i <VIDEO_SIZE; i++) {
		puts("=================================================");
		cout << "Video " << i << " : " << filename[i] << endl;
		videos.push_back(VideoInfo(i));
		Heuristic bsaH(videos.size()-1);
		//Knapsack bsaDP(videos.size() - 1);
		CBS cbs(videos.size() - 1);
		for (int ratio = 85; ratio <=100; ratio += 5) {
			cout << "ratio =" << ratio << endl;
			bsaH.execute(ratio);
			//bsaDP.execute(ratio);
			cbs.execute(ratio);
		}
		puts("=================================================");
	}
	return 0;
}