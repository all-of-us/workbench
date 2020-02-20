project_id="$(gcloud config get-value project)"
curl -X POST https://iam.googleapis.com/v1/projects/$project_id/serviceAccounts/$1@$project_id.iam.gserviceaccount.com/keys \
  --header "Authorization: Bearer $(gcloud auth print-access-token)"