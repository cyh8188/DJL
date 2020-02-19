/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
#include "../build/include/ai_djl_pytorch_jni_PyTorchLibrary.h"
#include "djl_pytorch_jni_utils.h"

// The file is the implementation for PyTorch tensor creation ops

JNIEXPORT jobject JNICALL Java_ai_djl_pytorch_jni_PyTorchLibrary_torchFromBlob(
  JNIEnv* env,
  jobject jthis,
  jobject jbuffer,
  jlongArray jshape,
  jint jdtype,
  jint jlayout,
  jintArray jdevice,
  jboolean jrequired_grad) {
  const auto shape_vec = utils::GetVecFromJLongArray(env, jshape);
  const auto device = utils::GetDeviceFromJDevice(env, jdevice);
  auto options = torch::TensorOptions()
    .layout((jlayout == 0) ? torch::kStrided : torch::kSparse)
    .requires_grad(JNI_TRUE == jrequired_grad);
  // DJL's UNKNOWN type
  if (jdtype != 8) {
    options = options.dtype(utils::GetScalarTypeFromDType(jdtype));
  }
  torch::Tensor data = torch::from_blob(
    env->GetDirectBufferAddress(jbuffer),
    shape_vec,
    options);
  // Don't change device unless data on CPU
  if (!device.is_cpu()) {
    data = data.to(device);
  }
  const torch::Tensor* tensor_ptr = new torch::Tensor(data);
  return utils::CreatePointer<torch::Tensor>(env, tensor_ptr);
}


JNIEXPORT jobject JNICALL Java_ai_djl_pytorch_jni_PyTorchLibrary_torchEmpty(
  JNIEnv* env,
  jobject jthis,
  jlongArray jshape,
  jint jdtype,
  jint jlayout,
  jintArray jdevice,
  jboolean jrequired_grad) {
  const auto shape_vec = utils::GetVecFromJLongArray(env, jshape);
  const auto options = utils::CreateTensorOptions(env, jdtype, jlayout, jdevice, jrequired_grad);
  const torch::Tensor* tensor_ptr = new torch::Tensor(torch::empty(shape_vec, options));
  return utils::CreatePointer<torch::Tensor>(env, tensor_ptr);
}

JNIEXPORT jobject JNICALL Java_ai_djl_pytorch_jni_PyTorchLibrary_torchZeros(
  JNIEnv* env,
  jobject jthis,
  jlongArray jshape,
  jint jdtype,
  jint jlayout,
  jintArray jdevice,
  jboolean jrequired_grad) {
  const auto shape_vec = utils::GetVecFromJLongArray(env, jshape);
  const auto options = utils::CreateTensorOptions(env, jdtype, jlayout, jdevice, jrequired_grad);
  const torch::Tensor* tensor_ptr = new torch::Tensor(torch::zeros(shape_vec, options));
  return utils::CreatePointer<torch::Tensor>(env, tensor_ptr);
}

JNIEXPORT jobject JNICALL Java_ai_djl_pytorch_jni_PyTorchLibrary_torchOnes(
  JNIEnv* env,
  jobject jthis,
  jlongArray jshape,
  jint jdtype,
  jint jlayout,
  jintArray jdevice,
  jboolean jrequired_grad) {
  const auto shape_vec = utils::GetVecFromJLongArray(env, jshape);
  const auto options = utils::CreateTensorOptions(env, jdtype, jlayout, jdevice, jrequired_grad);
  const auto* tensor_ptr = new torch::Tensor(torch::ones(shape_vec, options));
  return utils::CreatePointer<torch::Tensor>(env, tensor_ptr);
}

JNIEXPORT jobject JNICALL Java_ai_djl_pytorch_jni_PyTorchLibrary_torchArange(
  JNIEnv* env,
  jobject jthis,
  jdouble jstart,
  jdouble jend,
  jdouble jstep,
  jint jdtype,
  jint jlayout,
  jintArray jdevice,
  jboolean jrequired_grad) {
  const auto options = utils::CreateTensorOptions(env, jdtype, jlayout, jdevice, jrequired_grad);
  const auto* tensor_ptr = new torch::Tensor(torch::arange(jstart, jend, jstep, options));
  return utils::CreatePointer<torch::Tensor>(env, tensor_ptr);
}

JNIEXPORT jobject JNICALL Java_ai_djl_pytorch_jni_PyTorchLibrary_torchLinspace(
  JNIEnv* env,
  jobject jthis,
  jdouble jstart,
  jdouble jend,
  jint jstep,
  jint jdtype,
  jint jlayout,
  jintArray jdevice,
  jboolean jrequired_grad) {
  const auto options = utils::CreateTensorOptions(env, jdtype, jlayout, jdevice, jrequired_grad);
  const auto* tensor_ptr = new torch::Tensor(torch::linspace(jstart, jend, jstep, options));
  return utils::CreatePointer<torch::Tensor>(env, tensor_ptr);
}

JNIEXPORT jobject JNICALL Java_ai_djl_pytorch_jni_PyTorchLibrary_torchEye(
  JNIEnv* env,
  jobject jthis,
  jint jn,
  jint jm,
  jint jdtype,
  jint jlayout,
  jintArray jdevice,
  jboolean jrequired_grad) {
  const auto options = utils::CreateTensorOptions(env, jdtype, jlayout, jdevice, jrequired_grad);
  const auto* tensor_ptr = new torch::Tensor(torch::eye(jn, jm, options));
  return utils::CreatePointer<torch::Tensor>(env, tensor_ptr);
}