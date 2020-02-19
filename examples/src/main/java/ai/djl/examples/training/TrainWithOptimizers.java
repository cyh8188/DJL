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
package ai.djl.examples.training;

import ai.djl.Application;
import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.basicdataset.Cifar10;
import ai.djl.basicmodelzoo.BasicModelZoo;
import ai.djl.basicmodelzoo.cv.classification.ResNetV1;
import ai.djl.examples.training.util.Arguments;
import ai.djl.examples.training.util.ExampleTrainingResult;
import ai.djl.examples.training.util.TrainingUtils;
import ai.djl.metric.Metrics;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.mxnet.engine.MxEngine;
import ai.djl.mxnet.zoo.MxModelZoo;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.SymbolBlock;
import ai.djl.nn.core.Linear;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.optimizer.learningrate.LearningRateTracker;
import ai.djl.training.optimizer.learningrate.MultiFactorTracker;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Pipeline;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** This example features sample usage of a variety of optimizers to train Cifar10. */
public final class TrainWithOptimizers {

    private TrainWithOptimizers() {}

    public static void main(String[] args)
            throws IOException, ParseException, ModelNotFoundException, MalformedModelException {
        TrainWithOptimizers.runExample(args);
    }

    public static ExampleTrainingResult runExample(String[] args)
            throws IOException, ParseException, ModelNotFoundException, MalformedModelException {
        Options options = OptimizerArguments.getOptions();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args, null, false);
        OptimizerArguments arguments = new OptimizerArguments(cmd);

        try (Model model = getModel(arguments)) {
            // get training dataset
            RandomAccessDataset trainDataset = getDataset(Dataset.Usage.TRAIN, arguments);
            RandomAccessDataset validationDataset = getDataset(Dataset.Usage.TEST, arguments);

            // setup training configuration
            DefaultTrainingConfig config = setupTrainingConfig(arguments);
            config.addTrainingListeners(
                    TrainingListener.Defaults.logging(
                            TrainWithOptimizers.class.getSimpleName(),
                            arguments.getBatchSize(),
                            (int) trainDataset.getNumIterations(),
                            (int) validationDataset.getNumIterations(),
                            arguments.getOutputDir()));

            ExampleTrainingResult result;
            try (Trainer trainer = model.newTrainer(config)) {
                trainer.setMetrics(new Metrics());

                /*
                 * CIFAR10 is 32x32 image and pre processed into NCHW NDArray.
                 * 1st axis is batch axis, we can use 1 for initialization.
                 */
                Shape inputShape = new Shape(1, 3, Cifar10.IMAGE_HEIGHT, Cifar10.IMAGE_WIDTH);

                // initialize trainer with proper input shape
                trainer.initialize(inputShape);
                TrainingUtils.fit(
                        trainer,
                        arguments.getEpoch(),
                        trainDataset,
                        validationDataset,
                        arguments.getOutputDir(),
                        "resnetv1");

                result = new ExampleTrainingResult(trainer);
            }
            model.save(Paths.get("build/model"), "resnetv1");
            return result;
        }
    }

    private static Model getModel(Arguments arguments)
            throws IOException, ModelNotFoundException, MalformedModelException {
        boolean isSymbolic = arguments.isSymbolic();
        boolean preTrained = arguments.isPreTrained();
        Map<String, String> options = arguments.getCriteria();
        Criteria.Builder<BufferedImage, Classifications> builder =
                Criteria.builder()
                        .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                        .setTypes(BufferedImage.class, Classifications.class)
                        .optProgress(new ProgressBar())
                        .optModelLoaderName("resnet");
        if (isSymbolic) {
            // currently only MxEngine support removeLastBlock
            builder.optEngine(MxEngine.ENGINE_NAME).optModelZooName(MxModelZoo.NAME);
            if (options == null) {
                builder.optOption("layers", "50");
                builder.optOption("flavor", "v1");
            } else {
                builder.optOptions(options);
            }

            Model model = ModelZoo.loadModel(builder.build());
            SequentialBlock newBlock = new SequentialBlock();
            SymbolBlock block = (SymbolBlock) model.getBlock();
            block.removeLastBlock();
            newBlock.add(block);
            newBlock.add(x -> new NDList(x.singletonOrThrow().squeeze()));
            newBlock.add(Linear.builder().setOutChannels(10).build());
            newBlock.add(Blocks.batchFlattenBlock());
            model.setBlock(newBlock);
            if (!preTrained) {
                model.getBlock().clear();
            }
            return model;
        }
        // imperative resnet50
        if (preTrained) {
            builder.optModelZooName(BasicModelZoo.NAME);
            if (options == null) {
                builder.optOption("layers", "50");
                builder.optOption("flavor", "v1");
                builder.optOption("dataset", "cifar10");
            } else {
                builder.optOptions(options);
            }
            // load pre-trained imperative ResNet50 from DJL model zoo
            return ModelZoo.loadModel(builder.build());
        } else {
            // construct new ResNet50 without pre-trained weights
            Model model = Model.newInstance();
            Block resNet50 =
                    ResNetV1.builder()
                            .setImageShape(new Shape(3, Cifar10.IMAGE_HEIGHT, Cifar10.IMAGE_WIDTH))
                            .setNumLayers(50)
                            .setOutSize(10)
                            .build();
            model.setBlock(resNet50);
            return model;
        }
    }

    private static DefaultTrainingConfig setupTrainingConfig(OptimizerArguments arguments) {
        return new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                .addEvaluator(new Accuracy())
                .optOptimizer(setupOptimizer(arguments))
                .setBatchSize(arguments.getBatchSize())
                .optDevices(Device.getDevices(arguments.getMaxGpus()));
    }

    private static Optimizer setupOptimizer(OptimizerArguments arguments) {
        String optimizerName = arguments.getOptimizer();
        int batchSize = arguments.getBatchSize();
        switch (optimizerName) {
            case "sgd":
                // epoch number to change learning rate
                int[] epochs;
                if (arguments.isPreTrained()) {
                    epochs = new int[] {2, 5, 8};
                } else {
                    epochs = new int[] {20, 60, 90, 120, 180};
                }
                int[] steps = Arrays.stream(epochs).map(k -> k * 60000 / batchSize).toArray();
                MultiFactorTracker learningRateTracker =
                        LearningRateTracker.multiFactorTracker()
                                .setSteps(steps)
                                .optBaseLearningRate(1e-3f)
                                .optFactor((float) Math.sqrt(.1f))
                                .optWarmUpBeginLearningRate(1e-4f)
                                .optWarmUpSteps(200)
                                .build();
                return Optimizer.sgd()
                        .setLearningRateTracker(learningRateTracker)
                        .optWeightDecays(0.001f)
                        .optClipGrad(5f)
                        .build();
            case "adam":
                return Optimizer.adam().build();
            default:
                throw new IllegalArgumentException("Unknown optimizer");
        }
    }

    private static RandomAccessDataset getDataset(Dataset.Usage usage, Arguments arguments)
            throws IOException {
        Pipeline pipeline =
                new Pipeline(
                        new ToTensor(),
                        new Normalize(Cifar10.NORMALIZE_MEAN, Cifar10.NORMALIZE_STD));
        Cifar10 cifar10 =
                Cifar10.builder()
                        .optUsage(usage)
                        .setSampling(arguments.getBatchSize(), true)
                        .optMaxIteration(arguments.getMaxIterations())
                        .optPipeline(pipeline)
                        .build();
        cifar10.prepare(new ProgressBar());
        return cifar10;
    }

    private static class OptimizerArguments extends Arguments {

        private String optimizer;

        public OptimizerArguments(CommandLine cmd) {
            super(cmd);

            if (cmd.hasOption("optimizer")) {
                optimizer = cmd.getOptionValue("optimizer");
            } else {
                optimizer = "adam";
            }
        }

        public static Options getOptions() {
            Options options = Arguments.getOptions();
            options.addOption(
                    Option.builder("z")
                            .longOpt("optimizer")
                            .hasArg()
                            .argName("OPTIMIZER")
                            .desc("The optimizer to use.")
                            .build());
            return options;
        }

        public String getOptimizer() {
            return optimizer;
        }
    }
}
