version 1.0

workflow IngestSyntheticVcfWf {
  input {
    File base_vcf
    File file_of_sample_names
    String output_file
  }

  call CountSamples {
    input:
      file_of_sample_names = file_of_sample_names
  }

  call RandomizeVcf {
    input:
      base_vcf = base_vcf,
      file_of_sample_names = file_of_sample_names,
      number_of_samples = CountSamples.number_of_samples,
      output_file = output_file
  }

  output {
    File randomized_vcf = RandomizeVcf.randomized_vcf
  }
}

task CountSamples {
  input {
    File file_of_sample_names
  }

  command <<<
    set -euo pipefail

    wc -l ${file_of_sample_names}
  >>>

  runtime {
    memory: "1 GiB"
    disks: "local-disk 1 HDD"
    docker: "ubuntu:latest"
    preemptible: 3
  }

  output {
    Int number_of_samples = stdout()
  }
}

task RandomizeVcf {
  input {
    File base_vcf
    File file_of_sample_names
    Int number_of_samples
    String output_file
  }

  command <<<
    set -euo pipefail

    ./project.rb randomizeVcf \
      --vcf ${base_vcf} \
      --sample-names-file ${file_of_sample_names} \
      --output ${output_file}
  >>>

  runtime {
    memory: "4GiB"
    disks: "local-disk " + number_of_samples + " HDD"
    docker: "gcr.io/all-of-us-workbench-test/randomizevcf:1.0"
    preemptible: 3
  }

  output {
    File randomized_vcf = output_file
  }
}
