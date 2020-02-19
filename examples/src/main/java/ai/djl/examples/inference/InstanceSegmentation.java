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

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.DetectedObjects;
import ai.djl.modality.cv.ImageVisualization;
import ai.djl.modality.cv.util.BufferedImageUtils;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example of inference using an instance segmentation model.
 *
 * <p>See this <a
 * href="https://github.com/awslabs/djl/blob/master/examples/docs/instance_segmentation.md">doc</a>
 * for information about this example.
 */
public final class InstanceSegmentation {

    private static final Logger logger = LoggerFactory.getLogger(InstanceSegmentation.class);

    private InstanceSegmentation() {}

    public static void main(String[] args) throws IOException, ModelException, TranslateException {
        DetectedObjects detection = InstanceSegmentation.predict();
        logger.info("{}", detection);
    }

    public static DetectedObjects predict() throws IOException, ModelException, TranslateException {
        Path imageFile = Paths.get("src/test/resources/segmentation.jpg");
        BufferedImage img = BufferedImageUtils.fromFile(imageFile);

        Criteria<BufferedImage, DetectedObjects> criteria =
                Criteria.builder()
                        .optApplication(Application.CV.INSTANCE_SEGMENTATION)
                        .setTypes(BufferedImage.class, DetectedObjects.class)
                        .optOption("backbone", "resnet18")
                        .optOption("flavor", "v1b")
                        .optOption("dataset", "coco")
                        .optProgress(new ProgressBar())
                        .build();

        try (ZooModel<BufferedImage, DetectedObjects> model = ModelZoo.loadModel(criteria)) {
            try (Predictor<BufferedImage, DetectedObjects> predictor = model.newPredictor()) {
                DetectedObjects detection = predictor.predict(img);
                saveBoundingBoxImage(img, detection);
                return detection;
            }
        }
    }

    private static void saveBoundingBoxImage(BufferedImage img, DetectedObjects detection)
            throws IOException {
        Path outputDir = Paths.get("build/output");
        Files.createDirectories(outputDir);

        // Make image copy with alpha channel because original image was jpg
        BufferedImage newImage =
                new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        ImageVisualization.drawBoundingBoxes(newImage, detection);

        Path imagePath = outputDir.resolve("instances.png");
        ImageIO.write(newImage, "png", imagePath.toFile());
        logger.info("Segmentation result image has been saved in: {}", imagePath);
    }
}
