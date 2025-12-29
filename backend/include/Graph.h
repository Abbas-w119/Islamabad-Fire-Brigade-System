#ifndef GRAPH_H
#define GRAPH_H

#include <vector>
#include <limits>

using namespace std;

struct Edge {
    int destination;
    int weight;
};

class Graph {
private:
    vector<vector<Edge>> adjacencyList;
    int vertices;
    
public:
    Graph() : vertices(20) {
        adjacencyList.resize(vertices);
    }
    
    void addEdge(int src, int dest, int weight) {
        adjacencyList[src].push_back({dest, weight});
        adjacencyList[dest].push_back({src, weight}); // Undirected graph
    }
    
    vector<vector<Edge>>& getAdjacencyList() {
        return adjacencyList;
    }
    
    int getVertexCount() {
        return vertices;
    }
};

#endif