Code snippets for using Pyspark within the Researcher Workbench.

## Spark console links

```python
from IPython.core.display import HTML
import os

spark_links = [
    {'name': 'Yarn Resource Manager', 'path': 'yarn'},
    {'name': 'MapReduce Job History', 'path': 'jobhistory'},
    {'name': 'YARN Application Timeline', 'path': 'apphistory'},
    {'name': 'Spark History Server', 'path': 'sparkhistory'},
    {'name': 'HDFS NameNode', 'path': 'hdfs'}
]

base_url = f"/proxy/{os.getenv('GOOGLE_PROJECT')}/{os.getenv('RUNTIME_NAME')}"
html_links = [f"<li><a href=\"{base_url}/{link['path']}\">{link['name']}</a>" for link in spark_links]

HTML('<h3>Spark Console links</h3><br/>' + '\n'.join(html_links))
```

## Pyspark Setup

```python
from pyspark import SparkContext
sc = SparkContext.getOrCreate()
```

## Process bucket files via HDFS

Sample code copies a file to worker disk from Cloud storage (via HDFS/GCS connector),
unzips it, then counts the lines.

```python
import os
import subprocess

input_files = !gsutil ls gs://fc-aou-test-datasets-registered/6/wgs/vcf/merged
input_files = input_files[:5]

def wc(path):
    name = os.path.basename(path)
    subprocess.run(['hadoop', 'fs', '-get', '-f', path, name]).check_returncode()
    subprocess.run(['ls']).check_returncode()
    subprocess.run(['gunzip', '-f', name]).check_returncode()
    result = subprocess.run(['wc', '-l', name[:-len('.gz')]], stdout=subprocess.PIPE)
    result.check_returncode()
    return result.stdout

sc.parallelize(input_files).map(wc).collect()
```

## Run an arbitrary bash script over a collection of files

```bash
%%bash

cat > my_script.sh << EOF
#!/bin/bash

echo "processing file" \$1
EOF

chmod +x my_script.sh
./my_script.sh foo.txt
```

```bash
%%bash

hadoop fs -mkdir hdfs:///user/scripts/
hadoop fs -put -f my_script.sh hdfs:///user/scripts/
hadoop fs -ls hdfs:///user/scripts/
```

```python
import os
import subprocess

input_files = !gsutil ls gs://fc-aou-test-datasets-registered/6/wgs/vcf/merged
input_files = input_files[:5]

def script(path):
    subprocess.run(['hadoop', 'fs', '-get', '-f', 'hdfs:///user/scripts/my_script.sh', '.']).check_returncode()
    subprocess.run(['chmod', 'a+x', 'my_script.sh']).check_returncode()
    subprocess.run(['ls']).check_returncode()
    result = subprocess.run(['./my_script.sh', path], stdout=subprocess.PIPE)
    result.check_returncode()
    return result.stdout

sc.parallelize(input_files).map(script).collect()
```

## Write to workspace bucket from workers

```python
# This is necessary to make hadoop fs work, even though GCS doesn't have the concept of directories.
! hadoop fs -mkdir -p “${WORKSPACE_BUCKET}/spark-test/outputs”

import os
import subprocess

out_path = os.getenv('WORKSPACE_BUCKET') + '/spark-test/outputs'
count = 5

def write(i):
    path = f"my_output-{i}-of-{count}.txt"
    with open(path, "w") as f:
        f.write(f"my output {i}")
    
    subprocess.run(['hadoop', 'fs', '-put', '-f', path, f"{out_path}/{path}"]).check_returncode()

sc.range(count).map(write).collect()
```

```bash
! gsutil ls "${WORKSPACE_BUCKET}/spark-test/outputs"
```

## Kill a rogue Spark job

Easiest from the Jupyter terminal:

```bash
yarn application -list
…
                Application-Id      Application-Name        Application-Type          User           Queue  State        Final-State             Progress                        Tracking-URL
application_1628813867520_0002         pyspark-shell                   SPARK       jupyter         defaultRUNNING          UNDEFINED                  10% http://all-of-us-137-m.c.aou-rw-test-92318201.internal:4041
application_1628813867520_0001         pyspark-shell                   SPARK       jupyter         defaultRUNNING          UNDEFINED                  10% http://all-of-us-137-m.c.aou-rw-test-92318201.internal:4040
```

```bash
jupyter@all-of-us-137-m:~$ yarn application -kill application_1628813867520_0002
…
Killing application application_1628813867520_0002
21/08/13 01:34:15 INFO impl.YarnClientImpl: Killed application application_1628813867520_0002
```
