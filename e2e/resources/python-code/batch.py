!pip3 install --quiet dsub
# We attempt to curl any public URL on the Internet. This should fail as batch
# VMs should be restricted from general Internet access. The default timeout
# for curl is very long, set it to 20s instead.
dsub_lines = !dsub \
    --provider google-cls-v2 \
    --project ${GOOGLE_PROJECT} \
    --image gcr.io/cloud-builders/curl \
    --logging ${WORKSPACE_BUCKET}/dsub_logs \
    --command 'curl -m 20 https://raw.githubusercontent.com/all-of-us/workbench/main/LICENSE.txt' \
    --service-account $(gcloud config get-value account) \
    --network "network" \
    --subnetwork "subnetwork" \
    --wait
dsub_out = '\n'.join(dsub_lines)

assert "unexpected exit status 28" in dsub_out, f"expected dsub command to fail with curl exit code 28, got:{dsub_out}"

print("success")
