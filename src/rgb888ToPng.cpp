#include <iostream>
#include <sstream>
#include <cstdlib>

using namespace std;

int rgb888ToPng(int width, int height, string filename, string newFilename);

int main(int argc, char** argv)
{
	//return(rgb888ToPng(2592, 1936, "../felix.rgb", "../felix.png"));
	return(rgb888ToPng(atoi(argv[1]), atoi(argv[2]), argv[3], argv[4]));
}

int rgb888ToPng(int width, int height, string filename, string newFilename)
{
	stringstream command;
	
	command << "convert -size " << """" << width << "x" << height << """" << " -depth 8 " << filename
			   << " " << newFilename;
	return(system(command.str().c_str()));
}

