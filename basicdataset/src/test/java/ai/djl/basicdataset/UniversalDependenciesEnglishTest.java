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
package ai.djl.basicdataset;

import ai.djl.basicdataset.nlp.UniversalDependenciesEnglish;
import ai.djl.basicdataset.utils.TextData;
import ai.djl.basicdataset.utils.TextData.Configuration;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.Repository;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.dataset.Record;
import ai.djl.translate.TranslateException;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UniversalDependenciesEnglishTest {

    private static final int EMBEDDING_SIZE = 15;

    @Test
    public void testGetDataWithPreTrainedEmbedding() throws IOException, TranslateException {
        Repository repository = Repository.newInstance("test", "src/test/resources/mlrepo");
        try (NDManager manager = NDManager.newBaseManager()) {
            UniversalDependenciesEnglish universalDependenciesEnglish =
                    UniversalDependenciesEnglish.builder()
                            .optRepository(repository)
                            .optUsage(Dataset.Usage.TRAIN)
                            .setSourceConfiguration(
                                    new Configuration()
                                            .setTextEmbedding(
                                                    TestUtils.getTextEmbedding(
                                                            manager, EMBEDDING_SIZE)))
                            .setTargetConfiguration(
                                    new Configuration()
                                            .setTextEmbedding(
                                                    TestUtils.getTextEmbedding(
                                                            manager, EMBEDDING_SIZE)))
                            .setSampling(32, true)
                            .optLimit(10)
                            .build();

            universalDependenciesEnglish.prepare();
            Record record = universalDependenciesEnglish.get(manager, 0);
            Assert.assertEquals(record.getData().get(0).getShape().dimension(), 2);
            Assert.assertEquals(record.getLabels().get(0).getShape().dimension(), 1);
        }
    }

    @Test
    public void testGetDataWithTrainableEmbedding() throws IOException, TranslateException {
        Repository repository = Repository.newInstance("test", "src/test/resources/mlrepo");
        try (NDManager manager = NDManager.newBaseManager()) {
            UniversalDependenciesEnglish universalDependenciesEnglish =
                    UniversalDependenciesEnglish.builder()
                            .optRepository(repository)
                            .optUsage(Dataset.Usage.TEST)
                            .setSourceConfiguration(
                                    new Configuration().setEmbeddingSize(EMBEDDING_SIZE))
                            .setTargetConfiguration(
                                    new Configuration().setEmbeddingSize(EMBEDDING_SIZE))
                            .setSampling(32, true)
                            .optLimit(10)
                            .build();

            universalDependenciesEnglish.prepare();
            Record record = universalDependenciesEnglish.get(manager, 0);
            Assert.assertEquals(record.getData().size(), 1);
            Assert.assertEquals(record.getData().get(0).getShape().dimension(), 1);
            Assert.assertEquals(record.getLabels().size(), 1);
            Assert.assertEquals(record.getLabels().get(0).getShape().dimension(), 1);
        }
    }

    @Test
    public void testMisc() throws TranslateException, IOException {
        Repository repository = Repository.newInstance("test", "src/test/resources/mlrepo");
        try (NDManager manager = NDManager.newBaseManager()) {
            UniversalDependenciesEnglish universalDependenciesEnglish =
                    UniversalDependenciesEnglish.builder()
                            .optRepository(repository)
                            .optUsage(Dataset.Usage.VALIDATION)
                            .setSourceConfiguration(
                                    new TextData.Configuration()
                                            .setTextEmbedding(
                                                    TestUtils.getTextEmbedding(
                                                            manager, EMBEDDING_SIZE)))
                            .setTargetConfiguration(
                                    new TextData.Configuration()
                                            .setTextEmbedding(
                                                    TestUtils.getTextEmbedding(
                                                            manager, EMBEDDING_SIZE)))
                            .setSampling(32, true)
                            .optLimit(350)
                            .build();

            universalDependenciesEnglish.prepare();
            universalDependenciesEnglish.prepare();
            Assert.assertEquals(universalDependenciesEnglish.size(), 350);
        }
    }
}
