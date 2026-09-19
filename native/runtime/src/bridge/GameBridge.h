#pragma once

#include <atomic>
#include <cstdint>
#include <mutex>
#include <string>
#include <thread>

namespace eclient_runtime {

struct Vec3 { float x{}, y{}, z{}; };

struct BridgeSnapshot {
    bool libraryLoaded{false};
    bool fingerprintMatched{false};
    bool symbolsReady{false};
    bool hookInstalled{false};
    bool playerSeen{false};
    Vec3 position{};
    float yaw{};
    std::string buildId{};
    std::string libraryPath{};
    std::string status{"initializing"};
    std::uint64_t heartbeat{0};
    std::int64_t lastUpdateMs{0};
};

class GameBridge {
public:
    static GameBridge& instance();
    bool initialize();
    void shutdown();
    BridgeSnapshot snapshot() const;

private:
    GameBridge() = default;
    GameBridge(const GameBridge&) = delete;
    GameBridge& operator=(const GameBridge&) = delete;
    bool discoverMinecraft();
    bool verifyFingerprint();
    void heartbeatLoop();

    std::uintptr_t moduleBase_{0};
    std::string modulePath_{};
    std::string buildId_{};
    mutable std::mutex mutex_;
    BridgeSnapshot snapshot_{};
    std::atomic_bool running_{false};
    std::thread heartbeatThread_{};
};

} // namespace eclient_runtime
