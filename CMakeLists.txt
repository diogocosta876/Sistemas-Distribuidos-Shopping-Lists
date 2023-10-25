cmake_minimum_required(VERSION 3.10)
project(run)

set(CMAKE_CXX_STANDARD 14)

find_package(cppzmq)

add_executable(run src/main.cpp src/db.cpp src/server.cpp src/client.cpp)
target_include_directories(run PRIVATE ${cppzmq_INCLUDE_DIR})
target_link_libraries(run ${cppzmq_LIBRARY})