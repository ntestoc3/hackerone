(defproject hackerone "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [floatingpointio/graphql-builder "0.1.9"]

                 [clj-http "3.10.0"]
                 [cheshire "5.8.1"]
                 ]
  :main ^:skip-aot hackerone.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})