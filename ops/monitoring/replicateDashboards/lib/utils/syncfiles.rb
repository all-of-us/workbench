require "open3"

class SyncFiles
  attr :c

  def initialize(common)
    @c = common
  end

  def is_fswatch_installed()
    status = c.run %W{which fswatch}
    return status.success?
  end

  def src_vol_name()
    env = c.load_env
    "#{env.namespace}-src"
  end

  def output_vol_name()
    env = c.load_env
    "#{env.namespace}-out"
  end

  def get_src_volume_mount()
    if is_fswatch_installed
      %W{-v #{src_vol_name}:/w}
    else
      %W{-v #{ENV["PWD"]}:/w}
    end
  end

  def get_volume_mounts()
    env = c.load_env
    if env.static_file_dest
      get_src_volume_mount + %W{-v #{output_vol_name}:/w/#{env.static_file_dest}}
    else
      get_src_volume_mount
    end
  end

  def log_file_name()
    ".rsync.log"
  end

  def log_message(s)
    File.open(log_file_name, "a") do |file|
      file.write s
    end
  end

  def get_dest_path(src, dst)
    if dst.nil?
      dst = src
    end
    dst = dst.split(/\//).reverse.drop(1).reverse.join("/")
    if not dst.empty?
      dst = "/w/#{dst}"
    else
      dst = "/w"
    end
  end

  def start_rsync_container()
    env = c.load_env
    c.docker.requires_docker
    c.docker.ensure_image("tjamet/rsync")
    cmd = %W{
      docker run -d
        --name #{env.namespace}-rsync
        -v #{src_vol_name}:/w
    }
    if env.static_file_dest
      cmd += %W{-v #{output_vol_name}:/w/#{env.static_file_dest}}
    end
    c.run_inline cmd + %W{-e DAEMON=docker tjamet/rsync}
  end

  def stop_rsync_container()
    env = c.load_env
    c.run_inline %W{docker rm -f #{env.namespace}-rsync}
  end

  def rsync_path(src, dst, log)
    env = c.load_env
    dst = get_dest_path(src, dst)
    rsync_remote_shell = "docker exec -i"
    cmd = %W{
      rsync --blocking-io -azlv --delete -e #{rsync_remote_shell}
        #{src}
        #{env.namespace}-rsync:#{dst}
    }
    if log
      Open3.popen3(*cmd) do |i, o, e, t|
        i.close
        if not t.value.success?
          c.error e.read
          exit t.value.exitstatus
        end
        log_message o.read
      end
    else
      c.run_inline cmd
    end
  end

  def link_static_file(src, dst)
    env = c.load_env
    cmd = %W{docker run --rm -w /w} + get_volume_mounts + %W{alpine ln -snf /w/#{src} /w/#{dst}}
    c.run_inline cmd
  end

  def link_static_files()
    env = c.load_env
    threads = []
    foreach_static_file do |path, entry|
      threads << Thread.new do
        link_static_file "#{path}/#{entry}", "#{env.static_file_dest}/#{entry}"
      end
    end
    threads.each do |t|
      t.join
    end
  end

  def foreach_static_file()
    env = c.load_env
    Dir.foreach(env.static_file_src) do |entry|
      unless [".", ".."].include?(entry)
        yield env.static_file_src, entry
      end
    end
  end

  def watch_path(src, dst)
    Open3.popen3(*%W{fswatch -o #{src}}) do |stdin, stdout, stderr, thread|
      Thread.current["pid"] = thread.pid
      stdin.close
      stdout.each_line do |_|
        rsync_path src, dst, true
      end
    end
  end

  def perform_initial_sync()
    env = c.load_env
    env.source_file_paths.each do |src_path|
      # Copying using tar ensures the destination directories will be created.
      c.pipe(
        %W{env COPYFILE_DISABLE=1 tar -c #{src_path}},
        %W{docker cp - #{env.namespace}-rsync:/w}
      )
      rsync_path src_path, nil, false
    end
    if env.static_file_src
      c.pipe(
        %W{env COPYFILE_DISABLE=1 tar -c #{env.static_file_src}},
        %W{docker cp - #{env.namespace}-rsync:/w}
      )
      rsync_path env.static_file_src, nil, false
    end
  end

  def start_watching_sync()
    env = c.load_env
    File.open(log_file_name, "w") {} # Create and truncate if exists.
    paths_to_watch = env.source_file_paths
    if env.static_file_src
      paths_to_watch += [env.static_file_src]
    end
    paths_to_watch.each do |src_path|
      thread = Thread.new { watch_path src_path, nil }
      at_exit {
        Process.kill("HUP", thread["pid"])
        thread.join
      }
    end
  end

  def maybe_start_file_syncing()
    system_name, _ = Open3.capture2("uname")
    system_name.chomp!
    env = c.load_env
    if env.static_file_src
      c.status "Linking static files..."
      link_static_files
    end
    fswatch_installed = is_fswatch_installed
    if system_name == "Darwin" and not fswatch_installed
      c.error "fswatch is not installed."
      STDERR.puts "File syncing will be extremely slow due to a performance problem in docker.\n" \
        "Installing fswatch is highly recommended. Try:\n\n$ brew install fswatch\n\n"
    end
    if fswatch_installed
      c.status "Starting rsync container..."
      at_exit { stop_rsync_container }
      start_rsync_container
      c.status "Performing initial file sync..."
      perform_initial_sync
      start_watching_sync
      c.status "Watching source files. See log at #{log_file_name}."
    end
  end
end
