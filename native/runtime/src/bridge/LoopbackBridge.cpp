#include "bridge/LoopbackBridge.h"
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include <cstdio>
#include <chrono>
#include <thread>

namespace eclient_runtime {
void LoopbackBridge::start(void* logger) {
    (void)logger;
    if (running_.exchange(true)) return;
    thread_ = std::thread([this, logger] { run(logger); });
}
void LoopbackBridge::stop() {
    if (!running_.exchange(false)) return;
    if (thread_.joinable()) thread_.join();
}
void LoopbackBridge::run(void* logger) {
    (void)logger;
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) return;
    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(38170);
    inet_pton(AF_INET, "127.0.0.1", &addr.sin_addr);
    int one = 1;
    setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));
    if (bind(fd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) != 0 || listen(fd, 1) != 0) {
        close(fd);
        return;
    }
    while (running_) {
        timeval tv{0, 250000};
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
        int c = accept(fd, nullptr, nullptr);
        if (c < 0) continue;
        while (running_) {
            const auto s = GameBridge::instance().snapshot();
            char out[2048];
            const int n = std::snprintf(out, sizeof(out),
                "{\"minecraft\":\"1.21.80\",\"protocol\":800,\"library\":%s,\"fingerprint\":%s,\"symbols\":%s,\"hook\":%s,\"player\":%s,\"buildId\":\"%s\",\"path\":\"%s\",\"x\":%.3f,\"y\":%.3f,\"z\":%.3f,\"yaw\":%.3f,\"heartbeat\":%llu,\"timestamp\":%lld,\"status\":\"%s\"}\n",
                s.libraryLoaded?"true":"false", s.fingerprintMatched?"true":"false",
                s.symbolsReady?"true":"false", s.hookInstalled?"true":"false",
                s.playerSeen?"true":"false", s.buildId.c_str(), s.libraryPath.c_str(),
                s.position.x,s.position.y,s.position.z,s.yaw,
                static_cast<unsigned long long>(s.heartbeat), static_cast<long long>(s.lastUpdateMs), s.status.c_str());
            if (send(c, out, static_cast<size_t>(n), MSG_NOSIGNAL) < 0) break;
            std::this_thread::sleep_for(std::chrono::milliseconds(250));
        }
        close(c);
    }
    close(fd);
}
}
