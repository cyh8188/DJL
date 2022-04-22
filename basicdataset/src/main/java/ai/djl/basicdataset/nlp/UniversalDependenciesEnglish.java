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
package ai.djl.basicdataset.nlp;

import ai.djl.Application.NLP;
import ai.djl.modality.nlp.embedding.EmbeddingException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.repository.Artifact;
import ai.djl.repository.MRL;
import ai.djl.training.dataset.Record;
import ai.djl.util.Progress;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A Gold Standard Universal Dependencies Corpus for English, built over the source material of the
 * English Web Treebank LDC2012T13.
 * @see <a href="https://catalog.ldc.upenn.edu/LDC2012T13">English Web Treebank LDC2012T13</a>
 */
public class UniversalDependenciesEnglish extends TextDataset {

    private static final String VERSION = "2.0";
    private static final String ARTIFACT_ID = "universal-dependencies-en";

    private List<List<Integer>> universalPosTags;

    /**
     * Creates a new instance of {@code UniversalDependenciesEnglish}.
     *
     * @param builder the builder object to build from
     */
    protected UniversalDependenciesEnglish(Builder builder) {
        super(builder);
        this.usage = builder.usage;
        mrl = builder.getMrl();
    }

    /**
     * Creates a new builder to build a {@link UniversalDependenciesEnglish}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Prepares the dataset for use with tracked progress. In this method the TXT file will be
     * parsed. The sentences will be added to {@code sourceTextData} and the Universal POS tags
     * will be added to {@code universalPosTags}. Only {@code sourceTextData} will then be preprocessed.
     *
     * @param progress the progress tracker
     * @throws IOException for various exceptions depending on the dataset
     * @throws EmbeddingException if there are exceptions during the embedding process
     */
    @Override
    public void prepare(Progress progress) throws IOException, EmbeddingException {
        if (prepared) {
            return;
        }

        Artifact artifact = mrl.getDefaultArtifact();
        mrl.prepare(artifact, progress);
        Path root = mrl.getRepository().getResourceDirectory(artifact);
        Path usagePath = null;
        switch (usage) {
            case TRAIN:
                usagePath = Paths.get("en-ud-v2/en-ud-v2/en-ud-tag.v2.train.txt");
                break;
            case TEST:
                usagePath = Paths.get("en-ud-v2/en-ud-v2/en-ud-tag.v2.test.txt");
                break;
            case VALIDATION:
                usagePath = Paths.get("en-ud-v2/en-ud-v2/en-ud-tag.v2.dev.txt");
                break;
            default:
                break;
        }
        usagePath = root.resolve(usagePath);

        List<String> sourceTextData = new ArrayList<>();
        universalPosTags = new ArrayList<>();
        StringBuilder sentence = new StringBuilder();
        List<Integer> universalPosTag = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(usagePath)) {
            String row;
            while ((row = reader.readLine()) != null) {
                if (("").equals(row)) {
                    sourceTextData.add(sentence.toString());
                    universalPosTags.add(universalPosTag);

                    sentence.delete(0, sentence.length());
                    universalPosTag.clear();

                    continue;
                }
                String[] splits = row.split("\t");
                sentence.append(splits[0]);
                universalPosTag.add(UniversalPosTag.valueOf(splits[1]).ordinal());
            }
        }

        preprocess(sourceTextData, true);
        prepared = true;
    }

    /**
     * Gets the {@link Record} for the given index from the dataset.
     *
     * @param manager the manager used to create the arrays
     * @param index the index of the requested data item
     * @return a {@link Record} that contains the data and label of the requested data item. The
     *     data {@link NDList} contains one {@link NDArray} representing the sentence embedding,
     *     The label {@link NDList} contains one {@link NDArray} including the indices of the
     *     Universal POS tags of each token in the corresponding sentence. For the index of each
     *     Universal POS tag, see the enum class {@link UniversalPosTag}.
     */
    @Override
    public Record get(NDManager manager, long index) {
        NDList data = new NDList();
        data.add(sourceTextData.getEmbedding(manager, index));
        NDList labels = new NDList();
        labels.add(
                manager.create(
                                universalPosTags.get(Math.toIntExact(index))
                                        .stream()
                                        .mapToInt(Integer::intValue)
                                        .toArray())
                        .toType(DataType.INT32, false));
        return new Record(data, labels);
    }

    /**
     * Returns the number of records available to be read in this {@code Dataset}. In this
     * implementation, the actual size of available records are the size of {@code
     * sourceTextData}.
     *
     * @return the number of records available to be read in this {@code Dataset}
     */
    @Override
    protected long availableSize() {
        return sourceTextData.getSize();
    }

    /** A builder for a {@link UniversalDependenciesEnglish}. */
    public static class Builder extends TextDataset.Builder<Builder> {

        /** Constructs a new builder. */
        public Builder() {
            artifactId = ARTIFACT_ID;
        }

        /** {@inheritDoc} */
        @Override
        public Builder self() {
            return this;
        }

        /**
         * Builds the {@link UniversalDependenciesEnglish}.
         *
         * @return the {@link UniversalDependenciesEnglish}
         */
        public UniversalDependenciesEnglish build() {
            return new UniversalDependenciesEnglish(this);
        }

        MRL getMrl() {
            return repository.dataset(NLP.ANY, groupId, artifactId, VERSION);
        }
    }
}
