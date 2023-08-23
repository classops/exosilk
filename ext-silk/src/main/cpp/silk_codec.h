//
// Created by ketian on 16-9-23.
//

#ifndef JNI_SILK_H
#define JNI_SILK_H

#include <android/log.h>
#include <stdio.h>
#include "log.h"

unsigned long GetHighResolutionTime();

int convert_silk_to_pcm(const char *src, const FILE *dest, const int rate);

int convert_pcm_to_silk(const char *src, const char *dest, const int rate);


#endif //JNI_SILK_H
