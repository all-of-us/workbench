yaml = File.read("src/main/webapp/WEB-INF/app.yaml.template")
ENV.each { |k, v| yaml.gsub! "${#{k}}", v }

# Hacky fix for interpolating environment variables inside the connection strings since docker
# does not do it when loading environment files.
yaml.gsub! "$DB_HOST", ENV["DB_HOST"]

File.write("src/main/webapp/WEB-INF/app.yaml", yaml)
