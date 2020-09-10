version 1.0

workflow IngestSyntheticVcfWf {
  input {
    File base_vcf
    File base_vcf_index
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
      base_vcf_index = base_vcf_index,
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

    cat ~{file_of_sample_names} | wc -l
  >>>

  runtime {
    memory: "1 GiB"
    disks: "local-disk 1 HDD"
    docker: "ubuntu:latest"
    preemptible: 3
  }

  output {
    Int number_of_samples = read_int(stdout())
  }
}

task RandomizeVcf {
  input {
    File base_vcf
    File base_vcf_index
    File file_of_sample_names
    Int number_of_samples
    String output_file
  }

  Int disk_size = ceil(2 * number_of_samples * size(base_vcf, "GB"))

  command <<<
    set -euo pipefail

    /workbench/api/project.rb randomize-vcf \
    --vcf ~{base_vcf} \
    --sample-names-file ~{file_of_sample_names} \
    --output $(pwd)/~{output_file}

  >>>

  runtime {
    memory: "4 GiB"
    disks: "local-disk " + disk_size + " HDD"
    docker: "gcr.io/all-of-us-workbench-test/randomizevcf:1.0"
    preemptible: 3
  }

  output {
    File randomized_vcf = output_file
  }
}
