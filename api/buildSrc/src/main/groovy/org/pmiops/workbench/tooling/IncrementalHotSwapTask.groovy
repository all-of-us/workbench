package org.pmiops.workbench.tooling

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

// Custom task based on https://docs.gradle.org/2.5/userguide/custom_tasks.html#incremental_tasks
class IncrementalHotSwapTask extends DefaultTask {
    @InputDirectory
    File inputDir

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        // Only handle incremental builds---don't hot-swap on initial compile.
        if (inputs.incremental) {

            inputs.outOfDate { change ->
                String prefix = inputDir.path + "/"
                String relativePath = change.file.path.substring(prefix.length())
                String fqClassName = relativePath[0..-(".class".length() + 1)].replaceAll("/", ".")
                printf "Hot swapping $fqClassName from $change.file.path..."
                def proc = "echo redefine $fqClassName $change.file.path".execute() \
            | "${System.getenv('JAVA_HOME')}/bin/jdb -attach 8001".execute()
                proc.waitFor()
                println "done."
            }

            inputs.removed { change ->
                println "Class removed (may require restart): ${change.file.path}"
            }
        } else {
            println "Watching ${inputDir}"
        }
    }

}
