#include <iostream>
#include <fstream>
#include <algorithm>
#include <vector>
#include <string>
#include <cstdio>
#include <ctime>
using namespace std;
#define PRINT_DETAIL_MODE 1
const string filename[9] = { "motion","car","oops","dance","moving","truck","hongkong","walking","football" };
// Constants
enum Video : int {
	Motion, Car, Oops, Dance, Moving, Truck, Hongkong, Walking, Football
};
enum Resolution : int {
	_240p, _360p, _480p, _720p, _1080p
};

const double VIDEO_POWER_LIMIT[9] = { 0.803450251044396, 0.729584615247028, 0.808995082586798, 0.805575446516342, 0.81489502970664, 0.810117308707283 ,0.84,0.84,0.84 };
const double PLAYTIME[9] = { 90,90,90 ,90 ,90 , 90 ,90, 90,90 };
double POWER_LIMIT = 0.85;
int TABLE_SELECTED = Video::Motion;
const int INF = -10001;
// Variable
int total_power;
int limit_power;
int frame_length;
double max_ssim_value;
vector<vector<double> > power_table, ssim_table;
vector<vector<int> >  filesize_table;
string power_filename, ssim_filename, filesize_filename;

double real_total_power;
double dp_table[250000][60][6];// [consumed_power][frame][resolution]; // resolution => 5, consumed_power, frame => each video diffent
int visited[250000][60][6]; // Check if you visited dp table.
pair<int, int> parent[250000][60][6];
void init();

double bruteForce();
void setFileName();
void getLimitPower();
void readTable();
void readPowerTable();
void readSSIMTable();
void readFileSizeTable();
// knapsack
bool isPossiblePowerLimit();
double getMaxSSIMValue_Knapsack(int consumed_power, int frame, int resolution);
void initDptable();
void printSelectedValue();
void printResolution(int current_resolution);
// uniform
void uniform_set(double powerLimit);
void makeOutputFile();



int main() {
	//freopen("v20_knapsack_motion_85.txt", "w", stdout);
	if (!isPossiblePowerLimit()) {
		cout << "!Error!: Power Limit Constant Value is Low." << endl;
		return 0;
	}
	clock_t start, end;

	

	for (int t = 0; t < 5; t++) {
		TABLE_SELECTED = t;
		POWER_LIMIT = 0.85;
		cout << filename[t] << endl;
		for (int k = 0; k < 4; k++) {
			long long sum = 0;
			init();
			printf("table size : %d\n", power_table.size());
			printf("limit power : %d\n", limit_power);
			printf("%lf\n", POWER_LIMIT);
			initDptable();
			start = clock();
			max_ssim_value = getMaxSSIMValue_Knapsack(0, 0, 0);
			end = clock();
			sum += (end - start);
			printSelectedValue();
			puts("");
			POWER_LIMIT += 0.05;
		}
	}
	printf("brute force max ssim! : %.5lf\n", bruteForce());
	//puts("");
	//puts("US");
	//uniform_set(POWER_LIMIT);

}

bool isPossiblePowerLimit() {
	return POWER_LIMIT > VIDEO_POWER_LIMIT[TABLE_SELECTED];
}

void uniform_set(double p_limit) {
	vector<int> resolution = vector<int>(frame_length, Resolution::_1080p);
	double current_power = 0;

	for (int i = 1; i < frame_length; i++) {
		current_power += (int)power_table[i][Resolution::_1080p];
	}
	double power_limit = current_power*p_limit;
	int frame_idx = 0;
	while (current_power > power_limit) {
		if (resolution[frame_idx] == 0) break;
		int prev_resolution = resolution[frame_idx];
		current_power -= (int)power_table[frame_idx][prev_resolution] - (int)power_table[frame_idx][prev_resolution - 1];
		resolution[frame_idx]--;
		frame_idx = (frame_idx + 1) % frame_length;
	}
	puts("[ SELECT RESOLUTION ::]\n");
	double ssim_value = 0;
	for (int i = 1; i < frame_length; i++) {
		printf("%d ", resolution[i]);
		ssim_value += ssim_table[i][resolution[i]];
	}
	printf("\nSSIM VALUE is %lf\n Average SSIM VALUE is %lf\nPower is %lf\nLimit Power is %lf\n", ssim_value, ssim_value / (frame_length - 1), current_power, power_limit);
}


double getMaxSSIMValue_Knapsack(int consumed_power, int frame, int resolution) {
	if (consumed_power > limit_power)
		return INF;
	if (frame == frame_length - 1) return ssim_table[frame][resolution];
	double &ret = dp_table[consumed_power][frame][resolution];
	int &isVisited = visited[consumed_power][frame][resolution];
	if (isVisited) return ret;
	isVisited = true;
	ret = ssim_table[frame][resolution];
	double maxValue = 0;
	for (int r = Resolution::_240p; r <= Resolution::_1080p; r++) {
		int next_power = consumed_power + power_table[frame + 1][r];
		double value = getMaxSSIMValue_Knapsack(next_power, frame + 1, r);
		if (maxValue < value) {
			maxValue = value;
			parent[consumed_power][frame][resolution].first = r;
			parent[consumed_power][frame][resolution].second = next_power;
		}
	}
	return ret += maxValue;
}

void init() {
	setFileName();
	readTable();
	getLimitPower();
	
}

void setFileName() {
	power_filename = filename[TABLE_SELECTED] + "_power_table.txt";
	ssim_filename = filename[TABLE_SELECTED] + "_ssim_table.txt";
	filesize_filename = filename[TABLE_SELECTED] + "_filesize_table.txt";
}

void getLimitPower() {
	total_power = 0;
	real_total_power = 0;
	for (int i = 0; i < power_table.size(); i++)
	{
		total_power += (int)power_table[i][Resolution::_1080p];
		real_total_power += power_table[i][Resolution::_1080p];
	}
	limit_power = total_power*POWER_LIMIT;
}

double bruteForce() {
	double current_power = 0;

	for (int i = 1; i < frame_length; i++) {
		current_power += (int)power_table[i][Resolution::_1080p];
	}
	double power_limit = current_power*POWER_LIMIT;
	double max_ssim = 0;
	for (int a1 = Resolution::_240p; a1 <= Resolution::_1080p; a1++) {
		for (int a2 = Resolution::_240p; a2 <= Resolution::_1080p; a2++) {
			for (int a3 = Resolution::_240p; a3 <= Resolution::_1080p; a3++) {
				for (int a4 = Resolution::_240p; a4 <= Resolution::_1080p; a4++) {
					for (int a5 = Resolution::_240p; a5 <= Resolution::_1080p; a5++) {
						for (int a6 = Resolution::_240p; a6 <= Resolution::_1080p; a6++) {
							for (int a7 = Resolution::_240p; a7 <= Resolution::_1080p; a7++) {
								for (int a8 = Resolution::_240p; a8 <= Resolution::_1080p; a8++) {
									for (int a9 = Resolution::_240p; a9 <= Resolution::_1080p; a9++) {
										double ssimvalue = ssim_table[1][a1] + ssim_table[2][a2] + ssim_table[3][a3] + ssim_table[4][a4] + ssim_table[5][a5] + ssim_table[6][a6] + ssim_table[7][a7] + ssim_table[8][a8] + ssim_table[9][a9];
										int consumed_power = (int)power_table[1][a1] + (int)power_table[2][a2] + (int)power_table[3][a3] +
											(int)power_table[4][a4] + (int)power_table[5][a5] + (int)power_table[6][a6] + (int)power_table[7][a7] +
											(int)power_table[8][a8] + (int)power_table[9][a9];
										if (consumed_power > power_limit) continue;
										max_ssim = max(max_ssim, ssimvalue);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	return max_ssim;
}

void readTable() {
	readPowerTable();
	readSSIMTable();
	//	readFileSizeTable();
}

void printSelectedValue() {
	int maxidx = 0;
	double max = getMaxSSIMValue_Knapsack(0, 0, 0);
	int current_resolution = parent[0][0][0].first;
	int current_power = parent[0][0][0].second;
	double consumed_power = 0;
	int prev_resolution, prev_power;
	for (int i = 1; i < frame_length; i++) {
		prev_resolution = current_resolution;
		prev_power = current_power;
		consumed_power += (int)power_table[i][current_resolution];
#ifdef PRINT_DETAIL_MODE 
		cout << "frame #" << i << " : " << current_resolution;
		printResolution(current_resolution);
#endif
		cout << current_resolution << endl;
		current_resolution = parent[prev_power][i][prev_resolution].first;
		current_power = parent[prev_power][i][prev_resolution].second;
	}
	double energy_saving_rate = (double)consumed_power / total_power * 100;
#ifdef PRINT_DETAIL_MODE 
	cout << "Total Power Consumed : " << consumed_power << " / " << limit_power << " ! " << total_power << endl;
	cout << "Average Power Consumed : " << consumed_power / PLAYTIME[TABLE_SELECTED] << endl;
	cout << "Energy Saving is " << 100 - energy_saving_rate << " %" << endl; // 1080p와 비교했을 때 전력 소모 감소량
	cout << "Total SSIM Value : " << max_ssim_value << endl;
#endif
}

void printResolution(int current_resolution) {
	switch (current_resolution) {
	case Resolution::_240p: cout << " [240p]" << endl; break;
	case Resolution::_360p: cout << " [360p]" << endl; break;
	case Resolution::_480p: cout << " [480p]" << endl; break;
	case Resolution::_720p: cout << " [720p]" << endl; break;
	case Resolution::_1080p: cout << " [1080p]" << endl; break;
	}
}

void readPowerTable() {
	ifstream infile(power_filename);
	int cnt = 1;
	power_table.clear();
	power_table.push_back(vector<double>(5));
	while (!infile.eof()) {
		power_table.push_back(vector<double>(5));
		for (int r = Resolution::_240p; r <= Resolution::_1080p; r++)
			infile >> power_table[cnt][r];
		cnt++;
	}
}
void readSSIMTable() {
	ifstream infile(ssim_filename);
	int cnt = 1;							
	ssim_table.clear();
	ssim_table.push_back(vector<double>(5));
	while (!infile.eof()) {
		ssim_table.push_back(vector<double>(5));
		for (int r = Resolution::_240p; r <= Resolution::_1080p; r++) {
			infile >> ssim_table[cnt][r];
		}
		cnt++;
	}
}
/*
void readFileSizeTable() {
ifstream infile(filesize_fil ename);
int cnt = 1;
filesize_table.push_back(vector<int>(5));
while (!infile.eof()) {
filesize_table.push_back(vector<int>(5));
for (int r = Resolution::_240p; r <= Resolution::_1080p; r++) infile >> filesize_table[cnt][r];
cnt++;
}
} */
// 7 8 8 6 7 5
void initDptable() {
	frame_length = power_table.size();
	memset(dp_table, 0, sizeof(dp_table));
	memset(visited, 0, sizeof(visited));
	memset(parent, 0, sizeof(parent));
}

void makeOutputFile() {

}