#!/usr/bin/env python
#
# Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
# except in compliance with the License. A copy of the License is located at
#
# http://aws.amazon.com/apache2.0/
#
# or in the "LICENSE.txt" file accompanying this file. This file is distributed on an "AS IS"
# BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for
# the specific language governing permissions and limitations under the License.

from pyspark import SparkContext
from pyspark.sql import DataFrame
from typing import Optional


class TextEmbedder:

    def __init__(self,
                 input_col: str,
                 output_col: str,
                 model_url: str,
                 engine: Optional[str] = None,
                 batch_size: Optional[int] = None,
                 translator_factory=None,
                 batchifier: Optional[str] = None):
        """
        Initializes the TextEmbedder.

        :param input_col: The input column
        :param output_col: The output column
        :param model_url: The model URL
        :param engine (optional): The engine
        :param batch_size (optional): The batch size
        :param translator_factory (optional): The translator factory.
                                              Default is TextEmbeddingTranslatorFactory.
        :param batchifier (optional): The batchifier. Valid values include "none" (default),
                                      "stack", and "padding".
        """
        self.input_col = input_col
        self.output_col = output_col
        self.model_url = model_url
        self.engine = engine
        self.batch_size = batch_size
        self.translator_factory = translator_factory
        self.batchifier = batchifier

    def embed(self, dataset):
        """
        Performs text embedding on the provided dataset.

        :param dataset: input dataset
        :return: output dataset
        """
        sc = SparkContext._active_spark_context
        embedder = sc._jvm.ai.djl.spark.task.text.TextEmbedder() \
            .setInputCol(self.input_col) \
            .setOutputCol(self.output_col) \
            .setModelUrl(self.model_url)
        if self.engine is not None:
            embedder = embedder.setEngine(self.engine)
        if self.batch_size is not None:
            embedder = embedder.setBatchSize(self.batch_size)
        if self.translator_factory is not None:
            embedder = embedder.setTranslatorFactory(self.translator_factory)
        if self.batchifier is not None:
            embedder = embedder.setBatchifier(self.batchifier)
        return DataFrame(embedder.embed(dataset._jdf), dataset.sparkSession)
