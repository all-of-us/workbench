import psutil

# Convert to GiB.
psutil.virtual_memory().total / (1<<30)
