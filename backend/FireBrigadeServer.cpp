#include <iostream>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <vector>
#include <ctime>
#include <sstream>
#include <algorithm>
#include <climits>
#include <string>
#include <process.h>

#pragma comment(lib, "ws2_32.lib")

#define PORT 5000
#define BUFFER_SIZE 4096

struct Client {
    SOCKET socket;
    std::string ip;
    int id;
};

struct Station {
    int id;
    std::string name;
    double lat;
    double lon;
};

class Graph {
private:
    std::vector<std::vector<std::pair<int, int> > > adj;
    int vertices;

public:
    Graph(int v) : vertices(v) {
        adj.resize(v);
    }

    void addEdge(int u, int v, int weight) {
        adj[u].push_back(std::make_pair(v, weight));
        adj[v].push_back(std::make_pair(u, weight));
    }

    std::vector<int> dijkstra(int src, int dest) {
        std::vector<int> dist(vertices, INT_MAX);
        std::vector<int> parent(vertices, -1);
        std::vector<bool> visited(vertices, false);

        dist[src] = 0;

        for (int count = 0; count < vertices - 1; count++) {
            int u = -1;
            for (int v = 0; v < vertices; v++) {
                if (!visited[v] && (u == -1 || dist[v] < dist[u])) {
                    u = v;
                }
            }

            if (dist[u] == INT_MAX) break;
            visited[u] = true;

            for (size_t i = 0; i < adj[u].size(); i++) {
                int v = adj[u][i].first;
                int weight = adj[u][i].second;
                if (!visited[v] && dist[u] + weight < dist[v]) {
                    dist[v] = dist[u] + weight;
                    parent[v] = u;
                }
            }
        }

        std::vector<int> path;
        int current = dest;
        while (current != -1) {
            path.insert(path.begin(), current);
            current = parent[current];
        }

        return path;
    }
};

std::vector<Station> stations;
Graph* roadNetwork = NULL;

std::vector<std::string> split(const std::string& s, char delimiter) {
    std::vector<std::string> tokens;
    std::stringstream ss(s);
    std::string token;
    while (std::getline(ss, token, delimiter)) {
        tokens.push_back(token);
    }
    return tokens;
}

std::string getCurrentTime() {
    time_t now = time(NULL);
    struct tm* timeinfo = localtime(&now);
    char buffer[20];
    strftime(buffer, sizeof(buffer), "%H:%M:%S", timeinfo);
    return std::string(buffer);
}

std::string intToString(int value) {
    std::stringstream ss;
    ss << value;
    return ss.str();
}

std::string processMessage(std::string message, int clientId) {
    std::vector<std::string> parts = split(message, '|');
    if (parts.empty()) {
        return "ERROR|Empty message";
    }

    std::string command = parts[0];

    if (command == "INCIDENT") {
        std::cout << "[CLIENT " << clientId << "] - Incident received" << std::endl;
        return "INCIDENT_ACK|Processed|Route dispatching";
    }
    else if (command == "STATUS") {
        return "STATUS_RESPONSE|Online|5 stations|System operational|BACKEND:CPP";
    }
    else if (command == "STATIONS") {
        return "STATIONS|5|0,Main Station,33.6844,73.0479|1,Blue Area,33.7182,73.0605|2,G-6 Sector,33.7100,73.0800|3,Margalla Road,33.7400,73.0900|4,Airport Road,33.6167,73.0992";
    }
    else if (command == "DIJKSTRA") {
        if (parts.size() >= 3) {
            int src = atoi(parts[1].c_str());
            int dest = atoi(parts[2].c_str());
            
            std::vector<int> path = roadNetwork->dijkstra(src, dest);
            
            std::string pathStr = "";
            for (size_t i = 0; i < path.size(); i++) {
                pathStr += intToString(path[i]);
                if (i < path.size() - 1) pathStr += ",";
            }

            return "DIJKSTRA_RESULT|" + pathStr + "|Path optimized";
        }
    }
    else if (command == "PING") {
        return "PONG|Server alive|" + getCurrentTime();
    }

    return "ERROR|Unknown command";
}

unsigned int __stdcall handleClientThread(void* param) {
    Client* client = (Client*)param;
    SOCKET clientSocket = client->socket;
    int clientId = client->id;
    std::string clientIP = client->ip;

    char buffer[BUFFER_SIZE];

    std::string welcome = "WELCOME|Fire Brigade Connected|" + getCurrentTime() + "|BACKEND:CPP";
    send(clientSocket, welcome.c_str(), (int)welcome.length(), 0);
    std::cout << "[CLIENT " << clientId << "] - Welcome sent" << std::endl;

    while (true) {
        memset(buffer, 0, BUFFER_SIZE);
        int iResult = recv(clientSocket, buffer, BUFFER_SIZE, 0);
        
        if (iResult > 0) {
            buffer[iResult] = '\0';
            std::string message(buffer);
            
            std::cout << "[CLIENT " << clientId << "] - Received: " << message << std::endl;

            std::string response = processMessage(message, clientId);
            int sendResult = send(clientSocket, response.c_str(), (int)response.length(), 0);
            
            if (sendResult == SOCKET_ERROR) {
                std::cout << "[CLIENT " << clientId << "] - Send failed" << std::endl;
                break;
            }
        }
        else if (iResult == 0) {
            std::cout << "[CLIENT " << clientId << "] - Disconnected" << std::endl;
            break;
        }
        else {
            std::cout << "[CLIENT " << clientId << "] - Recv error" << std::endl;
            break;
        }
    }

    closesocket(clientSocket);
    delete client;
    _endthreadex(0);
    return 0;
}

int main() {
    std::cout << "\n========================================" << std::endl;
    std::cout << "ISLAMABAD FIRE BRIGADE - C++ SERVER" << std::endl;
    std::cout << "========================================\n" << std::endl;

    // Initialize stations
    Station s0; s0.id = 0; s0.name = "Main Station"; s0.lat = 33.6844; s0.lon = 73.0479;
    Station s1; s1.id = 1; s1.name = "Blue Area"; s1.lat = 33.7182; s1.lon = 73.0605;
    Station s2; s2.id = 2; s2.name = "G-6 Sector"; s2.lat = 33.7100; s2.lon = 73.0800;
    Station s3; s3.id = 3; s3.name = "Margalla Road"; s3.lat = 33.7400; s3.lon = 73.0900;
    Station s4; s4.id = 4; s4.name = "Airport Road"; s4.lat = 33.6167; s4.lon = 73.0992;
    
    stations.push_back(s0);
    stations.push_back(s1);
    stations.push_back(s2);
    stations.push_back(s3);
    stations.push_back(s4);

    std::cout << "[INFO] Initialized 5 fire stations" << std::endl;

    // Initialize graph
    roadNetwork = new Graph(5);
    roadNetwork->addEdge(0, 1, 6);
    roadNetwork->addEdge(0, 3, 8);
    roadNetwork->addEdge(1, 2, 5);
    roadNetwork->addEdge(1, 4, 12);
    roadNetwork->addEdge(2, 3, 7);
    roadNetwork->addEdge(3, 4, 15);
    roadNetwork->addEdge(0, 2, 9);
    roadNetwork->addEdge(2, 4, 13);
    roadNetwork->addEdge(1, 3, 10);
    roadNetwork->addEdge(0, 4, 18);

    std::cout << "[INFO] Road network initialized" << std::endl;

    // Initialize Winsock
    WSADATA wsaData;
    int iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (iResult != 0) {
        std::cerr << "[ERROR] WSAStartup failed" << std::endl;
        return 1;
    }

    std::cout << "[OK] Winsock initialized" << std::endl;

    // Create socket
    SOCKET listenSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (listenSocket == INVALID_SOCKET) {
        std::cerr << "[ERROR] Socket creation failed" << std::endl;
        WSACleanup();
        return 1;
    }

    // Bind socket
    sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_addr.s_addr = inet_addr("127.0.0.1");
    serverAddr.sin_port = htons(PORT);

    iResult = bind(listenSocket, (sockaddr*)&serverAddr, sizeof(serverAddr));
    if (iResult == SOCKET_ERROR) {
        std::cerr << "[ERROR] Bind failed" << std::endl;
        closesocket(listenSocket);
        WSACleanup();
        return 1;
    }

    std::cout << "[OK] Socket bound to 127.0.0.1:5000" << std::endl;

    // Listen
    if (listen(listenSocket, SOMAXCONN) == SOCKET_ERROR) {
        std::cerr << "[ERROR] Listen failed" << std::endl;
        closesocket(listenSocket);
        WSACleanup();
        return 1;
    }

    std::cout << "\n[READY] Server waiting for connections on port 5000..." << std::endl;
    std::cout << "========================================\n" << std::endl;

    int clientId = 0;
    while (true) {
        sockaddr_in clientAddr;
        int clientAddrLen = sizeof(clientAddr);

        SOCKET clientSocket = accept(listenSocket, (sockaddr*)&clientAddr, &clientAddrLen);
        if (clientSocket == INVALID_SOCKET) {
            std::cerr << "[ERROR] Accept failed" << std::endl;
            continue;
        }

        std::string clientIP = inet_ntoa(clientAddr.sin_addr);
        
        std::cout << "\n[CONNECTION] Client connected!" << std::endl;
        std::cout << "  IP: " << clientIP << std::endl;
        std::cout << "  ID: " << clientId << std::endl;
        std::cout << "  Time: " << getCurrentTime() << std::endl << std::endl;

        Client* newClient = new Client();
        newClient->socket = clientSocket;
        newClient->ip = clientIP;
        newClient->id = clientId;

        unsigned int threadId;
        _beginthreadex(NULL, 0, handleClientThread, newClient, 0, &threadId);

        clientId++;
    }

    closesocket(listenSocket);
    WSACleanup();
    return 0;
}