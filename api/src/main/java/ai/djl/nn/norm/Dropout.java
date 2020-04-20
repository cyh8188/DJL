/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package ai.djl.nn.norm;

import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.internal.NDArrayEx;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Parameter;
import ai.djl.nn.ParameterBlock;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * A dropout layer benefits a network by allowing some units (neurons), and hence their respective
 * connections, of a network to be randomly and temporarily removed by setting its value to 0
 * <b>only</b> during training by specified probability \(p\), usually set to 0.5. The use of
 * dropout acts as if multiple networks with different architectures had been trained, and during
 * test/inference, the removed unit's output is multiplied by \(p\) as an approximation of the
 * averaged output of all the possible network architectures for that unit. The implementation of
 * dropout gives state-of-the-art performances for diverse tasks as shown in the proposal's <a
 * href="https://www.cs.toronto.edu/~hinton/absps/JMLRdropout.pdf">paper</a>, suggesting its
 * general-use capability.
 *
 * <p>The idea of dropout itself was proposed in 2014, with the purpose of improving the performance
 * of large networks due to co-adaptation, where some connections are stronger and learned more
 * while other connections become weaker and loses their impact on the prediction, resulting in
 * network overfitting. It was also created as an alternative for costly networks, such as large or
 * ensemble networks, by removing several units, hence creating different thinned network
 * architectures and simulates multiple networks within a single network, greatly reducing the
 * computation cost.
 *
 * <p>Dropout is recommended to be used when one is trying to optimize an overfitting network or
 * when large dataset is available. It is still quite commonly used in many publications due to its
 * generalization capability. However, using dropout may not prevent overfitting due to variation
 * and limited size of the dataset, and it is reported that dropout layer increases training time by
 * 2-3 times since different simulated multiple networks are trained for each iteration, thus
 * resulting in noisy parameter updates.
 */
public class Dropout extends ParameterBlock {

    private static final byte VERSION = 2;

    private float probability;
    private int[] sharedAxes;

    Dropout(Builder builder) {
        probability = builder.probability;
        sharedAxes = builder.sharedAxes;
    }

    /** {@inheritDoc} */
    @Override
    public NDList forward(
            ParameterStore parameterStore, NDList inputs, PairList<String, Object> params) {
        NDArrayEx ex = inputs.singletonOrThrow().getNDArrayInternal();
        return ex.dropout(inputs, probability, sharedAxes, params);
    }

    /** {@inheritDoc} */
    @Override
    public Shape[] getOutputShapes(NDManager manager, Shape[] inputShapes) {
        return new Shape[] {inputShapes[0]};
    }

    /** {@inheritDoc} */
    @Override
    public List<Parameter> getDirectParameters() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public Shape getParameterShape(String name, Shape[] inputShapes) {
        throw new IllegalArgumentException("Dropout has no parameters");
    }

    /** {@inheritDoc} */
    @Override
    public void saveParameters(DataOutputStream os) throws IOException {
        os.writeByte(VERSION);
        saveInputShapes(os);
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
    }

    /**
     * Creates a builder to build a {@link Dropout}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The Builder to construct a {@link Dropout} type of {@link ai.djl.nn.Block}. */
    public static final class Builder {

        private float probability = 0.5f;
        private int[] sharedAxes = {};

        Builder() {}

        /**
         * Sets the probability or the fraction of the input that gets dropped out during training
         * time. Defaults to 0.5.
         *
         * @param probability fraction of the input that gets dropped out during training
         * @return this Builder
         */
        public Builder optProbability(float probability) {
            this.probability = probability;
            return this;
        }

        /**
         * Sets the axes for variational dropout kernel.
         *
         * @param sharedAxes the axes for variational dropout kernel
         * @return this Builder
         */
        public Builder optSharedAxes(int[] sharedAxes) {
            this.sharedAxes = sharedAxes;
            return this;
        }

        /**
         * Builds a {@link Dropout} block.
         *
         * @return the {@link Dropout} block
         */
        public Dropout build() {
            return new Dropout(this);
        }
    }
}
