import sys
import os

print(sys.version)
print(sys.executable)

# use environ[val] instead of getenv(val) to raise an exception when missing
# this would fail: print(os.environ['NOTHING'])
print(os.environ['OWNER_EMAIL'])
print(os.environ['WORKSPACE_CDR'])
print(os.environ['WORKSPACE_NAMESPACE'])
print(os.environ['GOOGLE_PROJECT']) # same as WORKSPACE_NAMESPACE
print(os.environ['CLUSTER_NAME'])
print(os.environ['WORKSPACE_BUCKET'])

print('success')
