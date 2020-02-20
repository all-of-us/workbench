class DockerHelper
  attr :c

  def initialize(common)
    @c = common
  end

  def in_docker?()
    File.exist?("/.dockerenv")
  end

  def requires_docker()
    status = c.run %W{which docker}
    unless status.success?
      c.error "docker not installed."
      STDERR.puts "Installation instructions:"
      STDERR.puts "\n  https://www.docker.com/community-edition\n\n"
      exit 1
    end
    status = c.run %W{docker info}
    unless status.success?
      c.error "`docker info` command failed."
      STDERR.puts "This is usually a permissions problem. Try allowing your user to run docker\n"
      STDERR.puts "without sudo:"
      STDERR.puts "\n$ sudo usermod -aG docker #{ENV["USER"]}\n\n"
      c.error "Note: You will need to log-in to a new shell before this change will take effect.\n"
      exit 1
    end
  end

  def image_exists?(name)
    requires_docker
    fmt = "{{.Repository}}:{{.Tag}}"
    c.capture_stdout(%W{docker images --format #{fmt}}).include?(name)
  end

  def ensure_image(name)
    requires_docker
    if not image_exists?(name)
      c.error "Missing docker image \"#{name}\". Pulling..."
      c.run_inline(%W{docker pull #{name}})
      c.status "Image \"#{name}\" pulled."
    end
  end
end
