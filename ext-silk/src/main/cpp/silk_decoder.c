//
// Created by hanter on 2023/2/24.
//

#include "silk_decoder.h"
#include "SKP_Silk_SDK_API.h"
#include "SKP_Silk_control.h"

#include <jni.h>
#include <malloc.h>
#include <android/log.h>
#include "include/log.h"
#include "silk_codec.h"


JNIEXPORT jlong JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_createSilkDecoder(JNIEnv *env, jclass clazz, jint sampleRate) {
    silk_decoder *decoder = malloc(sizeof(silk_decoder));
    decoder->decControl.API_sampleRate = sampleRate;
    decoder->decControl.framesPerPacket = 1;
    SKP_int32 decSizeBytes;
    int ret = SKP_Silk_SDK_Get_Decoder_Size(&decSizeBytes);
    if (ret) {
        printf("SKP_Silk_SDK_Get_Decoder_Size returned %d\n", ret);
    }
    decoder->psDec = malloc(decSizeBytes);
    ret = SKP_Silk_SDK_InitDecoder(decoder->psDec);
    if (ret) {
        printf("SKP_Silk_InitDecoder returned %d\n", ret);
    }
    return (jlong) decoder;
}

JNIEXPORT jint JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_getFrameSize(JNIEnv *env, jclass clazz, jlong ps_dec) {
    silk_decoder *decoder = (silk_decoder *) ps_dec;
    return decoder->decControl.frameSize;
}

JNIEXPORT jint JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_getMoreInternalDecoderFrames(JNIEnv *env, jclass clazz,
                                                                    jlong ps_dec) {
    silk_decoder *decoder = (silk_decoder *) ps_dec;
    return decoder->decControl.moreInternalDecoderFrames;
}

JNIEXPORT jint JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_getFramesPerPacket(JNIEnv *env, jclass clazz, jlong ps_dec) {
    silk_decoder *decoder = (silk_decoder *) ps_dec;
    return decoder->decControl.framesPerPacket;
}

JNIEXPORT jint JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_decode(JNIEnv *env, jclass clazz, jlong p_dec, jint lost_flag,
                                         jbyteArray in_data, jint in_offset, jint length,
                                         jbyteArray out_data, jint out_offset) {
    silk_decoder *decoder = (silk_decoder *) p_dec;
    jbyte *in = (*env)->GetByteArrayElements(env, in_data, 0);
    jbyte *out = (*env)->GetByteArrayElements(env, out_data, 0);
    SKP_int16 nSamplesOut;
    SKP_int ret = SKP_Silk_SDK_Decode(decoder->psDec, &(decoder->decControl),
                                      lost_flag, (unsigned char *) in + in_offset,
                                      length,
                                      (SKP_int16 *) (out + out_offset), &nSamplesOut);
    if (ret) {
        printf("SKP_Silk_Decode returned %d\n", ret);
    }
    return nSamplesOut * (int) sizeof(SKP_int16);
}

JNIEXPORT jint JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_decodeByteBuffer(JNIEnv *env, jclass clazz, jlong p_dec,
                                                   jint lost_flag, jbyteArray in_data,
                                                   jint in_offset, jint length, jobject out_data) {
    silk_decoder *decoder = (silk_decoder *) p_dec;
    jbyte *in = (*env)->GetByteArrayElements(env, in_data, 0);
    SKP_int16 *outputBufferData = (*env)->GetDirectBufferAddress(env, out_data);

    SKP_int16 nSamplesOut;
    SKP_int ret = SKP_Silk_SDK_Decode(decoder->psDec, &(decoder->decControl),
                                      lost_flag, (unsigned char *) in + in_offset,
                                      length, outputBufferData, &nSamplesOut);
    if (ret) {
        printf("SKP_Silk_Decode returned %d\n", ret);
    }
    return nSamplesOut * (int) sizeof(SKP_int16);
}

JNIEXPORT jint JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_searchForLBRR(JNIEnv *env, jclass clazz, jbyteArray payload_buff,
                                                jint offset, jint length, jint loss_offset,
                                                jbyteArray payload_fec) {
    jbyte *payload = (*env)->GetByteArrayElements(env, payload_buff, 0);
    jbyte *data = (*env)->GetByteArrayElements(env, payload_fec, 0);
    SKP_int16 fecNumBytes;
    SKP_Silk_SDK_search_for_LBRR((unsigned char *) payload + offset, length,
                                 loss_offset, (unsigned char *) data,
                                 &fecNumBytes);
    return fecNumBytes;
}

JNIEXPORT void JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_releaseDecoder(JNIEnv *env, jclass clazz, jlong p_dec) {
    free((void *) p_dec);
}

JNIEXPORT jint JNICALL
Java_com_github_classops_exoplayer_ext_silk_Silk_pcmToSilk(JNIEnv *env, jclass clazz, jstring src, jstring dest,
                                           jint rate) {
    const char *src_c = (*env)->GetStringUTFChars(env, src, NULL);
    const char *dest_c = (*env)->GetStringUTFChars(env, dest, NULL);

    LOG_D("convert %s to %s", src_c, dest_c);
    if (convert_pcm_to_silk(src_c, dest_c, rate) != 0) {
        LOG_D("convert pcm to silk failed");
        return -1;
    }

    return 0;
}