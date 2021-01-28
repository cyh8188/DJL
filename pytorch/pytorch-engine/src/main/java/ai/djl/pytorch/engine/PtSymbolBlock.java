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
package ai.djl.pytorch.engine;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.BlockList;
import ai.djl.nn.ParameterList;
import ai.djl.nn.SymbolBlock;
import ai.djl.pytorch.jni.IValueUtils;
import ai.djl.pytorch.jni.JniUtils;
import ai.djl.training.ParameterStore;
import ai.djl.training.initializer.Initializer;
import ai.djl.util.NativeResource;
import ai.djl.util.PairList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code PtSymbolBlock} is the PyTorch implementation of {@link SymbolBlock}.
 *
 * <p>You can create a {@code PtSymbolBlock} using {@link ai.djl.Model#load(java.nio.file.Path,
 * String)}.
 */
// TODO: Memory handling
public class PtSymbolBlock extends NativeResource<Long> implements SymbolBlock {

    private static final Logger logger = LoggerFactory.getLogger(PtSymbolBlock.class);
    private PtNDManager manager;
    private boolean isTrain;
    private PairList<String, Shape> inputDescriptions;
    private PairList<String, Shape> outputDescriptions;
    private boolean first;

    /**
     * Constructs a {@code PtSymbolBlock}.
     *
     * <p>You can create a {@code PtSymbolBlock} using {@link ai.djl.Model#load(java.nio.file.Path,
     * String)}.
     *
     * @param manager the manager to use for the block
     * @param handle the module handle
     */
    public PtSymbolBlock(PtNDManager manager, long handle) {
        super(handle);
        this.manager = manager;
        manager.attach(getUid(), this);
        // training mode is on by default
        isTrain = true;
        first = true;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        Long pointer = handle.getAndSet(null);
        if (pointer != null) {
            JniUtils.deleteModule(pointer);
            manager.detach(getUid());
            manager = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeLastBlock() {
        throw new UnsupportedOperationException("Not supported for PyTorch");
    }

    /** {@inheritDoc} */
    @Override
    public NDList forward(
            ParameterStore parameterStore,
            NDList inputs,
            NDList output,
            boolean training,
            PairList<String, Object> params) {
        // TODO refactor the forward to not take ParameterStore
        if (isTrain != training) {
            isTrain = training;
            if (isTrain) {
                JniUtils.enableTrainingMode(this);
            } else {
                JniUtils.enableInferenceMode(this);
            }
        }
        if (first) {
            synchronized (PtSymbolBlock.class) {
                if (first) {
                    inputDescriptions = new PairList<>();
                    outputDescriptions = new PairList<>();
                    for (NDArray array : inputs) {
                        inputDescriptions.add(array.getName(), array.getShape());
                    }
                    NDList outputs = IValueUtils.forward(this, inputs, output, training);
                    for (NDArray array : outputs) {
                        outputDescriptions.add(array.getName(), array.getShape());
                    }
                    first = false;
                    return outputs;
                }
            }
        }
        return IValueUtils.forward(this, inputs, output, training);
    }

    /** {@inheritDoc} */
    @Override
    public void setInitializer(Initializer initializer) {
        throw new UnsupportedOperationException("Not supported for PyTorch");
    }

    /** {@inheritDoc} */
    @Override
    public void setInitializer(Initializer initializer, String paramName) {
        throw new UnsupportedOperationException("Not supported for PyTorch");
    }

    /** {@inheritDoc} */
    @Override
    public Shape[] initialize(NDManager manager, DataType dataType, Shape... inputShapes) {
        throw new UnsupportedOperationException("Not supported for PyTorch");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInitialized() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void cast(DataType dataType) {
        throw new UnsupportedOperationException("Not supported for PyTorch");
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported for PyTorch");
    }

    /** {@inheritDoc} */
    @Override
    public PairList<String, Shape> describeInput() {
        if (inputDescriptions == null) {
            logger.warn(
                    "Input shapes are unknown, please run predict or forward once"
                            + "and call describeInput again.");
        }
        return inputDescriptions;
    }

    /** {@inheritDoc} */
    @Override
    public PairList<String, Shape> describeOutput() {
        if (outputDescriptions == null) {
            logger.warn(
                    "Output shapes are unknown, please run predict or forward once"
                            + "and call describeOutput again.");
        }
        return outputDescriptions;
    }

    /** {@inheritDoc} */
    @Override
    public BlockList getChildren() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public ParameterList getDirectParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public ParameterList getParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public Shape getParameterShape(String name, Shape[] inputShapes) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public Shape[] getOutputShapes(NDManager manager, Shape[] inputShapes) {
        return new Shape[0];
    }

    /** {@inheritDoc} */
    @Override
    public void saveParameters(DataOutputStream os) {
        throw new UnsupportedOperationException("Not supported for PyTorch");
    }

    /** {@inheritDoc} */
    @Override
    public void loadParameters(NDManager manager, DataInputStream is) {
        throw new UnsupportedOperationException("Not supported for PyTorch");
    }
}
