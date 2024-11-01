#include <jni.h>
#include <malloc.h>

jbyteArray process(JNIEnv*, jbyteArray, jbyteArray);

JNIEXPORT jbyteArray JNICALL
Java_edu_mk_code_persistence_ExportCryptor_encrypt(
        JNIEnv *env,
        jobject thiz,
        jbyteArray password,
        jbyteArray plain
        ) {
    return process(env, password, plain);
}

JNIEXPORT jbyteArray JNICALL
Java_edu_mk_code_persistence_ExportCryptor_decrypt(
        JNIEnv *env,
        jobject thiz,
        jbyteArray password,
        jbyteArray encrypted
        ) {
    return process(env, password, encrypted);
}

jbyteArray process(JNIEnv *env, jbyteArray password, jbyteArray data) {
    jsize password_len = (*env)->GetArrayLength(env, password);
    jbyte *password_bytes = (*env)->GetByteArrayElements(env, password, 0);

    jsize data_len = (*env)->GetArrayLength(env, data);
    jbyte *data_bytes = (*env)->GetByteArrayElements(env, data, 0);

    int xorred_byte;
    jbyte *result_bytes = (jbyte *) malloc(data_len);
    for (int i = 0; i < data_len; i++) {
        xorred_byte = data_bytes[i] ^ password_bytes[i % password_len];
        result_bytes[i] = (jbyte) (xorred_byte & 0xFF);
    }

    jbyteArray result = (*env)->NewByteArray(env, data_len);
    (*env)->SetByteArrayRegion(env, result, 0, data_len, result_bytes);

    (*env)->ReleaseByteArrayElements(env, password, password_bytes, 0);
    (*env)->ReleaseByteArrayElements(env, data, data_bytes, 0);
    free(result_bytes);

    return result;
}
