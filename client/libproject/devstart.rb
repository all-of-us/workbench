require "optparse"
require "fileutils"
require_relative "../../libproject/utils/common"
require_relative "../../libproject/swagger"

def swagger_regen()
  Workbench::Swagger.download_swagger_codegen_cli

  common = Common.new
  #check out py-client branch
  common.run_inline %W{
      java -jar #{Workbench::Swagger::SWAGGER_CODEGEN_CLI_JAR}
      generate --lang python --input-spec #{Workbench::Swagger::SWAGGER_SPEC} --output py/tmp}
  move_opts = {:force => true, :verbose => true}
  FileUtils.mv('py/tmp/swagger_client', 'py/aou_workbench_client/', move_opts)
  FileUtils.mv('py/tmp/docs', 'py/swagger_docs', move_opts)
  FileUtils.mv('py/tmp/README.md', 'py/README.swagger.md', move_opts)
  FileUtils.mv('py/tmp/requirements.txt', 'py/swagger-requirements.txt', move_opts)
  FileUtils.remove_dir('py/tmp')
end

Common.register_command({
  :invocation => "swagger-regen",
  :description => "rebuilds the Swagger-generated client libraries",
  :fn => Proc.new { |*args| swagger_regen(*args) }
})
