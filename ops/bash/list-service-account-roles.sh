gcloud projects get-iam-policy "$(gcloud config get-value project)"  \
--flatten="bindings[].members" \
--format='table(bindings.role)' \
--filter="bindings.members:$1"