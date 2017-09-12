require "optparse"
require_relative "../../libproject/utils/common"
require_relative "../../libproject/swagger"

def swagger_regen()
  Workbench::Swagger.download_swagger_codegen_cli

  common = Common.new
  common.run_inline %W{
      java -jar #{Workbench::Swagger::SWAGGER_CODEGEN_CLI_JAR}
      generate --lang python --input-spec #{Workbench::Swagger::SWAGGER_SPEC} --output py/}
end

Common.register_command({
  :invocation => "swagger-regen",
  :description => "rebuilds the Swagger-generated client libraries",
  :fn => Proc.new { |*args| swagger_regen(*args) }
})
