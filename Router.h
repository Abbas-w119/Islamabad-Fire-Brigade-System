#ifndef ROUTER_H
#define ROUTER_H

#include "Graph.h"
#include <string>

class Router {
public:
    static string processRequest(Graph &graph, int fireNode);
};

#endif
