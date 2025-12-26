#include <iostream>
#include <winsock2.h>
#pragma comment(lib, "ws2_32.lib")

int main() {
    WSADATA wsa;
    SOCKET serverSock, clientSock;
    sockaddr_in serverAddr, clientAddr;
    int clientSize = sizeof(clientAddr);

    WSAStartup(MAKEWORD(2,2), &wsa);

    serverSock = socket(AF_INET, SOCK_STREAM, 0);

    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(8080);
    serverAddr.sin_addr.s_addr = INADDR_ANY;

    bind(serverSock, (sockaddr*)&serverAddr, sizeof(serverAddr));
    listen(serverSock, 1);

    std::cout << "Server waiting...\n";
    clientSock = accept(serverSock, (sockaddr*)&clientAddr, &clientSize);
    std::cout << "Client connected\n";

    // Receive fire location
    double lat, lon;
    recv(clientSock, (char*)&lat, sizeof(lat), 0);
    recv(clientSock, (char*)&lon, sizeof(lon), 0);

    std::cout << "Fire at: " << lat << " , " << lon << std::endl;

    // Send route (Phase 6)
    const char* route =
        "33.6844,73.0479;"
        "33.6938,73.0652;"
        "33.7000,73.0800\n";

    send(clientSock, route, strlen(route), 0);

    closesocket(clientSock);
    closesocket(serverSock);
    WSACleanup();
    return 0;
}
