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
package ai.djl.mxnet.zoo.cv.classification;

import ai.djl.Application;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.ImageClassificationTranslator;
import ai.djl.modality.cv.transform.CenterCrop;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.mxnet.zoo.MxModelZoo;
import ai.djl.repository.MRL;
import ai.djl.repository.Repository;
import ai.djl.repository.zoo.BaseModelLoader;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorFactory;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Model loader for Image Classification models. */
public abstract class ImageClassificationModelLoader extends BaseModelLoader {

    private static final Application APPLICATION = Application.CV.IMAGE_CLASSIFICATION;
    private static final String GROUP_ID = MxModelZoo.GROUP_ID;

    /**
     * Creates the Model loader from the given repository.
     *
     * @param repository the repository to load the model from
     * @param artifactId the artifact id of the model
     * @param version the version number of the model
     */
    public ImageClassificationModelLoader(
            Repository repository, String artifactId, String version) {
        super(repository, MRL.model(APPLICATION, GROUP_ID, artifactId), version);
        Map<Type, TranslatorFactory<?, ?>> map = new ConcurrentHashMap<>();
        map.put(Classifications.class, new FactoryImpl());
        factories.put(BufferedImage.class, map);
    }

    /** {@inheritDoc} */
    @Override
    public Application getApplication() {
        return APPLICATION;
    }

    private static final class FactoryImpl
            implements TranslatorFactory<BufferedImage, Classifications> {

        @Override
        public Translator<BufferedImage, Classifications> newInstance(
                Map<String, Object> arguments) {
            int width = ((Double) arguments.getOrDefault("width", 224d)).intValue();
            int height = ((Double) arguments.getOrDefault("height", 224d)).intValue();
            String flag = (String) arguments.getOrDefault("flag", NDImageUtils.Flag.COLOR.name());

            Pipeline pipeline = new Pipeline();
            pipeline.add(new CenterCrop()).add(new Resize(width, height)).add(new ToTensor());

            return ImageClassificationTranslator.builder()
                    .optFlag(NDImageUtils.Flag.valueOf(flag))
                    .setPipeline(pipeline)
                    .setSynsetArtifactName("synset.txt")
                    .build();
        }
    }
}
