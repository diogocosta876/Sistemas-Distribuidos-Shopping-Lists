cmake_minimum_required(VERSION 3.10)
project(run)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_BUILD_TYPE Debug)  # Set build type to Debug
find_package(Threads REQUIRED)
find_package(cppzmq REQUIRED)

add_executable(run src/main.cpp src/db.cpp src/server.cpp src/client.cpp src/shoppinglist.cpp)
target_include_directories(run PRIVATE ${cppzmq_INCLUDE_DIR})
target_link_libraries(run PRIVATE cppzmq Threads::Threads)
