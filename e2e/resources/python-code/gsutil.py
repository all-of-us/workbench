paths = ! gsutil ls $WORKSPACE_BUCKET

notebook_dirs = filter(lambda p: p.startswith('gs://') and p.endswith('/notebooks/'), paths)
assert any(notebook_dirs), f"gsutil did not return expected notebooks dir: {paths}"

print("success")
