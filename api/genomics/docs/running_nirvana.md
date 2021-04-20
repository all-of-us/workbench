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

### Download nirvana annotation files (~22Gb)

I just used the built in downloader in the test environment, cell 3 in the notebook file in the next section.
We could have users run the downloader themselves if the directional service perimeter is set up. Otherwise,
we can upload the files to a featured workspace and ask users to download the files from the 
featured workspace bucket using `gsutil cp`.

### Notebook demo

Running on above docker image (ericsongbroad/aou-nirvana)

https://github.com/all-of-us/workbench/blob/2fc347cfdfc47ffbb148756f1806334d327e3d8e/api/genomics/docs/nirvana.ipynb
