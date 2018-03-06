require "open-uri"
require "yaml"
require_relative "utils/common"
require_relative "workbench"

module Workbench
  module Swagger
    SWAGGER_CODEGEN_CLI_JAR = File.join(
        Workbench::LIBPROJECT_DIR, "swagger-codegen-cli.jar")

    def download(url, path)
      File.open(path, "wb") do |f|
        IO.copy_stream(open(url), f)
      end
    end
    module_function :download    
    
    def merge_yaml(main_yaml_file, files_to_include, output_yaml_file)
      main_yaml = YAML.load_file(main_yaml_file)
      sections_to_merge = ['parameters', 'paths', 'definitions']
      files_to_include.each do |file_to_include|
        file_yaml = YAML.load_file(file_to_include)
        sections_to_merge.each do |section|
          if file_yaml.key?(section)
            main_yaml[section] = main_yaml[section].merge(file_yaml[section])
          end
        end        
      end
      File.open(output_yaml_file, 'w') { |f| YAML.dump(main_yaml, f) }
    end
    module_function :merge_yaml
      
    def download_swagger_codegen_cli()
      unless File.exist?(SWAGGER_CODEGEN_CLI_JAR)
        common = Common.new
        jar_url = "https://storage.googleapis.com" +
          "/swagger-codegen-cli/swagger-codegen-cli-2.3.0-20170814.101630-90.jar"
        common.status "#{jar_url} > #{SWAGGER_CODEGEN_CLI_JAR}..."
        download(jar_url, SWAGGER_CODEGEN_CLI_JAR)
      end
    end
    module_function :download_swagger_codegen_cli

  end
end
