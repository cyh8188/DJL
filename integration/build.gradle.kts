@file:Suppress("UNCHECKED_CAST")

plugins {
    ai.djl.javaProject
    application
    jacoco
}

dependencies {
    implementation(libs.commons.cli)
    implementation(libs.apache.log4j.slf4j)
    implementation(projects.basicdataset)
    implementation(projects.modelZoo)
    implementation(projects.testing)

    runtimeOnly(projects.engines.mxnet.mxnetModelZoo)
    runtimeOnly(projects.engines.pytorch.pytorchModelZoo)
    runtimeOnly(projects.engines.pytorch.pytorchJni)
    runtimeOnly(projects.engines.tensorflow.tensorflowModelZoo)
    runtimeOnly(projects.engines.ml.xgboost)
    runtimeOnly(projects.engines.ml.lightgbm)
    runtimeOnly(projects.engines.onnxruntime.onnxruntimeEngine)
    runtimeOnly(projects.extension.tokenizers)
}

tasks {
    compileJava {
        // need to use `project` as receiver otherwise something else will be picked up
        javaCompiler = project.javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(11) }
        // TODO, cant remove from `options.compilerArgs`
        //        println(options.compilerArgs)
        //        println(options.compilerArgs::class.java)
        //        val args = options.compilerArgs as MutableList<String>
        //        args.removeAll(listOf("--release", "8"))
        //        args.clear()
    }

    register<Copy>("copyDependencies") {
        into("build/dependencies")
        from(configurations.runtimeClasspath)
    }

    run.configure {
        environment("TF_CPP_MIN_LOG_LEVEL" to "1") // turn off TensorFlow print out
        // @Niels Doucet
        // Just a heads-up: gradle support warned me about systemProperties System.getProperties(). It's really
        // dangerous to just copy over all system properties to a task invocation. You should really be specific about
        // the properties you'd like to expose inside the task, or you might get very strange issues.
        systemProperties = System.getProperties().toMap() as Map<String, Any>
        systemProperties.remove("user.dir")
        systemProperty("file.encoding", "UTF-8")
        jvmArgs("-Xverify:none")
    }

    register<JavaExec>("debugEnv") {
        classpath = sourceSets.main.get().runtimeClasspath
        systemProperties = System.getProperties().toMap() as Map<String, Any>
        systemProperties.remove("user.dir")
        systemProperties["ai.djl.logging.level"] = "debug"
        mainClass = "ai.djl.integration.util.DebugEnvironment"
    }

    distTar { enabled = false }
}

application {
    mainClass = System.getProperty("main", "ai.djl.integration.IntegrationTest")
}
