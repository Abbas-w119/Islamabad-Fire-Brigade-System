#include "Router.h"
#include <sstream>

string Router::processRequest(Graph &graph, int fireNode) {
    stringstream ss;
    ss << "Fire reported at node: " << fireNode;
    ss << " | Routing logic will be applied here.";
    return ss.str();
    
}