#ifndef GRAPH_H
#define GRAPH_H

#include <vector>
#include <utility>

class Graph {
private:
    int V;
    std::vector<std::vector<std::pair<int, double>>> adj;

public:
    Graph(int vertices);

    void addEdge(int u, int v, double weight);
    const std::vector<std::vector<std::pair<int, double>>>& getAdj() const;
};

#endif