import sys
import os

print(sys.version)
print(sys.executable)
print(os.getenv('OWNER_EMAIL'))
print(os.getenv('WORKSPACE_CDR'))
print(os.getenv('WORKSPACE_NAMESPACE'))
print(os.getenv('GOOGLE_PROJECT')) # same as WORKSPACE_NAMESPACE
print(os.getenv('CLUSTER_NAME'))
print(os.getenv('WORKSPACE_BUCKET'))

print('success')
