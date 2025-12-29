#ifndef NODE_H
#define NODE_H

#include <string>

struct Node {
    int id;
    std::string name;
    double x, y;

    Node(int id, std::string name, double x, double y);
};

#endif