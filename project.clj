(defproject hackerone "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [floatingpointio/graphql-builder "0.1.11"]
                 [ntestoc3/common "2.1.5-SNAPSHOT"]
                 [cheshire "5.10.0"]
                 [com.rpl/specter "1.1.3"]
                 [lambdaisland/deep-diff "0.0-47"]
                 [juxt/crux-core "20.01-1.6.2-alpha"] ;; db
                 [juxt/crux-rocksdb "20.01-1.6.2-alpha"]
                 [juxt/crux-kafka "20.01-1.6.2-alpha"]
                 ]
  :profiles {:dev {:dependencies [[midje "1.9.9" :exclusions [org.clojure/clojure]]
                                  ]
                   :plugins [[lein-midje "3.2.1"]]}
             :uberjar {:aot :all}}
  :main ^:skip-aot hackerone.core
  :target-path "target/%s"
  )
