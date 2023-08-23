cmake_minimum_required(VERSION 3.0.0)

project(silk)

include_directories(.)

aux_source_directory(. SILK_SRC_LIST)

add_library(silk STATIC
        ${SILK_SRC_LIST})

target_include_directories(silk
        INTERFACE ${CMAKE_CURRENT_SOURCE_DIR})