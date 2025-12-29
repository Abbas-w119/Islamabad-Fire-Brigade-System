#ifndef DIJKSTRA_H
#define DIJKSTRA_H

#include "Graph.h"
#include <vector>
#include <limits>
#include <queue>
#include <climits>

using namespace std;

class Dijkstra {
public:
    static vector<int> shortestPath(Graph& graph, int src, int dest) {
        int V = graph.getVertexCount();
        vector<int> dist(V, INT_MAX);
        vector<int> parent(V, -1);
        vector<bool> visited(V, false);
        
        priority_queue<pair<int, int>, vector<pair<int, int>>, greater<pair<int, int>>> pq;
        
        dist[src] = 0;
        pq.push({0, src});
        
        while (!pq.empty()) {
            int u = pq.top().second;
            pq.pop();
            
            if (visited[u]) continue;
            visited[u] = true;
            
            if (u == dest) break; // Early termination when destination reached
            
            for (auto& edge : graph.getAdjacencyList()[u]) {
                int v = edge.destination;
                int weight = edge.weight;
                
                if (!visited[v] && dist[u] != INT_MAX && dist[u] + weight < dist[v]) {
                    dist[v] = dist[u] + weight;
                    parent[v] = u;
                    pq.push({dist[v], v});
                }
            }
        }
        
        // Reconstruct path
        vector<int> path;
        int current = dest;
        
        // Check if destination is reachable
        if (dist[dest] == INT_MAX) {
            // If unreachable, return direct connection
            path.push_back(src);
            path.push_back(dest);
            return path;
        }
        
        while (current != -1) {
            path.insert(path.begin(), current);
            current = parent[current];
        }
        
        return path;
    }
};

#endif