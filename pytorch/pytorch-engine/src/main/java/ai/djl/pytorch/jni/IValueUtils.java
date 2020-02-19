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

package ai.djl.pytorch.jni;

import ai.djl.ndarray.NDList;
import ai.djl.pytorch.engine.PtNDArray;
import ai.djl.pytorch.engine.PtNDManager;
import ai.djl.pytorch.engine.PtSymbolBlock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** IValueUtils is utility class to deal with IValue in PyTorch. */
public final class IValueUtils {

    private IValueUtils() {}

    /**
     * Create IValue Pointer from NDArray.
     *
     * @param array {@link PtNDArray}
     * @return IValue Pointer
     */
    public static Pointer toIValuePointer(PtNDArray array) {
        return PyTorchLibrary.LIB.iValueCreateFromTensor(array.getHandle());
    }

    /**
     * Check IValue is a container of {@link PtNDArray}.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @return result
     */
    public static boolean isNDArray(Pointer iValueHandle) {
        return PyTorchLibrary.LIB.iValueIsTensor(iValueHandle);
    }

    /**
     * Check IValue is a container of {@link NDList}.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @return result
     */
    public static boolean isNDList(Pointer iValueHandle) {
        return PyTorchLibrary.LIB.iValueIsTensorList(iValueHandle);
    }

    /**
     * Check IValue is a container of IValue Array.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @return result
     */
    public static boolean isArray(Pointer iValueHandle) {
        return PyTorchLibrary.LIB.iValueIsList(iValueHandle);
    }

    /**
     * Check IValue is a container of IValue Map.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @return result
     */
    public static boolean isMap(Pointer iValueHandle) {
        return PyTorchLibrary.LIB.iValueIsMap(iValueHandle);
    }

    /**
     * Check IValue is a container of String.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @return result
     */
    public static boolean isString(Pointer iValueHandle) {
        return PyTorchLibrary.LIB.iValueIsString(iValueHandle);
    }

    /**
     * Extract IValue with a {@link PtNDArray} value.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @param manager {@link PtNDManager} that creates {@link PtNDArray}
     * @return {@link ai.djl.ndarray.NDArray}
     */
    public static PtNDArray toNDArray(Pointer iValueHandle, PtNDManager manager) {
        Pointer ndHandle = PyTorchLibrary.LIB.iValueToTensor(iValueHandle);
        return new PtNDArray(manager, ndHandle);
    }

    /**
     * Extract IValue to {@link NDList}.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @param manager {@link PtNDManager} that creates {@link PtNDArray}
     * @return {@link NDList}
     */
    public static NDList toNDList(Pointer iValueHandle, PtNDManager manager) {
        Pointer[] ndHandles = PyTorchLibrary.LIB.iValueToTensorList(iValueHandle);
        NDList list = new NDList();
        for (Pointer handle : ndHandles) {
            list.add(new PtNDArray(manager, handle));
        }
        return list;
    }

    /**
     * Extract IValue to String.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @return String
     */
    public static String toString(Pointer iValueHandle) {
        return PyTorchLibrary.LIB.iValueToString(iValueHandle);
    }

    /**
     * Extract IValue to an IValue Array.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @return IValue array
     */
    public static Pointer[] toIValueArray(Pointer iValueHandle) {
        return PyTorchLibrary.LIB.iValueToList(iValueHandle);
    }

    /**
     * Extract IValue to a Map.
     *
     * @param iValueHandle IValue {@link Pointer}
     * @return IValue Map
     */
    public static Map<Pointer, Pointer> toIValueMap(Pointer iValueHandle) {
        Pointer[] iValueHandles = PyTorchLibrary.LIB.iValueToMap(iValueHandle);
        Map<Pointer, Pointer> map = new ConcurrentHashMap<>();
        for (int i = 0; i < iValueHandles.length; i += 2) {
            map.put(iValueHandles[i], iValueHandles[i + 1]);
        }
        return map;
    }

    /**
     * Run the forward of PyTorch module.
     *
     * @param block the block that contains PyTorch module.
     * @param inputs input {@link NDList}
     * @return result {@link NDList}
     */
    public static NDList forward(PtSymbolBlock block, NDList inputs) {
        Pointer[] iValuesHandles =
                inputs.stream()
                        .map(input -> toIValuePointer((PtNDArray) input))
                        .toArray(Pointer[]::new);
        Pointer result = PyTorchLibrary.LIB.moduleForward(block.getHandle(), iValuesHandles);
        if (isNDArray(result)) {
            return new NDList(toNDArray(result, (PtNDManager) inputs.get(0).getManager()));
        } else if (isNDList(result)) {
            return toNDList(result, (PtNDManager) inputs.get(0).getManager());
        } else {
            throw new UnsupportedOperationException("Unsupported IValue type");
        }
    }
}
