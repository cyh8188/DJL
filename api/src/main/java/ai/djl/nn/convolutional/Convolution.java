/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package ai.djl.nn.convolutional;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.internal.NDArrayEx;
import ai.djl.ndarray.types.LayoutType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.Parameter;
import ai.djl.nn.ParameterBlock;
import ai.djl.nn.ParameterType;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A convolution layer does a dot product calculation on each channel of \(k\)-channel input data by
 * specified number of filters, each containing \(k\) kernels for calculating each channel in the
 * input data and then summed per filter, hence the number of filters denote the number of output
 * channels of a convolution layer. Some modifications may be set on a convolution layer, namely
 * stride which shows the distance between each convolved input data in a channel, and padding which
 * shows the preservation of input size (width and/or height and/or depth) by adding specified
 * padding to the sides of the output. A convolution layer extracts features of input data with
 * different representations where each representation lies per channel in the output, often known
 * as feature map or feature vector.
 *
 * <p>While convolution process itself has been around for quite some time in mathematics, in 1998
 * LeCun <i>et al.</i> implemented the very first convolution layers forming a network called
 * LeNet-5 for character recognition task; details of the network's implementation can be find in
 * LeNet-5's <a href="http://yann.lecun.com/exdb/publis/pdf/lecun-01a.pdf">paper</a>. When other
 * approaches at that time used handcrafted features with external stage of feature extraction,
 * convolution layer performed feature extraction on its own with no human interference. This marks
 * a new era of machine-extracted features, but it was not until 2012 that the published <a
 * href="https://papers.nips.cc/paper/4824-imagenet-classification-with-deep-convolutional-neural-networks.pdf">
 * paper</a> of AlexNet marked the beginning of convolutional neural networks, which by the name
 * itself heavily relies on convolution layer.
 *
 * <p>Convolution layer is usually used in image-related tasks due to its well-renowned performance
 * as shown by existing works and currently, other non-image-related fields of study are beginning
 * to incorporate convolution layer as an addition or replacement of previous approaches, with one
 * example being time series processing with 1-dimensional convolution layer. Due to the nature of
 * convolution that processes all points in the input data, it is computationally expensive, hence
 * the use of GPU is strongly recommended for faster performance as opposed to using CPU. Note that
 * it is also common to stack convolution layers with different output channels for more
 * representations of the input data.
 *
 * <p>Current implementations of {@code Convolution} are {@link Conv1D} with input dimension of
 * {@link LayoutType#WIDTH}, {@link Conv2D} with input dimension of {@link LayoutType#WIDTH} and
 * {@link LayoutType#HEIGHT}, and lastly {@link Conv3D} with input dimension of {@link
 * LayoutType#WIDTH}, {@link LayoutType#HEIGHT}, and {@link LayoutType#DEPTH}. These implementations
 * share the same core principal as a {@code Convolution} layer does, with the difference being the
 * number of input dimension each operates on as denoted by {@code ConvXD} for {@code X}
 * dimension(s).
 */
public abstract class Convolution extends ParameterBlock {

    private static final byte VERSION = 2;

    protected Shape kernel;
    protected Shape stride;
    protected Shape pad;
    protected Shape dilate;
    protected int numFilters;
    protected int numGroups;
    protected boolean includeBias;

    protected Parameter weight;
    protected Parameter bias;

    /**
     * Creates a {@link Convolution} object.
     *
     * @param builder the {@code Builder} that has the necessary configurations
     */
    public Convolution(ConvolutionBuilder<?> builder) {
        kernel = builder.kernel;
        stride = builder.stride;
        pad = builder.pad;
        dilate = builder.dilate;
        numFilters = builder.numFilters;
        numGroups = builder.numGroups;
        includeBias = builder.includeBias;

        weight = new Parameter("weight", this, ParameterType.WEIGHT);
        if (includeBias) {
            bias = new Parameter("bias", this, ParameterType.BIAS);
        }
    }

    /**
     * Returns the expected layout of the input.
     *
     * @return the expected layout of the input
     */
    protected abstract LayoutType[] getExpectedLayout();

    /**
     * Returns the string representing the layout of the input.
     *
     * @return the string representing the layout of the input
     */
    protected abstract String getStringLayout();

    /**
     * Returns the number of dimensions of the input.
     *
     * @return the number of dimensions of the input
     */
    protected abstract int numDimensions();

    /** {@inheritDoc} */
    @Override
    public NDList forward(
            ParameterStore parameterStore, NDList inputs, PairList<String, Object> params) {
        inputs = opInputs(parameterStore, inputs);
        NDArrayEx ex = inputs.head().getNDArrayInternal();
        return ex.convolution(
                inputs,
                kernel,
                stride,
                pad,
                dilate,
                numFilters,
                numGroups,
                getStringLayout(),
                !includeBias,
                params);
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeInitialize(Shape[] inputs) {
        this.inputShapes = inputs;
        Shape inputShape = inputs[0];
        Block.validateLayout(getExpectedLayout(), inputShape.getLayout());
    }

    /** {@inheritDoc} */
    @Override
    public Shape[] getOutputShapes(NDManager manager, Shape[] inputs) {
        long[] shape = new long[numDimensions()];
        shape[0] = inputs[0].get(0);
        shape[1] = numFilters;
        for (int i = 0; i < numDimensions() - 2; i++) {
            shape[2 + i] =
                    (inputs[0].get(2 + i)
                                            + 2 * pad.get(i)
                                            - dilate.get(0) * (kernel.get(i) - 1)
                                            - 1)
                                    / stride.get(0)
                            + 1;
        }
        return new Shape[] {new Shape(shape)};
    }

    /** {@inheritDoc} */
    @Override
    public Shape getParameterShape(String name, Shape[] inputShapes) {
        Shape shape = inputShapes[0];
        switch (name) {
            case "weight":
                return new Shape(numFilters, shape.get(1)).addAll(kernel);
            case "bias":
                return new Shape(numFilters);
            default:
                throw new IllegalArgumentException("Invalid parameter name");
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Parameter> getDirectParameters() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(weight);
        if (includeBias) {
            parameters.add(bias);
        }
        return parameters;
    }

    /** {@inheritDoc} */
    @Override
    public void saveParameters(DataOutputStream os) throws IOException {
        os.writeByte(VERSION);
        saveInputShapes(os);
        weight.save(os);
        if (bias != null) {
            bias.save(os);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void loadParameters(NDManager manager, DataInputStream is)
            throws IOException, MalformedModelException {
        byte version = is.readByte();
        if (version == VERSION) {
            readInputShapes(is);
        } else if (version != 1) {
            throw new MalformedModelException("Unsupported encoding version: " + version);
        }
        weight.load(manager, is);
        if (bias != null) {
            bias.load(manager, is);
        }
    }

    private NDList opInputs(ParameterStore parameterStore, NDList inputs) {
        NDArray data = inputs.singletonOrThrow();
        Device device = data.getDevice();
        NDList ret = new NDList(3);
        ret.add(data);
        ret.add(parameterStore.getValue(weight, device));
        if (bias != null) {
            ret.add(parameterStore.getValue(bias, device));
        }
        return ret;
    }

    /**
     * A builder that can build any {@code Convolution} block.
     *
     * @param <T> the type of {@code Convolution} block to build
     */
    @SuppressWarnings("rawtypes")
    public abstract static class ConvolutionBuilder<T extends ConvolutionBuilder> {

        protected Shape kernel;
        protected Shape stride;
        protected Shape pad;
        protected Shape dilate;
        protected int numFilters;
        protected int numGroups = 1;
        protected boolean includeBias = true;

        /**
         * Sets the shape of the kernel.
         *
         * @param kernel the shape of the kernel
         * @return this Builder
         */
        public T setKernel(Shape kernel) {
            this.kernel = kernel;
            return self();
        }

        /**
         * Sets the stride of the convolution. Defaults to 1 in each dimension.
         *
         * @param stride the shape of the stride
         * @return this Builder
         */
        public T optStride(Shape stride) {
            this.stride = stride;
            return self();
        }

        /**
         * Sets the padding along each dimension. Defaults to 0 along each dimension.
         *
         * @param pad the shape of padding along each dimension
         * @return this Builder
         */
        public T optPad(Shape pad) {
            this.pad = pad;
            return self();
        }

        /**
         * Sets the dilation along each dimension. Defaults to 1 along each dimension.
         *
         * @param dilate the shape of dilation along each dimension
         * @return this Builder
         */
        public T optDilate(Shape dilate) {
            this.dilate = dilate;
            return self();
        }

        /**
         * Sets the <b>Required</b> number of filters.
         *
         * @param numFilters the number of convolution filters(channels)
         * @return this Builder
         */
        public T setNumFilters(int numFilters) {
            this.numFilters = numFilters;
            return self();
        }

        /**
         * Sets the number of group partitions.
         *
         * @param numGroups the number of group partitions
         * @return this Builder
         */
        public T optNumGroups(int numGroups) {
            this.numGroups = numGroups;
            return self();
        }

        /**
         * Sets the optional parameter of whether to include a bias vector. Includes bias by
         * default.
         *
         * @param includeBias whether to use a bias vector parameter
         * @return this Builder
         */
        public T optBias(boolean includeBias) {
            this.includeBias = includeBias;
            return self();
        }

        /**
         * Validates that the required arguments are set.
         *
         * @throws IllegalArgumentException if the required arguments are not set
         */
        protected void validate() {
            if (kernel == null || numFilters == 0) {
                throw new IllegalArgumentException("Kernel and numFilters must be set");
            }
        }

        protected abstract T self();
    }
}
