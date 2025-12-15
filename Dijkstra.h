#ifndef DIJKSTRA_H
#define DIJKSTRA_H

#include <vector>
#include "Graph.h"
using namespace std;

class Dijkstra {
public:
    static vector<int> shortestPath(Graph& graph, int src);
};

#endif
