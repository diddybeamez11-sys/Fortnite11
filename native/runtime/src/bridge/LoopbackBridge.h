#pragma once
#include "bridge/GameBridge.h"
#include <atomic>
#include <thread>

namespace eclient_runtime {
class LoopbackBridge {
public:
    void start(void* logger = nullptr);
    void stop();
private:
    void run(void* logger);
    std::atomic_bool running_{false};
    std::thread thread_{};
};
}
