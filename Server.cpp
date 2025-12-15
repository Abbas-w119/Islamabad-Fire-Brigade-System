#include <iostream>
#include <winsock2.h>
#include "Graph.h"
#include "Router.h"

#pragma comment(lib, "ws2_32.lib")

using namespace std;

int main() {
    WSADATA wsa;
    WSAStartup(MAKEWORD(2,2), &wsa);

    SOCKET serverSocket = socket(AF_INET, SOCK_STREAM, 0);

    sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(8080);
    serverAddr.sin_addr.s_addr = INADDR_ANY;

    bind(serverSocket, (sockaddr*)&serverAddr, sizeof(serverAddr));
    listen(serverSocket, SOMAXCONN);

    cout << "🔥 Fire-Brigade C++ Server Running...\n";

    // ---- Build Islamabad Map ----
    Graph graph(6);
    graph.addEdge(0, 1, 5);  // Zero Point -> Blue Area
    graph.addEdge(1, 2, 6);  // Blue Area -> F-10
    graph.addEdge(1, 4, 4);  // Blue Area -> G-9
    graph.addEdge(4, 2, 3);  // G-9 -> F-10
    graph.addEdge(3, 0, 7);  // Faizabad -> Zero Point
    graph.addEdge(5, 3, 5);  // I-8 -> Faizabad

    while (true) {  
        SOCKET clientSocket = accept(serverSocket, NULL, NULL);
        cout << "Client connected\n";

        char buffer[1024];
        int bytes = recv(clientSocket, buffer, 1024, 0);
        buffer[bytes] = '\0';

        int fireNode = atoi(buffer);

        string response = Router::processRequest(graph, fireNode);
        send(clientSocket, response.c_str(), response.length(), 0);

        closesocket(clientSocket);
    }

    closesocket(serverSocket);
    WSACleanup();
    return 0;
}
