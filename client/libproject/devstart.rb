require "optparse"
# How can we tell what symbols come out of this import?
require_relative "../../libproject/download-swagger-codegen-cli"
require_relative "../../libproject/utils/common"

def swagger_regen()
  download_swagger_codegen_cli

  common = Common.new
  common.run_inline %W{java -jar #{SWAGGER_CODEGEN_CLI_JAR} generate --lang python --input-spec #{SWAGGER_SPEC} --output py/}
end

Common.register_command({
  :invocation => "swagger-regen",
  :description => "rebuilds the Swagger-generated client libraries",
  :fn => Proc.new { |*args| swagger_regen(*args) }
})
