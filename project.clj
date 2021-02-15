(defproject clj-camunda "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.h2database/h2 "1.4.200"]
                 [clojure-jsr223/clojure-jsr223 "1.2"]]
  :resource-paths ["lib/camunda-engine-7.15.0-SNAPSHOT.jar" "resources"]
  :repl-options {:init-ns clj-camunda.core})
