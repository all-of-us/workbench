# Redirection here suppresses this warning:
#   *** WARNING : deprecated key derivation used.
#   Using -iter or -pbkdf2 would be better.
SA_KEY_JSON="$(echo "$GCLOUD_CREDENTIALS" \
  | openssl enc -d -md sha256 -aes-256-cbc -base64 -A -k "$GCLOUD_CREDENTIALS_KEY" 2>/dev/null)"

