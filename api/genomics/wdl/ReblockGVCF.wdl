version 1.0

# Sourced from https://github.com/broadinstitute/warp/blob/JointGenotyping_v1.4.0/pipelines/broad/dna_seq/germline/joint_genotyping/reblocking/ReblockGVCF.wdl but there's been significant divergence since then to support an array of inputs

workflow ReblockGVCF {

  String pipeline_version = "1.1.0"

  input {
    File gvcfs_list
    String output_gcs_bucket
    String docker_image = "us.gcr.io/broad-gatk/gatk:4.1.8.0"
  }

  String output_gcs_bucket_no_trailing_slash = sub(output_gcs_bucket, "/$", "")

  scatter (gvcf in read_lines(gvcfs_list)) {
    call Reblock {
      input:
        gvcf = gvcf,
        gvcf_index = gvcf + ".tbi",
        output_gcs_bucket = output_gcs_bucket_no_trailing_slash,
        docker_image = docker_image
    }
  }

  output {
    Array[String] output_vcf_gcs_paths = Reblock.output_vcf_path
  }

  meta {
    allowNestedInputs: true
  }
}

task Reblock {

  input {
    File gvcf
    File gvcf_index
    String output_gcs_bucket
    String docker_image
  }

  String gvcf_basename = basename(gvcf, ".g.vcf.gz")
  String output_vcf_filename = gvcf_basename + ".reblocked.g.vcf.gz"

  String output_vcf_gcs_path = output_gcs_bucket + "/" + output_vcf_filename
  String output_vcf_index_gcs_path = output_vcf_gcs_path + ".tbi"

  Int disk_size = ceil(size(gvcf, "GiB")) * 2

  command {
    gatk --java-options "-Xms3g -Xmx3g" \
      ReblockGVCF \
      -V ~{gvcf} \
      -drop-low-quals \
      -do-qual-approx \
      --floor-blocks -GQB 10 -GQB 20 -GQB 30 -GQB 40 -GQB 50 -GQB 60 \
      -O ~{output_vcf_filename}

    gsutil cp ~{output_vcf_filename} ~{output_vcf_gcs_path}
    gsutil cp ~{output_vcf_filename}.tbi ~{output_vcf_index_gcs_path}
  }

  runtime {
    memory: "3.75 GB"
    bootDiskSizeGb: "15"
    disks: "local-disk " + disk_size + " HDD"
    preemptible: 3
    docker: docker_image
  }

  output {
    String output_vcf_path = output_vcf_gcs_path
    String output_vcf_index_path = output_vcf_index_gcs_path
  }
}

