_project_rb_complete() {
  COMPREPLY=()
  local word="${COMP_WORDS[COMP_CWORD]}"
  local completions="$($1 --cmplt "$COMP_CWORD" "${COMP_WORDS[@]}")"
  COMPREPLY=( $(compgen -W "$completions" -- "$word") )
}

complete -F _project_rb_complete ./project.rb
