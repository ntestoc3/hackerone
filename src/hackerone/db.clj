(ns hackerone.db
  (:require [crux.api :as crux]
            [me.raynes.fs :as fs]
            [lambdaisland.deep-diff :as ddiff]
            [java-time]
            [clojure.data :refer [diff]]
            [clojure.java.io :as io]
            [taoensso.timbre :as log])
  (:import [crux.api ICruxAPI]))

(def my-node nil)
(def my-node
  (crux/start-node
   {:crux.node/topology 'crux.kafka/topology
    :crux.kafka/bootstrap-servers "localhost:9092"
    }))

(def ^crux.api.ICruxAPI node
  (crux/start-node {:crux.node/topology :crux.kafka/topology
                    :crux.node/kv-store "crux.kv.memdb/kv"
                    :crux.kafka/group-id           "node-2"
                    :crux.kafka/bootstrap-servers "localhost:9092"}))

(defn make-id
  "构造一个id"
  [ns-key id-name]
  (keyword (name ns-key) (name id-name)))

(defn make-entity-type
  "构造实体类型"
  [entity-name entity-type]
  {:entity/name entity-name
   :entity/type entity-type})

(defn submap?
  "第一个map是否为第二个的子集"
  [m1 m2]
  (= m1 (select-keys m2 (keys m1))))

(defn save-program-scope!
  [handle scope-info]
  (let [id (make-id :hackerone.scope handle)]
    (crux/submit-tx
     my-node
     [[:crux.tx/put
       (merge {:crux.db/id id}
              (make-entity-type :program/scope :hackerone)
              (select-keys scope-info [:in-scopes :out-scopes]))]])))

(defn save-program!
  [program scope-info]
  (let [handle (:handle program)
        id (make-id :hackerone.program handle)
        old-info (crux/entity (crux/db my-node) id)
        last-scope-date (:last-update scope-info)]
    (when-not (submap? program old-info)
      (log/info :save-program! "new program:" handle)
      (crux/submit-tx
       my-node
       [[:crux.tx/put
         (merge {:crux.db/id id
                 :last-scope-update last-scope-date}
                (make-entity-type :program :hackerone)
                program)
         ]]))
    (when-not (= last-scope-date (:last-scope-update old-info))
      (log/info :save-program! "new program scope:" handle)
      (save-program-scope! handle scope-info))))

(comment

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
   [[:crux.tx/put
     {:crux.db/id :hackerone.program/ruby
      :name "ruby"
      :handle "ruby"
      :inscopes [{:type "domain" :name "www.ruby.com"}
                 {:type "Android" :name "app"}]
      }
     ]])

  (crux/submit-tx
   my-node
   [[:crux.tx/put
     {:crux.db/id :hackerone.program/ruby
      :name "ruby"
      :handle "ruby"
      :action "do test"
      }
     ]])

  (crux/submit-tx
   my-node
   [[:crux.tx/put
     {:crux.db/id :hackerone.program/ruby
      :name "ruby"
      :handle "ruby666"
      }
     ]])

  (crux/history my-node :hackerone.program/ruby)

  (crux/entity (crux/db my-node) :hackerone.program/ruby)

  (def prog-scopes (crux/q (crux/db my-node)
                           '{:find [e scopes]
                             :where [[e :entity/name :program/scope]
                                     [e :in-scopes scopes]]}))

  (defn find-url-info [url]
    (filter #(-> (second %)
                 :asset_identifier
                 (= url))
            prog-scopes))

  ;; in scopes infos
  (crux/q (crux/db my-node)
          '{:find [scopes]
            :where [[e :entity/name :program/scope]
                    [e :in-scopes scopes]]})

  (crux/q (crux/db my-node)
          '{:find [e]
            :where [[e :name "pixiv"]]})

  (crux/entity (crux/db my-node) :hackerone.program/pixiv)

  ;; 查看一个id的历史
  (crux/history my-node :hackerone.program/pixiv)
  ;; 根据历史id获得历史entity
  (crux/entity (crux/db my-node) (:crux.db/id (first *1))))
