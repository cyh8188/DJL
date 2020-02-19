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
package ai.djl.repository.zoo;

import ai.djl.Application;
import ai.djl.Device;
import ai.djl.engine.Engine;
import ai.djl.translate.Translator;
import ai.djl.util.Progress;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code Criteria} class contains search criteria to look up a {@link ZooModel}.
 *
 * @param <I> the model input type
 * @param <O> the model output type
 */
public class Criteria<I, O> {

    private Application application;
    private Class<I> inputClass;
    private Class<O> outputClass;
    private String engine;
    private Device device;
    private String modelZooName;
    private String modelLoaderName;
    private Map<String, String> options;
    private Map<String, Object> arguments;
    private Translator<I, O> translator;
    private Progress progress;

    Criteria(Builder<I, O> builder) {
        this.application = builder.application;
        this.inputClass = builder.inputClass;
        this.outputClass = builder.outputClass;
        this.engine = builder.engine;
        this.device = builder.device;
        this.modelZooName = builder.modelZooName;
        this.modelLoaderName = builder.modelLoaderName;
        this.options = builder.options;
        this.arguments = builder.arguments;
        this.translator = builder.translator;
        this.progress = builder.progress;
    }

    /**
     * Returns the application of the model.
     *
     * @return the application of the model
     */
    public Application getApplication() {
        return application;
    }

    /**
     * Returns the input data type.
     *
     * @return the input data type
     */
    public Class<I> getInputClass() {
        return inputClass;
    }

    /**
     * Returns the output data type.
     *
     * @return the output data type
     */
    public Class<O> getOutputClass() {
        return outputClass;
    }

    /**
     * Returns the engine name.
     *
     * @return the engine name
     */
    public String getEngine() {
        return engine;
    }

    /**
     * Returns the {@link Device} of the model to be loaded on.
     *
     * @return the {@link Device} of the model to be loaded on
     */
    public Device getDevice() {
        return device;
    }

    /**
     * Returns the name of the {@link ModelZoo} to be searched.
     *
     * @return the name of the {@link ModelZoo} to be searched
     */
    public String getModelZooName() {
        return modelZooName;
    }

    /**
     * Returns the name of the {@link ModelLoader} to be searched.
     *
     * @return the name of the {@link ModelLoader} to be searched
     */
    public String getModelLoaderName() {
        return modelLoaderName;
    }

    /**
     * Returns the search conditions that must match the properties of the model.
     *
     * @return the search conditions that must match the properties of the model.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Returns the override configurations of the model loading arguments.
     *
     * @return the override configurations of the model loading arguments
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Returns the optional {@link Translator} to be used for {@link ZooModel}.
     *
     * @return the optional {@link Translator} to be used for {@link ZooModel}
     */
    public Translator<I, O> getTranslator() {
        return translator;
    }

    /**
     * Returns the optional {@link Progress} for the model loading.
     *
     * @return the optional {@link Progress} for the model loading
     */
    public Progress getProgress() {
        return progress;
    }

    /**
     * Creates a builder to build a {@code Criteria}.
     *
     * @return a new builder
     */
    public static Builder<?, ?> builder() {
        return new Builder<>();
    }

    /** A Builder to construct a {@code Criteria}. */
    public static final class Builder<I, O> {

        Application application;
        Class<I> inputClass;
        Class<O> outputClass;
        String engine;
        Device device;
        String modelZooName;
        String modelLoaderName;
        Map<String, String> options;
        Map<String, Object> arguments;
        Translator<I, O> translator;
        Progress progress;

        Builder() {
            engine = Engine.getInstance().getEngineName();
        }

        private Builder(Class<I> inputClass, Class<O> outputClass, Builder<?, ?> parent) {
            this.inputClass = inputClass;
            this.outputClass = outputClass;
            application = parent.application;
            engine = parent.engine;
            device = parent.device;
            modelZooName = parent.modelZooName;
            options = parent.options;
            arguments = parent.arguments;
            progress = parent.progress;
        }

        /**
         * Creates a new @{code Builder} class with the specified input and output data type.
         *
         * @param <P> the input data type
         * @param <Q> the output data type
         * @param inputClass the input class
         * @param outputClass the output class
         * @return a new @{code Builder} class with the specified input and output data type
         */
        public <P, Q> Builder<P, Q> setTypes(Class<P> inputClass, Class<Q> outputClass) {
            return new Builder<>(inputClass, outputClass, this);
        }

        /**
         * Sets the model application for this criteria.
         *
         * @param application the model application
         * @return this {@code Builder}
         */
        public Builder<I, O> optApplication(Application application) {
            this.application = application;
            return this;
        }

        /**
         * Sets the engine name for this criteria.
         *
         * @param engine the engine name
         * @return this {@code Builder}
         */
        public Builder<I, O> optEngine(String engine) {
            this.engine = engine;
            return this;
        }

        /**
         * Sets the {@link Device} for this criteria.
         *
         * @param device the {@link Device} for the criteria
         * @return this {@code Builder}
         */
        public Builder<I, O> optDevice(Device device) {
            this.device = device;
            return this;
        }

        /**
         * Sets optional model zoo name for this criteria.
         *
         * @param modelZooName the model zoo name
         * @return this {@code Builder}
         */
        public Builder<I, O> optModelZooName(String modelZooName) {
            this.modelZooName = modelZooName;
            return this;
        }

        /**
         * Sets optional model name for this criteria.
         *
         * @param modelLoaderName the model loader name
         * @return this {@code Builder}
         */
        public Builder<I, O> optModelLoaderName(String modelLoaderName) {
            this.modelLoaderName = modelLoaderName;
            return this;
        }

        /**
         * Sets the extra search conditions for this criteria.
         *
         * @param options the extra search conditions
         * @return this {@code Builder}
         */
        public Builder<I, O> optOptions(Map<String, String> options) {
            this.options = options;
            return this;
        }

        /**
         * Sets an extra search condition for this criteria.
         *
         * @param key the search key
         * @param value the search value
         * @return this {@code Builder}
         */
        public Builder<I, O> optOption(String key, String value) {
            if (options == null) {
                options = new HashMap<>();
            }
            options.put(key, value);
            return this;
        }

        /**
         * Sets an extra model loading argument for this criteria.
         *
         * @param arguments optional model loading arguments
         * @return this {@code Builder}
         */
        public Builder<I, O> optArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
            return this;
        }

        /**
         * Sets the optional model loading argument for this criteria.
         *
         * @param key the model loading argument key
         * @param value the model loading argument value
         * @return this {@code Builder}
         */
        public Builder<I, O> optArgument(String key, Object value) {
            if (arguments == null) {
                arguments = new HashMap<>();
            }
            arguments.put(key, value);
            return this;
        }

        /**
         * Sets the optional {@link Translator} to override default {@code Translator}.
         *
         * @param translator the override {@code Translator}
         * @return this {@code Builder}
         */
        public Builder<I, O> optTranslator(Translator<I, O> translator) {
            this.translator = translator;
            return this;
        }

        /**
         * Set the optional {@link Progress}.
         *
         * @param progress the {@code Progress}
         * @return this {@code Builder}
         */
        public Builder<I, O> optProgress(Progress progress) {
            this.progress = progress;
            return this;
        }

        /**
         * Builds a {@link Criteria} instance.
         *
         * @return the {@link Criteria} instance
         */
        public Criteria<I, O> build() {
            return new Criteria<>(this);
        }
    }
}
