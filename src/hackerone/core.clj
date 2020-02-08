(ns hackerone.core
  (:require [graphql-builder.parser :refer [defgraphql]]
            [graphql-builder.core :as graphql]
            [graphql-builder.util :as graphql-util]
            [clj-http.client :as client]
            [cheshire.core :as json])
  (:gen-class))

(defmacro trans-fn
  ""
  [form]
  (letfn [(trans-form [form]
            ;; (prn "trans form:" form "exp:" (type (first form)))
            (cond (#{'and 'or} (first form)) (trans-and-or form)
                  (seqable? form) (trans-exp form)
                  :else form))
          (trans-and-or [[and-or & exps]]
            ;; (prn "trans and or:" exps)
            (let [exps (map trans-form exps)]
              ;; (prn "and or exps:" exps)
              `{~(str "_" (name and-or)) [~@exps]}))
          (trans-exp [[op value & objs]]
            ;; (prn "trans exp:" op)
            (loop [objs objs
                   result `{~(str "_" (name op)) ~value}]
              (if-let [curr-obj (first objs)]
                (recur (next objs)
                       `{~curr-obj ~result})
                result)))]
    (trans-form form)))

(comment
  (trans-fn (:eq 3 2))

  (trans-fn (eq "open" "submission_state"))

  (trans-fn (is_null false "id" "submission_state"))

  (trans-fn (and (:neq "id" "5") (or (:is_null false "id")(:eq 3 2))))

  (trans-fn (and (or (eq "open" "submission_state")
                     (eq "api_only" "submission_state")
                     (is_null false "id" "external_program"))
                 (is_null false "id" "external_program")
                 (or (neq "sandboxed" "state")
                     (neq "soft_launched" "state")
                     (is_null false "id" "external_program"))))

  )


(defgraphql directory-query "directory.graphql")

(def dir-query (graphql/query-map directory-query {:where {:and []}}))

(with-redefs-fn {#'graphql-util/variables->graphql
                 (fn [vars]
                   (graphql-util/transform-keys name vars))}
  #((get-in dir-query [:query :directory-query])
    {:first 25
     :secureOrderBy {:started_accepting_at {:_direction "DESC"}}
     :where (trans-fn (and (or (eq "open" "submission_state")
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
