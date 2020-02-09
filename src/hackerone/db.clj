(ns hackerone.db
  (:require [crux.api :as crux]
            [me.raynes.fs :as fs]
            [clojure.data :refer [diff]]
            [clojure.java.io :as io])
  (:import [crux.api ICruxAPI]))

(def my-node
  (crux/start-node
   {:crux.node/topology 'crux.standalone/topology
    :crux.node/kv-store 'crux.kv.rocksdb/kv
    :crux.standalone/event-log-dir "data/event-log"
    :crux.kv/db-dir "data/db"}))


(crux/submit-tx
 my-node
 [[:crux.tx/put
   {:crux.db/id :hackerone.program/pixiv
    :name "pixiv"
    :handle "pixiv"
    }
   #inst "2020-02-06T08:30:22"]])

(crux/submit-tx
 my-node
 [[:crux.tx/cas
   {:crux.db/id :hackerone.program/pixiv
    :name "pixiv"
    :handle "pixiv"
    }
   {:crux.db/id :hackerone.program/pixiv
    :name "pixiv"
    :handle "pixiv"
    }]])

(crux/q (crux/db my-node)
        '{:find [e]
          :where [[e :name "pixiv"]]})

(crux/entity (crux/db my-node) :hackerone.program/pixiv)

;; 查看一个id的历史
(crux/history my-node :hackerone.program/pixiv)
;; 根据历史id获得历史entity
(crux/entity (crux/db my-node) (:crux.db/id (first *1)))
