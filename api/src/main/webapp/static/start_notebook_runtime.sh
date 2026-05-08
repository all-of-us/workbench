#!/bin/bash

# Log all commands, logs are written to the Leo staging directory during startup
# and can be found via the Leo API's GetRuntimeResponse.asyncRuntimeFields.stagingBucket.
set -x

# Runs before every Jupyter notebook runtime startup, including both creation
# and resumes. This file is served as a static asset from the Workbench API
# server and its path is passed in as jupyterStartUserScriptUri during notebook
# runtime creation. This runs after initialize_notebook_runtime.sh on creation.

IFACE=$(ip route show default | awk '{print $5}' | head -1)
pushd /usr/local/share/wondershaper
wondershaper -a "$IFACE" -u 16384 # kilobits; 16Mib/s (2MiB/s)
popd

# Exempt restricted.googleapis.com and internal VPC traffic from the upload
# rate limit. These exemptions are safe because:
#   - restricted.googleapis.com (199.36.153.4/30): access is scoped by the
#     VPC Service Perimeter; cross-perimeter exfiltration is blocked at the
#     platform level regardless of bandwidth.
#   - Internal VPC (10.0.0.0/8): covers intra-VM and Dataproc master-to-worker
#     communication that never leaves the VPC.
ROOT=$(tc qdisc show dev "$IFACE" | grep -E 'htb|cbq|tbf|hfsc' | head -1 | awk '{print $3}')
ROOT="${ROOT:-1:}"
tc filter add dev "$IFACE" parent "$ROOT" protocol ip prio 1 \
  u32 match ip dst 199.36.153.4/30 flowid "$ROOT" 2>/dev/null
tc filter add dev "$IFACE" parent "$ROOT" protocol ip prio 1 \
  u32 match ip dst 10.0.0.0/8 flowid "$ROOT" 2>/dev/null
