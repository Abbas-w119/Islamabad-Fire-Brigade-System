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
    vector<vector<pair<int, int>>> getAdj() const;
    int getVertexCount();
};

#endif
