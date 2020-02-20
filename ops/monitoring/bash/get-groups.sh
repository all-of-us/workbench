curl -X GET "https://monitoring.googleapis.com/v3/projects/$(gcloud config get-value project)/groups" \
  --header "Authorization: Bearer $(gcloud auth print-access-token)"

