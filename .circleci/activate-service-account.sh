source "$(dirname $0)"/SA_KEY_JSON.sh
source "$(dirname $0)"/SA_EMAIL.sh
gcloud config set survey/disable_prompts True
echo "$SA_KEY_JSON" | gcloud auth activate-service-account "$SA_EMAIL" --key-file=-

