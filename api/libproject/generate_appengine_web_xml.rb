xml = File.read("src/main/webapp/WEB-INF/appengine-web.xml.template")
ENV.each { |k, v| xml.gsub! "${#{k}}", v }

# Hacky fix for interpolating environment variables inside the connection strings since docker
# does not do it when loading environment files.
xml.gsub! "$DB_HOST", ENV["DB_HOST"]

File.write("src/main/webapp/WEB-INF/appengine-web.xml", xml)
