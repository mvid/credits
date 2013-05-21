(defproject credits "0.1.1-logging"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/tools.logging "0.2.4"]
                 [clj-http "0.4.2"]
                 [clj-json "0.5.0"]
                 [ring/ring-core "1.1.2"]
                 [ring/ring-jetty-adapter "1.1.2"]
                 [compojure "1.0.1"]
                 [org.clojars.tavisrudd/redis-clojure "1.3.1"]
                 [clojurewerkz/spyglass "1.1.0-beta3"]]
  :main credits.core
  :offline? true
  :min-lein-version "2.0.0")