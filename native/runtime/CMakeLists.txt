cmake_minimum_required(VERSION 3.22)
project(eclient_runtime LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT ANDROID)
    message(FATAL_ERROR "E-Client native runtime must be built with the Android NDK")
endif()
if(NOT CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "E-Client native runtime currently targets arm64-v8a only")
endif()

add_library(eclient_runtime SHARED
    src/main.cpp
    src/bridge/GameBridge.cpp
    src/bridge/LoopbackBridge.cpp
)

target_include_directories(eclient_runtime PRIVATE src)
target_compile_options(eclient_runtime PRIVATE -Wall -Wextra -Werror -fvisibility=hidden -fvisibility-inlines-hidden)
target_link_options(eclient_runtime PRIVATE "-Wl,-z,max-page-size=16384")

find_library(log-lib log REQUIRED)
target_link_libraries(eclient_runtime PRIVATE ${log-lib})

if(CMAKE_STRIP)
    add_custom_command(TARGET eclient_runtime POST_BUILD
        COMMAND "${CMAKE_STRIP}" --strip-unneeded "$<TARGET_FILE:eclient_runtime>"
        COMMENT "Stripping E-Client native runtime"
        VERBATIM
    )
endif()
