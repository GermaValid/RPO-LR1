#include <jni.h>
#include <string>
#include <android/log.h>
#include <spdlog/spdlog.h>
#include <spdlog/sinks/android_sink.h>



#define LOG_INFO(...) __android_log_print(ANDROID_LOG_INFO, "fclient_ndk", __VA_ARGS__)
#define SLOG_INFO(...) android_logger->info( __VA_ARGS__ )

#include <mbedtls/entropy.h>
#include <mbedtls/ctr_drbg.h>
#include <mbedtls/des.h>   // понадобится дальше для 3DES
#include <cstring>
#include <vector>

static mbedtls_entropy_context entropy;
static mbedtls_ctr_drbg_context ctr_drbg;
static const char* personalization = "fclient-sample-app";
static bool rng_inited = false;

auto android_logger = spdlog::android_logger_mt("android", "fclient_ndk");

extern "C" JNIEXPORT jstring JNICALL
Java_ru_bmstu_iu3_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "I love my little bun!";
    LOG_INFO("Hello from c++ %d", 2026);
    SLOG_INFO("Hello from spdlog {0}", 2026);
    __android_log_print(ANDROID_LOG_INFO, "iu3", "mbedtls version: %s", MBEDTLS_VERSION_STRING);
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jint JNICALL
Java_ru_bmstu_iu3_MainActivity_initRng(JNIEnv* /*env*/, jclass /*clazz*/) {
    if (rng_inited) return 0;

    mbedtls_entropy_init(&entropy);
    mbedtls_ctr_drbg_init(&ctr_drbg);

    int rc = mbedtls_ctr_drbg_seed(
            &ctr_drbg,
            mbedtls_entropy_func,
            &entropy,
            reinterpret_cast<const unsigned char*>(personalization),
            std::strlen(personalization)
    );

    rng_inited = (rc == 0);
    return rc;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_ru_bmstu_iu3_MainActivity_randomBytes(JNIEnv* env, jclass /*clazz*/, jint no) {
    if (no <= 0) return env->NewByteArray(0);
    if (!rng_inited) {
        // чтобы не забыть вызвать initRng()
        return env->NewByteArray(0);
    }

    std::vector<unsigned char> buf(static_cast<size_t>(no));
    int rc = mbedtls_ctr_drbg_random(&ctr_drbg, buf.data(), buf.size());
    if (rc != 0) return env->NewByteArray(0);

    jbyteArray out = env->NewByteArray(no);
    env->SetByteArrayRegion(out, 0, no, reinterpret_cast<jbyte*>(buf.data()));
    return out;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_ru_bmstu_iu3_MainActivity_encrypt(JNIEnv *env, jclass, jbyteArray key, jbyteArray data) {
    jsize ksz = env->GetArrayLength(key);
    jsize dsz = env->GetArrayLength(data);
    if ((ksz != 16) || (dsz <= 0)) return env->NewByteArray(0);

    mbedtls_des3_context ctx;
    mbedtls_des3_init(&ctx);

    jbyte *pkey = env->GetByteArrayElements(key, 0);

    // Padding (как в методичке)
    int rst = dsz % 8;
    int sz  = dsz + 8 - rst;
    uint8_t *buf = new uint8_t[sz];

    for (int i = 7; i > rst; i--) buf[dsz + i] = (uint8_t)rst;

    jbyte *pdata = env->GetByteArrayElements(data, 0);
    std::copy(pdata, pdata + dsz, buf);

    mbedtls_des3_set2key_enc(&ctx, (uint8_t *)pkey);

    int cn = sz / 8;
    for (int i = 0; i < cn; i++)
        mbedtls_des3_crypt_ecb(&ctx, buf + i * 8, buf + i * 8);

    jbyteArray dout = env->NewByteArray(sz);
    env->SetByteArrayRegion(dout, 0, sz, (jbyte *)buf);

    delete[] buf;
    env->ReleaseByteArrayElements(key, pkey, 0);
    env->ReleaseByteArrayElements(data, pdata, 0);
    return dout;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_ru_bmstu_iu3_MainActivity_decrypt(JNIEnv *env, jclass, jbyteArray key, jbyteArray data) {
    jsize ksz = env->GetArrayLength(key);
    jsize dsz = env->GetArrayLength(data);
    if ((ksz != 16) || (dsz <= 0) || ((dsz % 8) != 0)) return env->NewByteArray(0);

    mbedtls_des3_context ctx;
    mbedtls_des3_init(&ctx);

    jbyte *pkey = env->GetByteArrayElements(key, 0);
    uint8_t *buf = new uint8_t[dsz];

    jbyte *pdata = env->GetByteArrayElements(data, 0);
    std::copy(pdata, pdata + dsz, buf);

    mbedtls_des3_set2key_dec(&ctx, (uint8_t *)pkey);

    int cn = dsz / 8;
    for (int i = 0; i < cn; i++)
        mbedtls_des3_crypt_ecb(&ctx, buf + i * 8, buf + i * 8);

    // PKCS#5 (как в методичке, упрощено)
    int sz = dsz - 8 + buf[dsz - 1];

    jbyteArray dout = env->NewByteArray(sz);
    env->SetByteArrayRegion(dout, 0, sz, (jbyte *)buf);

    delete[] buf;
    env->ReleaseByteArrayElements(key, pkey, 0);
    env->ReleaseByteArrayElements(data, pdata, 0);
    return dout;
}




