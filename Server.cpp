#include <winsock2.h>
#include <iostream>

#pragma comment(lib, "ws2_32.lib")
using namespace std;

int main() {

    WSADATA wsa;
    SOCKET serverSocket, clientSocket;
    sockaddr_in serverAddr, clientAddr;
    int clientSize = sizeof(clientAddr);
    char buffer[1024];

    WSAStartup(MAKEWORD(2,2), &wsa);

    serverSocket = socket(AF_INET, SOCK_STREAM, 0);

    serverAddr.sin_family = AF_INET;
    serverAddr.sin_addr.s_addr = INADDR_ANY;
    serverAddr.sin_port = htons(9000);

    bind(serverSocket, (sockaddr*)&serverAddr, sizeof(serverAddr));
    listen(serverSocket, 1);

    cout << "Server waiting...\n";
    clientSocket = accept(serverSocket, (sockaddr*)&clientAddr, &clientSize);
    cout << "Client connected\n";

    while (true) {
        memset(buffer, 0, sizeof(buffer));
        recv(clientSocket, buffer, sizeof(buffer), 0);

        double lat, lon;
        sscanf(buffer, "%lf,%lf", &lat, &lon);

        cout << "Fire at: " << lat << " , " << lon << endl;
    }

    closesocket(serverSocket);
    WSACleanup();
}
