gcloud iam service-accounts keys create ./sa-key.json \
  --iam-account $1@$(gcloud config get-value project).iam.gserviceaccount.com
