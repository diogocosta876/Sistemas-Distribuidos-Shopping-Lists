cmake_minimum_required(VERSION 3.0)
project(SDLE_proj)  # Replace "YourProjectName" with your actual project name

find_package(cppzmq REQUIRED)

add_executable(exec hello_world.cpp)  # Replace "YourExecutableName" with the desired executable name
target_link_libraries(exec cppzmq)