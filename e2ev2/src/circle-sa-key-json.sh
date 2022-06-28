SA_KEY_JSON="$(echo "$GCLOUD_CREDENTIALS" \
  | openssl enc -d -md sha256 -aes-256-cbc -base64 -A -k "$GCLOUD_CREDENTIALS_KEY")"

SA_EMAIL="$(echo "$SA_KEY_JSON" | jq -r .client_email)"
