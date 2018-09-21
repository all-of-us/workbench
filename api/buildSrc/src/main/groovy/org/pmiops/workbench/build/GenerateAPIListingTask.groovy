package org.pmiops.workbench.build

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * See https://precisionmedicineinitiative.atlassian.net/browse/PD-2275
 * Run this gradle task from the command line in the /api directory:
 *
 * `gradle listProjectAPIs`
 *
 * This generates a tab separated list of apis and descriptions that can
 * then be easily pasted into a spreadsheet.
 */
@SuppressWarnings(["unused", "GrMethodMayBeStatic"])
@Slf4j
class GenerateAPIListingTask extends DefaultTask {

    @TaskAction
    void defaultAction() {
        List<String> methods = ["get", "delete", "patch", "post", "put"]
        File yaml = new File("src/main/resources/merged.yaml")
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        try {
            Swagger swagger = mapper.readValue(yaml, Swagger.class)
            swagger.paths?.each {
                String path = it.key
                // Find any APIs that match the methods we care about
                Map.Entry api = it.value.find { methods.contains(it.key) }
                if (api) {
                    String method = api.key
                    String fullDescription = api.value.get("description")
                    String description = fullDescription?.
                            trim()?.
                            replaceAll("\r", '')?.
                            replaceAll("\n", '')
                    // logger does not go to stdout here.
                    println("${method.toUpperCase()} $path\t${description ?: ''}")
                }

            }
        } catch (Exception e) {
            logger.error(e.message)
        }
    }

    /**
     * We only need the swagger paths for this feature. Can easily be expanded to
     * pull out other swagger metadata if needed.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Swagger {
        LinkedHashMap<String, LinkedHashMap> paths
    }

}
