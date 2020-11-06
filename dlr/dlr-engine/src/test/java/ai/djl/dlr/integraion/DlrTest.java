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
package ai.djl.dlr.integraion;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DlrTest {

    @Test(enabled = false)
    public void testDlr()
            throws MalformedModelException, ModelNotFoundException, IOException,
                    TranslateException {
        ImageClassificationTranslator translator =
                ImageClassificationTranslator.builder()
                        .addTransform(new Resize(224, 224))
                        .addTransform(new ToTensor())
                        .build();
        Criteria<Image, Classifications> criteria =
                Criteria.builder()
                        .setTypes(Image.class, Classifications.class)
                        .optTranslator(translator)
                        // TODO use the model from S3
                        .optModelUrls("file:///Users/leecheng/workspace/python/tvm_model_old")
                        .optEngine("DLR") // use DLR engine
                        .build();
        Image image =
                ImageFactory.getInstance()
                        .fromUrl(
                                "https://raw.githubusercontent.com/dmlc/mxnet.js/main/data/cat.png");
        try (ZooModel<Image, Classifications> model = ModelZoo.loadModel(criteria);
                Predictor<Image, Classifications> predictor = model.newPredictor()) {
            Classifications classifications = predictor.predict(image);
            Assert.assertEquals("n02123159 tiger cat", classifications.best().getClassName());
        }
    }
}
