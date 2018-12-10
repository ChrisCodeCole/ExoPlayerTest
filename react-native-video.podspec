require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "skitter-video"
  s.version      = package["version"]
  s.summary      = "A <Video /> element for react-native"
  s.author       = "Sungjae Kim <sungjkim34@gmail.com> (https://github.com/sungjkim34)"

  s.homepage     = "https://github.com/sungjkim34"

  s.license      = "MIT"

  s.ios.deployment_target = "7.0"
  s.tvos.deployment_target = "9.0"

  s.source       = { :git => "https://github.com/sungjkim34/skitter-video.git", :tag => "#{s.version}" }

  s.source_files  = "ios/*.{h,m}"

  s.dependency "React"
end
