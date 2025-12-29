#ifndef INCIDENT_QUEUE_H
#define INCIDENT_QUEUE_H

#include <queue>

using namespace std;

struct Incident {
    int stationId;
    int severity; // 1=Low, 2=Medium, 3=Critical
};

class IncidentQueue {
private:
    queue<Incident> incidentQueue;
    
public:
    void enqueue(Incident incident) {
        incidentQueue.push(incident);
    }
    
    Incident dequeue() {
        Incident incident = incidentQueue.front();
        incidentQueue.pop();
        return incident;
    }
    
    bool isEmpty() {
        return incidentQueue.empty();
    }
    
    int size() {
        return incidentQueue.size();
    }
};

#endif