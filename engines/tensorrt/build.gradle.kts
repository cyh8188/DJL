plugins {
    ai.djl.javaProject
    ai.djl.cppFormatter
    ai.djl.publish
}

group = "ai.djl.tensorrt"

dependencies {
    api(project(":api"))

    testImplementation(libs.testng) {
        exclude("junit", "junit")
    }
    testImplementation(libs.slf4j.simple)
    testRuntimeOnly(project(":engines:pytorch:pytorch-model-zoo"))
    testRuntimeOnly(project(":engines:pytorch:pytorch-engine"))
}

tasks {
    compileJava { dependsOn(processResources) }

    processResources {
        outputs.dir(buildDirectory / "classes/java/main/native/lib")
        doLast {
            val trtVersion = libs.versions.tensorrt.get()
            val djlVersion = libs.versions.djl.get()
            val url = "https://publish.djl.ai/tensorrt/${trtVersion}/jnilib/${djlVersion}"
            val files = listOf("linux-x86_64/libdjl_trt.so")
            val jnilibDir = project.projectDir / "jnilib/${djlVersion}"
            for (entry in files) {
                val file = jnilibDir / entry
                if (file.exists()) {
                    project.logger.lifecycle("prebuilt or cached file found for $entry")
                } else if (!project.hasProperty("jni")) {
                    project.logger.lifecycle("Downloading $url/$entry")
                    file.parentFile.mkdirs()
                    "$url/$entry".url into file
                }
            }

            copy {
                from(jnilibDir)
                into(buildDirectory / "classes/java/main/native/lib")
            }

            // write properties
            val propFile = buildDirectory / "classes/java/main/native/lib/tensorrt.properties"
            propFile.text = "version=${trtVersion}-${version}\n"
        }
    }

    register("compileJNI") {
        doFirst {
            if ("linux" in os) {
                exec {
                    commandLine("bash", "build.sh")
                }
            } else {
                throw IllegalStateException("Unknown Architecture $osName")
            }

            // for nightly ci
            val classifier = "${os}-x86_64"
            val ciDir = project.projectDir / "jnilib/${libs.versions.djl.get()}/${classifier}"
            copy {
                from(fileTree(buildDirectory) {
                    include("libdjl_trt.*")
                })
                into(ciDir)
            }
            delete("$home/.djl.ai/tensorrt")
        }
    }

    clean {
        doFirst {
            delete("$home/.djl.ai/tensorrt")
        }
    }
}
