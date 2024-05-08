package ai.djl

import div
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import os
import text
import java.net.URL
import java.util.*

fun checkClang(clang: File) {
    val url = "https://djl-ai.s3.amazonaws.com/build-tools/osx/clang-format"
    if (!clang.exists()) {
        // create the folder and download the executable
        clang.parentFile.mkdirs()
        // TODO original method was appending
        //clang.append(new URL(url).openStream())
        clang.writeBytes(URL(url).openStream().use { it.readAllBytes() })
        clang.setExecutable(true)
    }
}

fun formatCpp(f: File, clang: File): String {
    if (!f.name.endsWith(".cc") && !f.name.endsWith(".cpp") && !f.name.endsWith(".h"))
        return ""
    return ProcessBuilder(clang.absolutePath,
                          "-style={BasedOnStyle: Google, IndentWidth: 2, ColumnLimit: 120, AlignAfterOpenBracket: DontAlign, SpaceAfterCStyleCast: true}",
                          f.absolutePath)
            .start().inputStream.use { it.readAllBytes().decodeToString() }
}

interface FormatterConfig {
    val exclusions: ListProperty<String>
}
project.extensions.create<FormatterConfig>("formatCpp")

project.tasks {
    register("formatCpp") {
        doLast {
            if ("mac" !in os)
                return@doLast
            val rootProject = project.rootProject
            val clang = rootProject.projectDir / ".clang/clang-format"
            checkClang(clang)
            val files = project.fileTree("src")
            files.include("**/*.cc", "**/*.cpp", "**/*.h")
            val formatCpp = project.extensions.getByName<FormatterConfig>("formatCpp")
            if (formatCpp.exclusions.isPresent)
                for (exclusion in formatCpp.exclusions.get())
                    files.exclude(exclusion)

            for (f in files) {
                if (!f.isFile())
                    continue
                project.logger.info("formatting cpp file: $f")
                f.text = formatCpp(f, clang)
            }
        }
    }

    register("verifyCpp") {
        doLast {
            if ("mac" !in os)
                return@doLast
            val rootProject = project.rootProject
            val clang = rootProject.projectDir / ".clang/clang-format"
            checkClang(clang)
            val files = project.fileTree("src")
            files.include("**/*.cc", "**/*.cpp", "**/*.h")
            for (f in files) {
                if (!f.isFile())
                    continue
                project.logger.info("checking cpp file: $f")
                if (f.text != formatCpp(f, clang))
                    throw GradleException("File not formatted: " + f.absolutePath)
            }
        }
    }
}