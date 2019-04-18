for line in $(awk '!/^ *#/ && NF' $WORKBENCH_DIR/api/db/vars.env); do
  IFS='=' read -r var val <<< "$line"

  if [ "$var" == "DB_HOST" ]; then
    export DB_HOST=localhost
    continue
  fi

  evaluatedString=$(echo $(eval "echo $val"))
  export $var=$evaluatedString
done
