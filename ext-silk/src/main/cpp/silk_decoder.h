//
// Created by hanter on 2023/2/24.
//

#ifndef EXT_SILK_DECODER_H
#define EXT_SILK_DECODER_H

#include "SKP_Silk_control.h"

#ifdef __cplusplus
extern "C"
{
#endif

typedef struct {
    void *psDec;
    SKP_SILK_SDK_DecControlStruct decControl;
} silk_decoder;

#ifdef __cplusplus
}
#endif

#endif //EXT_SILK_DECODER_H
