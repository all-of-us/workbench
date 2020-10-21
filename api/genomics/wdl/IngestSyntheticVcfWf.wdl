version 1.0

workflow IngestSyntheticVcfWf {
  input {
    File base_vcf # Sample VCF file that the randomization will be based on
    File base_vcf_index
    File sample_names # Newline separated list of names 
    String output_bucket # Bucket that will contain the generated TSV data
    File probe_info_file

    Int batch_size # Should be set to 10 for actual runs
  }

  call AddSampleIds {
      input:
        sample_names = sample_names
  }

  call SplitFileIntoNSizeParts {
    input:
      file = AddSampleIds.sample_map,
      n = batch_size
  }

  scatter (sample_map in SplitFileIntoNSizeParts.partitions) {
    call ExtractSampleNamesFromMap {
      input:
        sample_map = sample_map
    }

    call RandomizeVcf {
      input:
        base_vcf = base_vcf,
        base_vcf_index = base_vcf_index,
        file_of_sample_names = ExtractSampleNamesFromMap.sample_names,
        number_of_samples = batch_size, # Not necessarily correct but its close enough to get a usable disk size
    }

    call CreateImportTsvs {
        input:
          input_vcf = RandomizeVcf.randomized_vcf,
          sample_map = sample_map,
          probe_info_file = probe_info_file,
          output_root_directory = output_bucket
    }
  }

  output {}
}

task AddSampleIds {
  input {
    File sample_names
  }

  command <<<
    set -euo pipefail

    cat ~{sample_names} | awk '{print NR","$0}' > sample_map.txt
  >>>

  runtime {
    memory: "1 GiB"
    disks: "local-disk 1 HDD"
    docker: "ubuntu:latest"
    preemptible: 2
  }

  output {
    File sample_map = "sample_map.txt"
  }
}

task ExtractSampleNamesFromMap {
  input {
    File sample_map
  }

  command <<<
    set -euo pipefail
    cat ~{sample_map} | cut -d"," -f2 > sample_names.txt
  >>>

  runtime {
    memory: "1 GiB"
    disks: "local-disk 1 HDD"
    docker: "ubuntu:latest"
    maxRetries: 1
    preemptible: 2
  }

  output {
    File sample_names = "sample_names.txt"
  }
}

task SplitFileIntoNSizeParts {
  input {
    File file
    Int n
  }

  String file_basename = basename(file, ".txt")

  command <<<
    set -euo pipefail

    split --numeric-suffixes=1 --suffix-length=6 -l ~{n} ~{file} ~{file_basename}_
  >>>

  runtime {
    memory: "1 GiB"
    disks: "local-disk 1 HDD"
    docker: "ubuntu:latest"
    maxRetries: 1
    preemptible: 2
  }

  output {
    Array[File] partitions = glob("${file_basename}_*")
  }
}

task RandomizeVcf {
  input {
    File base_vcf
    File base_vcf_index
    File file_of_sample_names
    Int number_of_samples
  }

  Int disk_size = ceil(2 * number_of_samples * size(base_vcf, "GB"))

  command <<<
    set -euo pipefail

    /workbench/api/project.rb randomize-vcf \
    --vcf ~{base_vcf} \
    --sample-names-file ~{file_of_sample_names} \
    --output $(pwd)/randomized.vcf

  >>>

  runtime {
    cpu: 1
    memory: "4 GiB"
    disks: "local-disk " + disk_size + " HDD"
    docker: "gcr.io/all-of-us-workbench-test/randomizevcf:1.0"
    maxRetries: 2
    preemptible: 3
  }

  output {
    File randomized_vcf = "randomized.vcf"
  }
}

task CreateImportTsvs {
  input {
    File input_vcf
    File sample_map
    File probe_info_file
    String output_root_directory
  }
  
  String outdir = sub(output_root_directory, "/$", "")

  Int disk_size = ceil(size(input_vcf, "GB") * 2.5) + 20

  meta {
    description: "Creates TSV files for import into BigQuery"
  }

  command <<<
    set -e

    while IFS= read -r line
    do
      sampleId=`echo $line | cut -d  "," -f 1`
      sampleName=`echo $line | cut -d  "," -f 2`

      bcftools view -s ${sampleName} ~{input_vcf} > sample.vcf

      gatk --java-options "-Xmx2500m" CreateArrayIngestFiles \
        -V sample.vcf \
        --sample-id ${sampleId} \
        --probe-info-file ~{probe_info_file} \
        --ref-version 37

      for i in 1 2 3;
        do gsutil cp *.tsv ~{outdir}/import/ && break || sleep 15;
      done
      
      rm *.tsv
    done < ~{sample_map}

  >>>

  runtime {
    cpu: 1
    memory: "4 GiB"
    docker: "gcr.io/all-of-us-workbench-test/gatk-varstore:3"
    disks: "local-disk " + disk_size + " HDD"
    maxRetries: 2
    preemptible: 3
  }

  output {}
}

