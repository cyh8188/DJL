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
package ai.djl.examples.inference;

import ai.djl.ModelException;
import ai.djl.examples.inference.cv.ActionRecognition;
import ai.djl.modality.Classifications;
import ai.djl.testing.TestRequirements;
import ai.djl.translate.TranslateException;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class ActionRecognitionTest {

    @Test
    public void testActionRecognition() throws ModelException, TranslateException, IOException {
        TestRequirements.engine("MXNet");

        Classifications result = ActionRecognition.predict();
        Classifications.Classification best = result.best();
        Assert.assertEquals(best.getClassName(), "ThrowDiscus");
        Assert.assertTrue(Double.compare(best.getProbability(), 0.9) > 0);
    }
}
