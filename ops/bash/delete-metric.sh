# Delete a Stackdriver metric by name. Useful if you create many misnamed metrics during
# development.

# Calls https://cloud.google.com/monitoring/api/ref_v3/rest/v3/projects.metricDescriptors/delete method.
curl -X DELETE https://monitoring.googleapis.com/v3/$1 --header "Authorization: Bearer $(gcloud auth print-access-token)"
