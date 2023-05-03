version 1.0
workflow EchoWF {
  call EchoTask
}
task EchoTask {
  command <<<
     echo hello
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
