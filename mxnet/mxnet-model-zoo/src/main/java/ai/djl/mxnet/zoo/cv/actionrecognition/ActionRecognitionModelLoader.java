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
package ai.djl.mxnet.zoo.cv.actionrecognition;

import ai.djl.Application;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.ImageClassificationTranslator;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
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

/**
 * Model loader for Action Recognition models.
 *
 * <p>The model was trained on Gluon and loaded in DJL in MXNet Symbol Block. See <a
 * href="https://arxiv.org/pdf/1608.00859.pdf">Reference paper</a>.
 *
 * @see ai.djl.mxnet.engine.MxSymbolBlock
 */
public class ActionRecognitionModelLoader extends BaseModelLoader {

    private static final Application APPLICATION = Application.CV.ACTION_RECOGNITION;
    private static final String GROUP_ID = MxModelZoo.GROUP_ID;
    private static final String ARTIFACT_ID = "action_recognition";
    private static final String VERSION = "0.0.1";

    /**
     * Creates the Model loader from the given repository.
     *
     * @param repository the repository to load the model from
     */
    public ActionRecognitionModelLoader(Repository repository) {
        super(repository, MRL.model(APPLICATION, GROUP_ID, ARTIFACT_ID), VERSION);
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
            // 299 is the minimum length for inception, 224 for vgg
            int width = ((Double) arguments.getOrDefault("width", 299d)).intValue();
            int height = ((Double) arguments.getOrDefault("height", 299d)).intValue();

            Pipeline pipeline = new Pipeline();
            pipeline.add(new Resize(width, height))
                    .add(new ToTensor())
                    .add(
                            new Normalize(
                                    new float[] {0.485f, 0.456f, 0.406f},
                                    new float[] {0.229f, 0.224f, 0.225f}));

            return ImageClassificationTranslator.builder()
                    .setPipeline(pipeline)
                    .setSynsetArtifactName("classes.txt")
                    .build();
        }
    }
}
