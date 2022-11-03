source "$(dirname $0)"/SA_KEY_JSON.sh
source "$(dirname $0)"/SA_EMAIL.sh
echo "$SA_KEY_JSON" | gcloud auth activate-service-account "$SA_EMAIL" --key-file=-

