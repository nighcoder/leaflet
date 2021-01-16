(defproject clojupyter-plugin/leaflet "0.1.1-SNAPSHOT"
  :description "Clojupyter plugin"
  :url "http://example.com/FIXME"
  :license {:name "MIT"}

  :dependencies [[clojupyter "0.4.0"]
                 [clojupyter-plugin/widgets "0.1.0"]
                 [camel-snake-kebab "0.4.1"]
                 [cheshire "5.10.0"]]

  :profiles {:dev           {:plugins [[org.clojars.nighcoder/lein-metajar "0.1.2"]]}
             :metajar       {:libdir "../lib"
                             :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}

  :target-path "target/plugins")
