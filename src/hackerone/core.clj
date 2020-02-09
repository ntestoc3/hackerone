(ns hackerone.core
  (:require [graphql-builder.parser :refer [defgraphql]]
            [graphql-builder.core :as graphql]
            [graphql-builder.util :as graphql-util]
            [common.http :as http]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [com.rpl.specter :refer :all])
  (:gen-class))

(defn ->graphql-exp
  "graphql表达式转换"
  [form]
  (letfn [(trans-form [form]
            ;; (prn "trans form:" form "exp:" (type (first form)))
            (if (seqable? form)
              (if (#{'and 'or} (first form))
                (trans-and-or form)
                (trans-exp form))
              form))
          (trans-and-or [[and-or & exps]]
            ;; (prn "trans and or:" exps)
            (let [exps (map trans-form exps)]
              ;; (prn "and or exps:" exps)
              {(str "_" (name and-or)) exps}))
          (trans-exp [[op value & objs]]
            ;; (prn "trans exp:" op)
            (loop [objs objs
                   result {(str "_" (name op)) value}]
              (if-let [curr-obj (first objs)]
                (recur (next objs)
                       {curr-obj result})
                result)))]
    (trans-form form)))

(defn get-graphql
  [querys operation variables]
  (with-redefs-fn {#'graphql-util/variables->graphql
                   (fn [vars]
                     (graphql-util/transform-keys name vars))}
    (fn []
      (-> ((get-in querys [:query operation]) variables)
          :graphql))))

(defn hackerone-graphql-query
  [querys operation args]
  (let [body (get-graphql querys operation args)]
    (some-> (http/post
             "https://hackerone.com/graphql"
             (http/build-http-opt {:content-type :json
                                   :body (json/encode body)
                                   :cookie-policy :standard
                                   :as :json
                                   :headers {"Referer" "https://hackerone.com/directory/programs"}}))
            :body
            :data)))

(defgraphql directory-query "directory.graphql")
(def program-query (graphql/query-map directory-query))
(def dirs (hackerone-graphql-query program-query
                                   :directory-query
                                   {:first 25
                                    :secureOrderBy {:started_accepting_at {:_direction "DESC"}}
                                    :where (->graphql-exp '(and (or (eq "open" "submission_state")
                                                                    (eq "api_only" "submission_state")
                                                                    (is_null false "id" "external_program"))
                                                                (is_null true "id" "external_program")
                                                                (or (and (neq "sandboxed" "state")
                                                                         (neq "soft_launched" "state"))
                                                                    (is_null false "id" "external_program"))))
                                    }))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
