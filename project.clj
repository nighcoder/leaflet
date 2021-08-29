(defproject org.clojars.nighcoder/leaflet "0.1.1"
  :description "Interactive maps for clojupyter."
  :url "https://github.com/nighcoder/leaflet"
  :license {:name "MIT"}

  :dependencies [[org.clojars.nighcoder/clojupyter "0.4.1"]
                 [org.clojars.nighcoder/widgets "0.1.1"]
                 [camel-snake-kebab "0.4.1"]
                 [cheshire "5.10.0"]]

  :profiles {:dev           {:plugins [[org.clojars.nighcoder/lein-metajar "0.1.2"]]}
             :metajar       {:libdir "../lib"
                             :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}

  :target-path "target/plugins")
