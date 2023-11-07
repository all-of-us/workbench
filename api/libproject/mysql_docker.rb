require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

def maybe_dockerize_mysql_cmd(cmd, interactive=false, tty=false)
  # In environments such as CircleCI, these commands may already be executing in a
  # docker container, in which case we've ensured we have mysql installed.
  if Workbench.in_docker?
    return cmd
  end

  # Otherwise, containerize mysql usage. This avoids the requirement for devs to
  # have mysql installed on their workstations.
  return "docker run " +
      "--rm " +
      (interactive ? "-i " : "") +
      (tty ? "-t " : "") +
      "--network host " +
      "--entrypoint '' " +
      "mariadb:10.2 " +
      cmd
end
