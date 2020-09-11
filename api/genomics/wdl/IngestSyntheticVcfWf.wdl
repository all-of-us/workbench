version 1.0

workflow IngestSyntheticVcfWf {
  input {
    File base_vcf # Sample VCF file that the randomization will be based on
    File base_vcf_index
    File sample_names # Newline separated list of names 
    Int num_partitions # sample_names will be evenly split across this #
    String output_bucket # Bucket that will contain the generated TSV data
    File probe_info_file

    Int batch_size # Should be set to 10 for actual runs
  }

  call AddSampleIds {
      input:
        sample_names = sample_names
  }

  call SplitFileIntoNParts {
    input:
      file = AddSampleIds.sample_map,
      n = num_partitions
  }

  scatter (partitioned_sample_map in SplitFileIntoNParts.partitions) {
    call SplitFileIntoNSizeParts {
      input:
        file = partitioned_sample_map,
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
    preemptible: 2
  }

  output {
    File sample_names = "sample_names.txt"
  }
}

task SplitFileIntoNParts {
  input {
    File file
    Int n
  }

  String file_basename = basename(file, ".txt")

  command <<<
    set -euo pipefail

    for i in $(seq 1 ~{n})
    do
      cat ~{file} | awk -v partition=${i} -v n=~{n} '{if(NR%n + 1 == partition){print $0}}' >> ~{file_basename}_${i}
    done
    
    echo "ls"
    ls
    echo "ls ~{file_basename}_*"
    ls ~{file_basename}_*
  >>>

  runtime {
    memory: "1 GiB"
    disks: "local-disk 1 HDD"
    docker: "ubuntu:latest"
    preemptible: 2
  }

  output {
    Array[File] partitions = glob("${file_basename}_*")
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
    
    # Just in case - check if the user is about to create partitions that are greater than the 4k limit
    lines=`cat ~{file} | wc -l`
    if [ "$lines" -gt 4000 ]
    then
      exit 1
    fi

    split --numeric-suffixes=1 -l ~{n} ~{file} ~{file_basename}_
  >>>

  runtime {
    memory: "1 GiB"
    disks: "local-disk 1 HDD"
    docker: "ubuntu:latest"
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

    mkdir output_tsvs

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

      mv *.tsv output_tsvs
    done < ~{sample_map}

    partition_number=`echo ~{basename(sample_map)} | awk -F'_' '{print $(NF-1) + 0}'`

    gsutil cp output_tsvs/*.tsv ~{outdir}/import/${partition_number}/ready/
  >>>

  runtime {
    cpu: 1
    memory: "4 GiB"
    docker: "gcr.io/all-of-us-workbench-test/gatk-varstore:2"
    disks: "local-disk " + disk_size + " HDD"
    preemptible: 3
  }

  output {}
}

