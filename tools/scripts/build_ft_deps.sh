#!/usr/bin/env bash

set -ex

FT_VERSION=$1
NVIDIA_TRITON_SERVER_VERSION=$2
IS_LLAMA_BUILD=$3

apt-get update && apt-get install -y rapidjson-dev

pushd /tmp

git clone https://github.com/NVIDIA/FasterTransformer.git -b ${FT_VERSION}

export FT_DIR=/tmp/FasterTransformer
mkdir -p /tmp/binaries

# Build FasterTransformer Triton library
if [ "$IS_LLAMA_BUILD" = "false" ] ; then
      git clone https://github.com/triton-inference-server/fastertransformer_backend.git
else
      echo "cloning forked FT backend repo with llama support"
      git clone https://github.com/rohithkrn/fastertransformer_backend.git -b llama_void_main
fi
mkdir -p fastertransformer_backend/build
cd fastertransformer_backend/build
cmake \
      -D CMAKE_EXPORT_COMPILE_COMMANDS=1 \
      -D CMAKE_BUILD_TYPE=Release \
      -D ENABLE_FP8=OFF \
      -D CMAKE_INSTALL_PREFIX=/opt/tritonserver \
      -D TRITON_COMMON_REPO_TAG="${NVIDIA_TRITON_SERVER_VERSION}" \
      -D TRITON_CORE_REPO_TAG="${NVIDIA_TRITON_SERVER_VERSION}" \
      -D TRITON_BACKEND_REPO_TAG="${NVIDIA_TRITON_SERVER_VERSION}" \
      ..
make -j$(nproc) install
cp /opt/tritonserver/backends/fastertransformer/*.so /tmp/binaries/
cd ../../

# Build FasterTransformer TH Ops library
mkdir -p FasterTransformer/build
cd FasterTransformer/build
git submodule init && git submodule update
cmake -DCMAKE_BUILD_TYPE=Release -DSM=70,75,80,86,90 -DBUILD_PYT=ON -DBUILD_MULTI_GPU=ON ..
make -j$(nproc)
cp lib/libth_transformer.so /tmp/binaries/
cd ../../

popd
