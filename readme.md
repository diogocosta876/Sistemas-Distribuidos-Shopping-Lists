## Build steps:

1. (Probably already done)

   Build libzmq via cmake.
   
   This does an out-of-source build and installs the build files.
   
   Download and unzip the lib, cd to directory:

   mkdir build

    cd build

    cmake ..

    sudo make -j4 install



2. Download ZIP at: https://github.com/zeromq/cppzmq

    Build cppzmq via cmake. 

    This does an out of source build and installs the build files

    download and unzip the lib, cd to directory

    mkdir build

    cd build

    cmake ..

    sudo make -j4 install
