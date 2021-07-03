/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package ai.djl.mxnet.zoo.nlp.qa;

import ai.djl.Model;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorFactory;
import ai.djl.util.Pair;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/** A {@link TranslatorFactory} that creates a {@link MxBertQATranslator} instance. */
public class MxBertQATranslatorFactory implements TranslatorFactory {

    /** {@inheritDoc} */
    @Override
    public Set<Pair<Type, Type>> getSupportedTypes() {
        return Collections.singleton(new Pair<>(QAInput.class, String.class));
    }

    /** {@inheritDoc} */
    @Override
    public Translator<?, ?> newInstance(
            Class<?> input, Class<?> output, Model model, Map<String, ?> arguments)
            throws TranslateException {
        if (!isSupported(input, output)) {
            throw new IllegalArgumentException("Unsupported input/output types.");
        }
        int seqLength;
        Object value = arguments.get("seqLength");
        if (value == null) {
            seqLength = 384;
        } else {
            seqLength = Integer.parseInt(value.toString());
        }

        return MxBertQATranslator.builder().setSeqLength(seqLength).build();
    }
}
