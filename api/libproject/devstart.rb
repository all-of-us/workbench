require_relative "utils/common"

def dev_up()
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose up -d}
  at_exit { common.run_inline %W{docker-compose down} }

  common.run_inline_swallowing_interrupt %W{docker-compose logs -f api}
end

def rebuild_image()
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose build}
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment.",
  :fn => Proc.new { |*args| dev_up(*args) }
})

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => Proc.new { |*args| rebuild_image(*args) }
})
