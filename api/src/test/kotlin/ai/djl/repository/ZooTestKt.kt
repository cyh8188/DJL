/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package ai.djl.repository

import ai.djl.MalformedModelException
import ai.djl.modality.Input
import ai.djl.modality.Output
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelNotFoundException
import ai.djl.repository.zoo.ModelZoo
import criteria
import invoke
import org.testng.Assert
import org.testng.annotations.Test
import java.io.IOException

class ZooTestKt {
    @Test
    fun testCriteriaToBuilder() {
        val criteria1 = criteria<Input, Output> {
            engine = "testEngine1"
            modelName = "testModelName"
        }

        val criteria2 = criteria1 { engine = "testEngine2" }

        Assert.assertEquals("testEngine1", criteria1.engine)
        Assert.assertEquals("testEngine2", criteria2.engine)
        Assert.assertEquals("testModelName", criteria1.modelName)
        Assert.assertEquals("testModelName", criteria2.modelName)
    }

    @Test(expectedExceptions = [IllegalArgumentException::class])
    @Throws(ModelNotFoundException::class, MalformedModelException::class, IOException::class)
    fun testInvalidCriteria() {
        val criteria = Criteria.builder().build()
        criteria.loadModel()
    }

    @Test fun testModelZooResolver() {
        ModelZoo.setModelZooResolver { null }
        val zoo = ModelZoo.getModelZoo("unknown")
        Assert.assertNull(zoo)
    }
}
