FROM us.gcr.io/broad-dsp-gcr-public/terra-jupyter-python:0.0.14 AS python

FROM us.gcr.io/broad-dsp-gcr-public/terra-jupyter-r:1.0.4

# copy everything pip installed from the python image
COPY --from=python /usr/local/lib/python3.7/dist-packages /usr/local/lib/python3.7/dist-packages

USER root

# need to apt-get everything for python since we can only copy pip installed packages
RUN apt-get update && apt-get install -yq --no-install-recommends \
  jq \
  python3.7-dev \
  python-tk \
  openjdk-8-jdk \
  tk-dev \
  libssl-dev \
  xz-utils \
  libhdf5-dev \
  openssl \
  make \
  liblzo2-dev \
  zlib1g-dev \
  libz-dev \
  libmagick++-dev \
  iproute2 \
  bcftools \
  # specify Java 8
  && update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

# Install Wondershaper from source, for client-side egress limiting.
RUN cd /usr/local/share && \
  git clone https://github.com/magnific0/wondershaper.git --depth 1 && \
  cd wondershaper && \
  make install && \
  cd $HOME

RUN echo "Sys.setenv(RETICULATE_PYTHON = '$(which python3)')" >> ~/.Rprofile

# Install GATK
ENV GATK_VERSION 4.1.7.0-31-g5ae5727-SNAPSHOT
ENV GATK_ZIP_PATH /tmp/gatk.zip

COPY gatk-ah_var_store-0.2.zip $GATK_ZIP_PATH
RUN unzip -o $GATK_ZIP_PATH -d /etc/ \
 && ln -s /etc/gatk-$GATK_VERSION/gatk /bin/gatk

# Install Notebook libraries as the user.

ENV USER jupyter-user
USER $USER

RUN pip3 install --upgrade \
  # Fix plotnine and its dependency mizani to a version that's compatible with 0.25.* of pandas.
  # The next versions require pandas 1.0 which we're not ready to upgrade to just yet.
  plotnine==0.6.0 \
  mizani==0.6.0 \
  statsmodels==0.10.0rc2 \
  "git+git://github.com/all-of-us/workbench-snippets.git#egg=terra_widgets&subdirectory=py"

