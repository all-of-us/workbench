import sys
import os

# This is what I want to do, but there's a bug preventing it: RW-5740
# def print_env_var_or_fail(var):
#   val = os.getenv(var)
#
#   if not val:
#     raise ValueError
#
#   print(val)

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
