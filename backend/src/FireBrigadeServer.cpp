#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include <algorithm>
#include <winsock2.h>
#include <climits>
#include "Graph.h"
#include "Dijkstra.h"
#include "IncidentQueue.h"

#pragma comment(lib, "ws2_32.lib")

using namespace std;

class FireBrigadeServer {
private:
    SOCKET serverSocket;
    Graph cityGraph;
    IncidentQueue incidents;
    int incidentCount;
    
public:
    FireBrigadeServer() : incidentCount(0) {
        initializeGraph();
    }
    
    void initializeGraph() {
        // Islamabad City Grid - 20 nodes with comprehensive road network
        // Central Core (0-4)
        cityGraph.addEdge(0, 1, 5);    // Main Station to Blue Area
        cityGraph.addEdge(1, 2, 4);    // Blue Area to G-6
        cityGraph.addEdge(2, 3, 6);    // G-6 to Margalla
        cityGraph.addEdge(3, 4, 7);    // Margalla to Airport
        cityGraph.addEdge(0, 2, 8);    // Main to G-6 (express)
        cityGraph.addEdge(1, 3, 9);    // Blue Area to Margalla
        cityGraph.addEdge(0, 4, 14);   // Main to Airport (long route)
        
        // Northern Zone (5-9)
        cityGraph.addEdge(2, 5, 8);    // G-6 to Saidpur
        cityGraph.addEdge(3, 6, 7);    // Margalla to Pir Sohawa
        cityGraph.addEdge(5, 6, 6);    // Saidpur to Pir Sohawa
        cityGraph.addEdge(5, 7, 5);    // Saidpur to Fatima Jinnah Park
        cityGraph.addEdge(6, 8, 8);    // Pir Sohawa to Aabpara
        cityGraph.addEdge(7, 8, 7);    // Fatima Park to Aabpara
        cityGraph.addEdge(8, 9, 4);    // Aabpara to Kashmir Highway
        
        // Eastern Zone (10-14)
        cityGraph.addEdge(4, 10, 9);   // Airport to Shifa Hospital
        cityGraph.addEdge(10, 11, 6);  // Shifa to Research Center
        cityGraph.addEdge(11, 12, 5);  // Research to Chak Shahzad
        cityGraph.addEdge(12, 13, 7);  // Chak Shahzad to Koral
        cityGraph.addEdge(13, 14, 6);  // Koral to Behlwal
        
        // Western Zone (15-19)
        cityGraph.addEdge(0, 15, 10);  // Main to Rawalpindi
        cityGraph.addEdge(15, 16, 8);  // Rawalpindi to Pirwadhai
        cityGraph.addEdge(16, 17, 7);  // Pirwadhai to Potters
        cityGraph.addEdge(17, 1, 9);   // Potters to Blue Area
        cityGraph.addEdge(15, 18, 6);  // Rawalpindi to Sector F
        cityGraph.addEdge(18, 19, 5);  // Sector F to Westridge
        
        // Cross-city connections
        cityGraph.addEdge(9, 14, 12);  // Kashmir Hwy to Behlwal
        cityGraph.addEdge(8, 11, 10);  // Aabpara to Research
        cityGraph.addEdge(7, 10, 11);  // Fatima Park to Shifa
        cityGraph.addEdge(4, 13, 15);  // Airport to Koral
        cityGraph.addEdge(1, 19, 13);  // Blue Area to Westridge
        cityGraph.addEdge(19, 14, 14); // Westridge to Behlwal
        cityGraph.addEdge(16, 10, 11); // Pirwadhai to Shifa
        cityGraph.addEdge(12, 9, 10);  // Chak Shahzad to Kashmir Hwy
    }

    
    void startServer(int port) {
        WSADATA wsaData;
        WSAStartup(MAKEWORD(2, 2), &wsaData);
        
        serverSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        
        sockaddr_in serverAddr;
        serverAddr.sin_family = AF_INET;
        serverAddr.sin_addr.s_addr = inet_addr("127.0.0.1");
        serverAddr.sin_port = htons(port);
        
        if (bind(serverSocket, (sockaddr*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
            cout << "Bind error!" << endl;
            return;
        }
        
        listen(serverSocket, 5);
        cout << "Fire Brigade Server started on port " << port << endl;
        cout << "Waiting for client connections..." << endl;
        
        while (true) {
            SOCKET clientSocket = accept(serverSocket, NULL, NULL);
            if (clientSocket != INVALID_SOCKET) {
                cout << "Client connected!" << endl;
                handleClient(clientSocket);
                closesocket(clientSocket);
                cout << "Client disconnected!" << endl;
            }
        }
    }
    
    void handleClient(SOCKET clientSocket) {
        char buffer[1024];
        int bytesReceived;
        
        while (true) {
            memset(buffer, 0, sizeof(buffer));
            bytesReceived = recv(clientSocket, buffer, sizeof(buffer) - 1, 0);
            
            if (bytesReceived <= 0) {
                break;
            }
            
            buffer[bytesReceived] = '\0';
            string request(buffer);
            
            // Remove newline characters
            request.erase(remove(request.begin(), request.end(), '\n'), request.end());
            request.erase(remove(request.begin(), request.end(), '\r'), request.end());
            
            cout << "Request: " << request << endl;
            
            string response = processRequest(request);
            
            cout << "Response: " << response << endl;
            
            // Send response with newline
            response += "\n";
            send(clientSocket, response.c_str(), response.length(), 0);
        }
    }
    
    string processRequest(string request) {
        stringstream ss(request);
        string command;
        ss >> command;
        
        if (command == "ROUTE") {
            int src, dest;
            ss >> src >> dest;
            vector<int> path = Dijkstra::shortestPath(cityGraph, src, dest);
            return formatPath(path);
        }
        else if (command == "INCIDENT") {
            int station, severity;
            ss >> station >> severity;
            incidents.enqueue({station, severity});
            incidentCount++;
            return "INCIDENT_ADDED|" + to_string(incidentCount);
        }
        else if (command == "GET_INCIDENTS") {
            return "INCIDENTS|" + to_string(incidentCount);
        }
        else if (command == "STATIONS") {
            return "STATIONS|5|Downtown,33.7414,74.3569|Airport,33.5651,74.2165|Margalla,33.8186,74.3289|BlueArea,33.7738,74.5175|Rawalpindi,33.5895,74.3055";
        }
        
        return "ERROR";
    }
    
    string formatPath(vector<int> path) {
        string result = "ROUTE|";
        for (size_t i = 0; i < path.size(); i++) {
            result += to_string(path[i]);
            if (i < path.size() - 1) result += ",";
        }
        return result;
    }
};

int main() {
    FireBrigadeServer server;
    server.startServer(5000);
    return 0;
}