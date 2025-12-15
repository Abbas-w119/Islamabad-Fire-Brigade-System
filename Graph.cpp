#include "Graph.h"

Graph::Graph(int vertices) {
    V = vertices;
    adj.resize(V);
}

void Graph::addEdge(int u, int v, int distance) {
    adj[u].push_back({v, distance});
    adj[v].push_back({u, distance}); // Undirected road
}

vector<vector<pair<int, int>>> Graph::getAdj() const {
    return adj;
}

int Graph::getVertexCount() {
    return V;
}
