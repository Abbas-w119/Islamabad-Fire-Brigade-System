#ifndef GRAPH_H
#define GRAPH_H

#include <vector>
#include <utility>
#include "Node.h"

using namespace std;

class Graph {
private:
    int V;
    vector<vector<pair<int, int>>> adj; 

public:
    Graph(int vertices);
    void addEdge(int u, int v, int distance);
    vector<pair<int, int>> getNeighbors(int u);
    int getVertexCount();
};

#endif
