#include "Router.h"
#include "Dijkstra.h"
#include <sstream>

string Router::processRequest(Graph& graph, int fireNode) {
    vector<int> stations = {0, 1}; // Example fire stations
    vector<int> dist = Dijkstra::shortestPath(graph, fireNode);

    int nearest = -1, minDist = INT_MAX;

    for (int s : stations) {
        if (dist[s] < minDist) {
            minDist = dist[s];
            nearest = s;
        }
    }

    stringstream ss;
    ss << "Nearest Station: " << nearest
       << " | Distance: " << minDist;

    return ss.str();
}
