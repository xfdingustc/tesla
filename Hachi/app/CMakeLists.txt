# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
             avrpro-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/source/base/avrpro_malloc.cpp
             src/main/cpp/source/base/sqlite3.c
             src/main/cpp/source/filters/avrpro_filter_if.cpp
             src/main/cpp/source/filters/avrpro_filter_manager.cpp
             src/main/cpp/source/filters/avrprofilter.cpp)
#add_library(lib_sqlite SHARED IMPORTED)
#set_target_properties(lib_sqlite PROPERTIES IMPORTED_LOCATION
#    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi/libsqlite.so)

set_target_properties(avrpro-lib
                      PROPERTIES
                      LIBRARY_OUTPUT_DIRECTORY
                      "${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}")


# Specifies a path to native header files.
include_directories(
    ${CMAKE_SOURCE_DIR}/src/main/cpp/include
    ${CMAKE_SOURCE_DIR}/src/main/source/include)


target_link_libraries(
                       avrpro-lib
                       android
                       log
                    )