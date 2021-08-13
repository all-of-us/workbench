package org.pmiops.workbench.tooling

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

// Custom task based on https://docs.gradle.org/2.5/userguide/custom_tasks.html#incremental_tasks
class IncrementalHotSwapTask extends DefaultTask {
    @InputDirectory
    File inputDir

    @OutputDirectory
    File outputDir

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        // Only handle incremental builds---don't hot-swap on initial compile.
        if (inputs.incremental) {

            inputs.outOfDate { change ->
                // Take the modified class, attach to the live JVM debug port, and attempt to
                // redefine the class via jdb.
                String prefix = inputDir.path + "/"
                String relativePath = change.file.path.substring(prefix.length())
                String fqClassName = relativePath[0..-(".class".length() + 1)].replaceAll("/", ".")
                printf "Hot swapping $fqClassName from $change.file.path...\n"
                def proc = "echo redefine $fqClassName $change.file.path".execute() \
                         | "${System.getenv('JAVA_HOME')}/bin/jdb -attach 8001".execute()
                def jdbOutput = new StringBuffer()
                proc.consumeProcessOutput(jdbOutput, System.err)
                proc.waitFor()

                // Hacky, but from the shell we don't have a clear way of extracting a status of an
                // "interactive" jdb command from the process exit status. Checking for output
                // presence in general is not ideal, since it often reports some innocuous errors
                // on launch. From experimentation, the jdb output typically looks like:
                // "Error redefining <class name> ... schema change not implemented"
                if (proc.exitValue() == 0 && !jdbOutput.contains("Error redefining")) {
                    println "hot swap succeeded"
                } else {
                    println "hot swap failed; the code change you made may not be supported by " +
                            "jdb redefine (for example, adding/renaming static variables), you " +
                            "may need to restart your API server to pick this up. See the logs " +
                            "below for more details"
                    printf "\nhot swap jdb output:\n  $jdbOutput\n\n"
                }
            }

            inputs.removed { change ->
                println "Class removed (may require restart): ${change.file.path}"
            }
        } else {
            println "Watching ${inputDir}"
        }
    }

}
