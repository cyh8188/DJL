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
package ai.djl.paddlepaddle.jni;

import java.nio.ByteBuffer;

/** A class containing utilities to interact with the PaddlePaddle Engine's JNI layer. */
@SuppressWarnings("missingjavadocmethod")
final class PaddleLibrary {

    static final PaddleLibrary LIB = new PaddleLibrary();

    private PaddleLibrary() {}

    native long paddleCreateTensor(ByteBuffer data, long length, int[] shape, int dType);

    native void deleteTensor(long handle);

    native long createAnalysisConfig(String modelDir, String paramDir, int deviceId);

}
