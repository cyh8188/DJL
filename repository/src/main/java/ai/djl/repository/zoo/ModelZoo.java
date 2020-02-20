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
package ai.djl.repository.zoo;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/** An interface represents a collection of models. */
public interface ModelZoo {

    /**
     * Returns the {@code ModelZoo} with the given name.
     *
     * @param name the name of ModelZoo to retrieve
     * @return the instance of {@code ModelZoo}
     * @throws ZooProviderNotFoundException when the provider cannot be found
     * @see ZooProvider
     */
    static ModelZoo getModelZoo(String name) {
        ServiceLoader<ZooProvider> providers = ServiceLoader.load(ZooProvider.class);
        for (ZooProvider provider : providers) {
            if (provider.getName().equals(name)) {
                return provider.getModelZoo();
            }
        }
        throw new ZooProviderNotFoundException("ZooProvider not found: " + name);
    }

    /**
     * Lists the available model families in the ModelZoo.
     *
     * @return the list of all available model families
     */
    default List<ModelLoader<?, ?>> getModelLoaders() {
        List<ModelLoader<?, ?>> list = new ArrayList<>();
        try {
            Field[] fields = getClass().getDeclaredFields();
            for (Field field : fields) {
                if (ModelLoader.class.isAssignableFrom(field.getType())) {
                    list.add((ModelLoader<?, ?>) field.get(null));
                }
            }
        } catch (ReflectiveOperationException e) {
            // ignore
        }
        return list;
    }

    /**
     * Returns the {@link ModelLoader} based on the model name.
     *
     * @param name the name of the model
     * @return the {@link ModelLoader} of the model
     */
    default ModelLoader<?, ?> getModelLoader(String name) {
        for (ModelLoader<?, ?> loader : getModelLoaders()) {
            if (name.equals(loader.getName())) {
                return loader;
            }
        }
        return null;
    }

    /**
     * Returns all supported engine names.
     *
     * @return all supported engine names
     */
    Set<String> getSupportedEngines();

    /**
     * Gets the {@link ModelLoader} based on the model name.
     *
     * @param criteria the name of the model
     * @param <I> the input data type for preprocessing
     * @param <O> the output data type after postprocessing
     * @return the model that matches the criteria
     * @throws IOException for various exceptions loading data from the repository
     * @throws ModelNotFoundException if no model with the specified criteria is found
     * @throws MalformedModelException if the model data is malformed
     */
    static <I, O> ZooModel<I, O> loadModel(Criteria<I, O> criteria)
            throws IOException, ModelNotFoundException, MalformedModelException {
        String modelZooName = criteria.getModelZooName();
        ServiceLoader<ZooProvider> providers = ServiceLoader.load(ZooProvider.class);
        for (ZooProvider provider : providers) {
            if (modelZooName != null && !provider.getName().equals(modelZooName)) {
                // filter out ZooProvider by supported Engine
                continue;
            }
            ModelZoo zoo = provider.getModelZoo();
            Set<String> supportedEngine = zoo.getSupportedEngines();
            if (!supportedEngine.contains(criteria.getEngine())) {
                continue;
            }

            Application application = criteria.getApplication();
            String modelLoaderName = criteria.getModelLoaderName();
            for (ModelLoader<?, ?> loader : zoo.getModelLoaders()) {
                if (modelLoaderName != null && !modelLoaderName.equals(loader.getName())) {
                    // filter out by model loader name
                    continue;
                }
                if (application != null && !loader.getApplication().equals(application)) {
                    // filter out ModelLoader by application
                    continue;
                }

                try {
                    return loader.loadModel(criteria);
                } catch (ModelNotFoundException e) {
                    // ignore
                }
            }
        }
        throw new ModelNotFoundException("No matching model found.");
    }
}
