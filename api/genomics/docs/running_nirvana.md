## Installing Nirvana on AoU runtimes

### terra-docker changes
Git .patch file on top of commit [f84de6452b3](https://github.com/DataBiosphere/terra-docker/commit/f84de6452b3cb6a354d98d4c711105eb2f76e022)  
I have a docker image with these changes at `ericsongbroad/aou-nirvana` if you want to apply it to your local config and try it out.
```
From 3515852c63fcb6813f0bd66627bf46baa16d5764 Mon Sep 17 00:00:00 2001
From: Eric Song <me@ericsong.io>
Date: Tue, 20 Apr 2021 16:55:52 -0400
Subject: [PATCH] add Nirvana

---
 terra-jupyter-aou/Dockerfile | 26 ++++++++++++++++++++++++++
 1 file changed, 26 insertions(+)

diff --git a/terra-jupyter-aou/Dockerfile b/terra-jupyter-aou/Dockerfile
index ba7b907..6cb4068 100644
--- a/terra-jupyter-aou/Dockerfile
+++ b/terra-jupyter-aou/Dockerfile
@@ -26,11 +26,36 @@ RUN apt-get update && apt-get install -yq --no-install-recommends \
   libz-dev \
   libmagick++-dev \
   iproute2 \
+	# Nirvana/.NET Core dependencies \
+  ca-certificates \
+	libc6 \
+	libgcc1 \
+	libgssapi-krb5-2 \
+	libicu60 \
+	liblttng-ust0 \
+	libssl1.0.0 \
+	libstdc++6 \
+	zlib1g \
   # specify Java 8
   && update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java \
   && apt-get clean \
   && rm -rf /var/lib/apt/lists/*
 
+# Install .NET Core + Nirvana
+ENV DOTNET_VERSION=2.1.26
+ENV NIRVANA_ZIP_URL=https://github.com/Illumina/Nirvana/releases/download/v3.14.0/Nirvana-3.14.0-dotnet-2.1.0.zip
+
+RUN curl -SL --output dotnet.tar.gz https://dotnetcli.azureedge.net/dotnet/Runtime/$DOTNET_VERSION/dotnet-runtime-$DOTNET_VERSION-linux-x64.tar.gz \
+    && dotnet_sha512='41cc13f14dd7721a079bdd0ab489de40e9d4f32787239a26e7d10fcb0020a8e78d446c3b430b4bf80a557a925b3ca87d7981bfda4bbf9495cc44b1d42d877c40' \
+    && echo "$dotnet_sha512 dotnet.tar.gz" | sha512sum -c - \
+    && mkdir -p /usr/share/dotnet \
+    && tar -zxf dotnet.tar.gz -C /usr/share/dotnet \
+    && rm dotnet.tar.gz \
+    && ln -s /usr/share/dotnet/dotnet /usr/bin/dotnet \
+    && mkdir -p /opt/nirvana \
+    && curl -SL --output nirvana.zip $NIRVANA_ZIP_URL \
+    && unzip -d /opt/nirvana nirvana.zip \
+    && rm nirvana.zip
 
 # Spark setup.
 # Copied from terra-jupyter-hail; keep updated.
@@ -89,3 +114,4 @@ RUN pip3 install --upgrade \
   "git+git://github.com/all-of-us/workbench-snippets.git#egg=terra_widgets&subdirectory=py" \
   && mkdir -p /home/$USER/.config/git \
   && nbstripout --install --global
+
-- 
2.29.2
```

### Notebook demo

Running on above docker image (ericsongbroad/aou-nirvana)

```python
!curl -O https://illumina.github.io/NirvanaDocumentation/files/HiSeq.10000.vcf.gz 
```

      % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                     Dload  Upload   Total   Spent    Left  Speed
    100  464k  100  464k    0     0  1659k      0 --:--:-- --:--:-- --:--:-- 1659k



```python
%env NIRVANA_SCRATCH /home/jupyter-user/notebooks/nirvana_scratch
```

    env: NIRVANA_SCRATCH=/home/jupyter-user/notebooks/nirvana_scratch



```bash
%%bash
mkdir $NIRVANA_SCRATCH
dotnet /opt/nirvana/Downloader.dll --ga GRCh38 -o $NIRVANA_SCRATCH
```

    ---------------------------------------------------------------------------
    Downloader                                          (c) 2020 Illumina, Inc.
    Stromberg, Roy, Lajugie, Jiang, Li, and Kang             3.14.0-0-g19868c36
    ---------------------------------------------------------------------------
    
    - downloading manifest... 38 files.
    
    - downloading file metadata:
      - finished (00:00:00.4).
    
    - downloading files (21.761 GB):
      - downloading MITOMAP_20200819.nsa.idx (GRCh38)
      - downloading 1000_Genomes_Project_Phase_3_v3_plus_refMinor.rma.idx (GRCh38)
      - downloading ClinVar_20200903.nsa.idx (GRCh38)
      - downloading ClinGen_Dosage_Sensitivity_Map_20200913.nsi (GRCh38)
      - downloading MITOMAP_SV_20200819.nsi (GRCh38)
      - downloading REVEL_20200205.nsa.idx (GRCh38)
      - downloading PrimateAI_0.2.nsa.idx (GRCh38)
      - downloading ClinGen_Dosage_Sensitivity_Map_20200913.nga (GRCh38)
      - downloading dbSNP_151_globalMinor.nsa.idx (GRCh38)
      - downloading ClinGen_disease_validity_curations_20200914.nga (GRCh38)
      - downloading phyloP_hg38.npd.idx (GRCh38)
      - downloading 1000_Genomes_Project_Phase_3_v3_plus.nsa.idx (GRCh38)
      - downloading SpliceAi_1.3.nsa.idx (GRCh38)
      - downloading TOPMed_freeze_5.nsa.idx (GRCh38)
      - downloading dbSNP_154.nsa.idx (GRCh38)
      - downloading MITOMAP_20200819.nsa (GRCh38)
      - downloading gnomAD_2.1.nsa.idx (GRCh38)
      - downloading gnomAD_gene_scores_2.1.nga (GRCh38)
      - downloading ClinGen_20160414.nsi (GRCh38)
      - downloading 1000_Genomes_Project_(SV)_Phase_3_v5a.nsi (GRCh38)
      - downloading OMIM_20200915.nga (GRCh38)
      - downloading 1000_Genomes_Project_Phase_3_v3_plus_refMinor.rma (GRCh38)
      - downloading MultiZ100Way_20171006.pcs (GRCh38)
      - downloading gnomAD_LCR_2.1.lcr (GRCh38)
      - downloading ClinVar_20200903.nsa (GRCh38)
      - downloading Both.transcripts.ndb (GRCh38)
      - downloading REVEL_20200205.nsa (GRCh38)
      - downloading PrimateAI_0.2.nsa (GRCh38)
      - downloading dbSNP_151_globalMinor.nsa (GRCh38)
      - downloading Both.sift.ndb (GRCh38)
      - downloading Both.polyphen.ndb (GRCh38)
      - downloading Homo_sapiens.GRCh38.Nirvana.dat
      - downloading 1000_Genomes_Project_Phase_3_v3_plus.nsa (GRCh38)
      - downloading phyloP_hg38.npd (GRCh38)
      - downloading SpliceAi_1.3.nsa (GRCh38)
      - downloading TOPMed_freeze_5.nsa (GRCh38)
      - downloading dbSNP_154.nsa (GRCh38)
      - downloading gnomAD_2.1.nsa (GRCh38)
      - finished (00:04:20.1).
    
    Description                                                     Status
    ---------------------------------------------------------------------------
    1000_Genomes_Project_(SV)_Phase_3_v5a.nsi (GRCh38)                OK
    1000_Genomes_Project_Phase_3_v3_plus_refMinor.rma (GRCh38)        OK
    1000_Genomes_Project_Phase_3_v3_plus_refMinor.rma.idx (...        OK
    1000_Genomes_Project_Phase_3_v3_plus.nsa (GRCh38)                 OK
    1000_Genomes_Project_Phase_3_v3_plus.nsa.idx (GRCh38)             OK
    Both.polyphen.ndb (GRCh38)                                        OK
    Both.sift.ndb (GRCh38)                                            OK
    Both.transcripts.ndb (GRCh38)                                     OK
    ClinGen_20160414.nsi (GRCh38)                                     OK
    ClinGen_disease_validity_curations_20200914.nga (GRCh38)          OK
    ClinGen_Dosage_Sensitivity_Map_20200913.nga (GRCh38)              OK
    ClinGen_Dosage_Sensitivity_Map_20200913.nsi (GRCh38)              OK
    ClinVar_20200903.nsa (GRCh38)                                     OK
    ClinVar_20200903.nsa.idx (GRCh38)                                 OK
    dbSNP_151_globalMinor.nsa (GRCh38)                                OK
    dbSNP_151_globalMinor.nsa.idx (GRCh38)                            OK
    dbSNP_154.nsa (GRCh38)                                            OK
    dbSNP_154.nsa.idx (GRCh38)                                        OK
    gnomAD_2.1.nsa (GRCh38)                                           OK
    gnomAD_2.1.nsa.idx (GRCh38)                                       OK
    gnomAD_gene_scores_2.1.nga (GRCh38)                               OK
    gnomAD_LCR_2.1.lcr (GRCh38)                                       OK
    Homo_sapiens.GRCh38.Nirvana.dat                                   OK
    MITOMAP_20200819.nsa (GRCh38)                                     OK
    MITOMAP_20200819.nsa.idx (GRCh38)                                 OK
    MITOMAP_SV_20200819.nsi (GRCh38)                                  OK
    MultiZ100Way_20171006.pcs (GRCh38)                                OK
    OMIM_20200915.nga (GRCh38)                                        OK
    phyloP_hg38.npd (GRCh38)                                          OK
    phyloP_hg38.npd.idx (GRCh38)                                      OK
    PrimateAI_0.2.nsa (GRCh38)                                        OK
    PrimateAI_0.2.nsa.idx (GRCh38)                                    OK
    REVEL_20200205.nsa (GRCh38)                                       OK
    REVEL_20200205.nsa.idx (GRCh38)                                   OK
    SpliceAi_1.3.nsa (GRCh38)                                         OK
    SpliceAi_1.3.nsa.idx (GRCh38)                                     OK
    TOPMed_freeze_5.nsa (GRCh38)                                      OK
    TOPMed_freeze_5.nsa.idx (GRCh38)                                  OK
    ---------------------------------------------------------------------------
    
    Time: 00:04:21.2



```bash
%%bash
# Uncomment if running for the 2nd+ time
# rm -f HiSeq_annotations.json.gz
# rm -f HiSeq_annotations.json

dotnet /opt/nirvana/Nirvana.dll \
     -c $NIRVANA_SCRATCH/Cache/GRCh38/Both \
     -r $NIRVANA_SCRATCH/References/Homo_sapiens.GRCh38.Nirvana.dat \
     --sd $NIRVANA_SCRATCH/SupplementaryAnnotation/GRCh38 \
     -i HiSeq.10000.vcf.gz \
     -o HiSeq_annotations

gunzip HiSeq_annotations.json.gz
```

    ---------------------------------------------------------------------------
    Nirvana                                             (c) 2020 Illumina, Inc.
    Stromberg, Roy, Lajugie, Jiang, Li, and Kang             3.14.0-0-g19868c36
    ---------------------------------------------------------------------------
    
    Initialization                                         Time     Positions/s
    ---------------------------------------------------------------------------
    Cache                                               00:00:05.7
    SA Position Scan                                    00:00:00.6       15,639
    
    Reference                                Preload    Annotation   Variants/s
    ---------------------------------------------------------------------------
    chr1                                    00:00:02.1  00:00:08.0        1,241
    
    Summary                                                Time         Percent
    ---------------------------------------------------------------------------
    Initialization                                      00:00:06.3       26.4 %
    Preload                                             00:00:02.1        9.1 %
    Annotation                                          00:00:08.0       33.4 %
    
    Time: 00:00:23.3



```python
import json

with open('HiSeq_annotations.json') as json_file:
    annotations = json.load(json_file)

print(json.dumps(annotations['positions'][0], indent=2))
```

    {
      "chromosome": "chr1",
      "position": 109,
      "refAllele": "A",
      "altAlleles": [
        "T"
      ],
      "quality": 0,
      "filters": [
        "FDRtranche2.00to10.00+"
      ],
      "strandBias": -1042.18,
      "mappingQuality": 19.2,
      "cytogeneticBand": "1p36.33",
      "samples": [
        {
          "genotype": "0/1",
          "variantFrequencies": [
            0.349
          ],
          "totalDepth": 308,
          "genotypeQuality": 99,
          "alleleDepths": [
            610,
            327
          ]
        }
      ],
      "variants": [
        {
          "vid": "1-109-A-T",
          "chromosome": "chr1",
          "begin": 109,
          "end": 109,
          "refAllele": "A",
          "altAllele": "T",
          "variantType": "SNV",
          "hgvsg": "NC_000001.11:g.109A>T"
        }
      ]
    }



