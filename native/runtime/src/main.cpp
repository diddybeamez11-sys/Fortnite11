#include "bridge/GameBridge.h"
#include "bridge/LoopbackBridge.h"

#include <jni.h>

namespace eclient_runtime {
namespace {
LoopbackBridge loopback;
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_rubidiumclient_core_runtime_NativeRuntimeBridge_nativeStart(JNIEnv*, jclass) {
    const bool ok = eclient_runtime::GameBridge::instance().initialize();
    eclient_runtime::loopback.start(nullptr);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_rubidiumclient_core_runtime_NativeRuntimeBridge_nativeStop(JNIEnv*, jclass) {
    eclient_runtime::loopback.stop();
    eclient_runtime::GameBridge::instance().shutdown();
}
