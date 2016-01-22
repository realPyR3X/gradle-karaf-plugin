/**
 * Copyright 2016, Luca Burgazzoli and contributors as indicated by the @author tags
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.lburgazzoli.gradle.plugin.karaf.features

import groovy.xml.MarkupBuilder
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

import com.github.lburgazzoli.gradle.plugin.karaf.AbstractKarafTask

/**
 * @author lburgazzoli
 */
class KarafFeaturesTask extends AbstractKarafTask {
    public static final String GROUP = "karaf"
    public static final String NAME = "generateFeatures"
    public static final String DESCRIPTION = "Generates Karaf features file"

    public static final String FEATURES_XMLNS_PREFIX = 'http://karaf.apache.org/xmlns/features/v'
    public static final VersionNumber XMLNS_V13 = new  VersionNumber(1, 3, 0, null)

    @TaskAction
    def run() {
        if(extension.features.featureDescriptors) {
            File outputFile = extension.features.getOutputFile()

            // write out a features repository xml.
            if(!outputFile.parentFile.exists()) {
                outputFile.parentFile.mkdirs()
            }

            println "$outputFile"
            def out = new BufferedWriter(new FileWriter(outputFile))
            out.write(generateFeatures(extension.features))
            out.close()
        }
    }

    String generateFeatures(KarafFeaturesExtension featuresExtension) {
        def writer = new StringWriter()

        def builder = new MarkupBuilder(writer)
        builder.setOmitNullAttributes(true)
        builder.setDoubleQuotes(true)

        def xsdVer13 = VersionNumber.parse(featuresExtension.xsdVersion).compareTo(XMLNS_V13) >= 0

        builder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8", standalone: "yes")
        builder.features(xmlns:FEATURES_XMLNS_PREFIX + featuresExtension.xsdVersion, name: featuresExtension.name) {
            featuresExtension.repositories.each {
                builder.repository( it )
            }
            featuresExtension.featureDescriptors.each { feature ->
                builder.feature(name: feature.name, version: feature.version, description: feature.description) {
                    feature.featureDependencies.each {
                        builder.feature(
                            [
                                version:  it.version,
                                dependency: (it.dependency && xsdVer13) ? true : null
                            ],
                            it.name
                        )
                    }

                    featuresExtension.resolver.resolve(feature).each {
                        builder.bundle(it.attributes, it.url)
                    }
                }
            }
        }

        return writer.toString()
    }
}
