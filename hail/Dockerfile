FROM us.gcr.io/broad-dsp-gcr-public/terra-jupyter-python:0.0.13
USER root

COPY scripts $JUPYTER_HOME/scripts

ENV PIP_USER=false
ENV PYTHONPATH $PYTHONPATH:/usr/lib/spark/python
ENV HAIL_VERSION=0.2.45

RUN find $JUPYTER_HOME/scripts -name '*.sh' -type f | xargs chmod +x \
    && $JUPYTER_HOME/scripts/kernel/kernelspec.sh $JUPYTER_HOME/scripts/kernel /usr/local/share/jupyter/kernels \
    # Note Spark and Hadoop are mounted from the outside Dataproc VM.
    # Make empty conf dirs for the update-alternatives commands.
    && mkdir -p /etc/spark/conf.dist && mkdir -p /etc/hadoop/conf.empty && mkdir -p /etc/hive/conf.dist \
    && update-alternatives --install /etc/spark/conf spark-conf /etc/spark/conf.dist 100 \
    && update-alternatives --install /etc/hadoop/conf hadoop-conf /etc/hadoop/conf.empty 100 \
    && update-alternatives --install /etc/hive/conf hive-conf /etc/hive/conf.dist 100 \
    && apt-get update \
    && apt install -yq --no-install-recommends openjdk-8-jdk \
        g++ \
        liblz4-dev \
    # specify Java 8
    && update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java \
    && pip3 install pypandoc \
    && pip3 install --no-dependencies hail==$HAIL_VERSION \
    && X=$(mktemp -d) \
    && mkdir -p $X \
    && (cd $X && pip3 download hail==$HAIL_VERSION --no-dependencies && \
        unzip hail*.whl &&  \
        grep 'Requires-Dist: ' hail*dist-info/METADATA | sed 's/Requires-Dist: //' | sed 's/ (//' | sed 's/)//' | grep -v 'pyspark' | xargs pip install) \
    && rm -rf $X \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN cd /usr/local/share && \
  git clone https://github.com/magnific0/wondershaper.git --depth 1 && \
  cd wondershaper && \
  make install

ENV PIP_USER=true
USER $USER
