#pragma once

#include <string>
#include <vector>
#include <map>
#include <iterator>
#include "rapidjson/document.h"

using namespace std;

class SumSim {
public:
    SumSim();
    ~SumSim();

    void simulate();

    rapidjson::Value & getMessage(string);
    void setMessage(string, rapidjson::Value &);
    void setMessage(string, string &);

    void generateInitialMessage();

private:
    rapidjson::Document doc;
    map<string, rapidjson::Value> input;
    map<string, rapidjson::Value> output;

    map<string, int> history_size;

    map<string, vector<rapidjson::Value> > history;

    void updateHistory();

};
