import psutil
import os

# Convert to GiB. Note that '/' does not exactly correspond to the disk size
# that we specify in our runtime configuration, but the notebooks dir is a
# docker volume mounted from our requested disk.
psutil.disk_usage(os.getenv('NOTEBOOKS_DIR')).total / (1<<30)
