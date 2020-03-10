package org.pmiops.workbench.tooling

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
        String apiYaml = "src/main/resources/workbench-api.yaml"
        printPaths(getApisFromYamlFile(apiYaml))
    }

    private void printPaths(LinkedHashMap<String, LinkedHashMap> paths) {
        List<String> methods = ["get", "delete", "patch", "post", "put"]
        paths?.each {
            String path = it.key
            it.value.findAll { methods.contains(it.key) }.each { api ->
                String method = api.key
                String description = api.value.get("description")?.
                        trim()?.
                        replaceAll("\r", '')?.
                        replaceAll("\n", '')
                // logger does not go to stdout here.
                println("${method.toUpperCase()} $path\t${description ?: ''}")
            }
        }
    }

    private LinkedHashMap<String, LinkedHashMap> getApisFromYamlFile(String path) {
        File yaml = new File(path)
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        try {
            Swagger swagger = mapper.readValue(yaml, Swagger.class)
            return swagger.paths
        } catch (Exception e) {
            logger.error(e.message)
        }
        new LinkedHashMap()
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
