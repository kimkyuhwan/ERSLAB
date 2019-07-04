#include <bits/stdc++.h>
using namespace std;
int BASE_BITRATE[] = { 5,20,100,5};
int BITRATE_RANGE[] = { 15,60,100,195 };



int main() {
	srand(time(0));
	for (int i = 0; i < 3; i++) {
		printf("{");
		for (int j = 0; j < 20; j++) {
			int bitrate = rand() % BITRATE_RANGE[i] + BASE_BITRATE[i];
			printf("%d", bitrate * 100000);
			if (j != 19) {
				printf(", ");
			}
		}
		printf("}\n");
	}
	printf("{");
	for (int j = 0; j < 20; j++) {
		int k = rand() % 3;
		int bitrate = rand() % BITRATE_RANGE[k] + BASE_BITRATE[k];
		printf("%d", bitrate * 100000);
		if (j != 19) {
			printf(", ");
		}
	}
	printf("}");

}