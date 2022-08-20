/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checking how the JavaExecGenerator checks the need to set a main class as environment variable JAVA_MAIN_CLASS
 * in various situations
 *
 * @author Oliver Weise
 */

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
class JavaExecGeneratorMainClassDeterminationTest {

    @Mocked
    private KitLogger log;
    @Mocked
    private JavaProject project;
    @Mocked
    private FatJarDetector fatJarDetector;
    @Mocked
    private FatJarDetector.Result fatJarDetectorResult;
    @Mocked
    private MainClassDetector mainClassDetector;
    private ProcessorConfig processorConfig;

    @BeforeEach
    void setUp() {
        processorConfig = new ProcessorConfig();
        // @formatter:off
        new Expectations() {{
            project.getVersion(); result = "1.33.7-SNAPSHOT";
            project.getOutputDirectory(); result = "/the/output/directory";
        }};
        // @formatter:on
    }

    /**
     * The main class is determined via config in a non-fat-jar deployment
     *
     */
    @Test
    void testMainClassDeterminationFromConfig() {
        // Given
        final Map<String, Object> configurations = new HashMap<>();
        configurations.put("mainClass", "the.main.ClassName");
        configurations.put("name", "TheImageName");
        processorConfig.getConfig().put("java-exec", configurations);
        final GeneratorContext generatorContext = GeneratorContext.builder()
                .project(project)
                .config(processorConfig)
                .strategy(JKubeBuildStrategy.docker)
                .logger(log)
                .build();

        JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

        List<ImageConfiguration> customized = generator.customize(new ArrayList<>(), false);

        assertThat(customized).singleElement()
                        .hasFieldOrPropertyWithValue("name", "TheImageName")
                        .extracting(ImageConfiguration::getBuildConfiguration)
                        .extracting(BuildConfiguration::getEnv)
                        .asInstanceOf(InstanceOfAssertFactories.MAP)
                        .as("Main Class set as environment variable")
                        .containsEntry("JAVA_MAIN_CLASS", "the.main.ClassName");
    }

    /**
     * The main class is determined via main class detection in a non-fat-jar deployment
     *
     */
    @Test
    void testMainClassDeterminationFromDetectionOnNonFatJar(@Injectable File baseDir) {
        processorConfig.getConfig().put("java-exec", Collections.singletonMap("name", "TheNonFatJarImageName"));
        new Expectations() {{
            project.getBaseDirectory();
            result = baseDir;
            fatJarDetector.scan();
            result = null;
            mainClassDetector.getMainClass();
            result = "the.detected.MainClass";
        }};

        final GeneratorContext generatorContext = GeneratorContext.builder()
                .project(project)
                .config(processorConfig)
                .strategy(JKubeBuildStrategy.docker)
                .logger(log)
                .build();

        JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

        List<ImageConfiguration> customized = generator.customize(new ArrayList<>(), false);

        assertThat(customized).singleElement()
                .hasFieldOrPropertyWithValue("name", "TheNonFatJarImageName")
                .extracting(ImageConfiguration::getBuildConfiguration)
                .extracting(BuildConfiguration::getEnv)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .as("Main Class set as environment variable")
                .containsEntry("JAVA_MAIN_CLASS", "the.detected.MainClass");
    }

    /**
     * The main class is determined as the Main-Class of a fat jar
     *
     */
    @Test
    void testMainClassDeterminationFromFatJar(
            @Mocked FileUtil fileUtil, @Injectable File baseDir, @Injectable File fatJarArchive) {
        processorConfig.getConfig().put("java-exec", Collections.singletonMap("name", "TheFatJarImageName"));
        new Expectations() {{
            project.getBaseDirectory();
            result = baseDir;
            fileUtil.getRelativePath(withInstanceOf(File.class), withInstanceOf(File.class));
            result = baseDir;
            fatJarDetector.scan();
            result = fatJarDetectorResult;
            fatJarDetectorResult.getArchiveFile();
            result = fatJarArchive;
        }};
        final GeneratorContext generatorContext = GeneratorContext.builder()
                .project(project)
                .config(processorConfig)
                .strategy(JKubeBuildStrategy.docker)
                .logger(log)
                .build();

        JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

        List<ImageConfiguration> customized = generator.customize(new ArrayList<>(), false);

        assertThat(customized).singleElement()
                .hasFieldOrPropertyWithValue("name", "TheFatJarImageName")
                .extracting(ImageConfiguration::getBuildConfiguration)
                .extracting(BuildConfiguration::getEnv)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .as("Main Class is NOT set as environment variable#")
                .doesNotContainEntry("JAVA_MAIN_CLASS", null);
    }

}
