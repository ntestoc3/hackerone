(ns hackerone.core
  (:require [graphql-builder.parser :refer [parse defgraphql]]
            [graphql-builder.core :as graphql]
            [graphql-builder.util :as graphql-util]
            [common.http :as http]
            [common.config :as config]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [com.rpl.specter :refer :all])
  (:gen-class))

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
    (log/info :hackerone-graphql-query args body)
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
(def program-query (with-redefs-fn {#'graphql-builder.generators.shared/argument-value
                                    (fn [argument config]
                                      (let [value (:value argument)
                                            variable-name (:variable-name argument)]
                                        (cond
                                          (not (nil? value)) (graphql-builder.generators.shared/argument-value-value argument)
                                          (not (nil? variable-name)) (str "$" (graphql-builder.generators.shared/add-var-prefix (:prefix config) variable-name)))))}
                     #(graphql/query-map directory-query)))

(def valid-assets #{"CIDR"
                    "URL"
                    "APPLE_STORE_APP_ID"
                    "TESTFLIGHT"
                    "OTHER_IPA"
                    "GOOGLE_PLAY_APP_ID"
                    "OTHER_APK"
                    "WINDOWS_APP_STORE_APP_ID"
                    "SOURCE_CODE"
                    "DOWNLOADABLE_EXECUTABLES"
                    "HARDWARE"
                    "OTHER"})

(defn query-dirs
  ":asset-type 不指定则查找任意类型asset, 必须为有效的asset类型
  "
  [{:keys [ibb bounties high-response managed active asset-type first cursor]
    :or {first 25
         active true} :as opt}]
  {:pre [(or (nil? asset-type)
             (valid-assets asset-type))]}
  (let [args (cond-> {:first first
                      :secureOrderBy {:started_accepting_at {:_direction "DESC"}}
                      :where {"_and"
                              (cond-> []
                                ibb (conj {"internet_bug_bounty" {"_eq" true}})
                                bounties (conj {"_or" [{"offers_bounties" {"_eq" true}}
                                                       {"external_program" {"offers_rewards" {"_eq" true}}}]})
                                high-response (conj {"response_efficiency_percentage" {"_gt" 80}})
                                managed (conj {"triage_subscriptions" {"is_active" true}})
                                asset-type (conj {"structured_scopes" {"_and"
                                                                       [{"asset_type" {"_eq" asset-type}}
                                                                        {"is_archived" false}]}})
                                active (conj {"_or" [{"submission_state" {"_eq" "open"}}
                                                     {"submission_state" {"_eq" "api_only"}}
                                                     {"external_program" {"id" {"_is_null" false}}}]})
                                :always (conj {"external_program" {"id" {"_is_null" true}}}
                                              {"_or" [{"_and" [{"state" {"_neq" "sandboxed"}}
                                                               {"state" {"_neq" "soft_launched"}}]}
                                                      {"external_program" {"id" {"_is_null" false}}}]}))}}
               cursor (assoc :cursor cursor))]
    (hackerone-graphql-query program-query :directory-query args)))

(defn page-seq
  "分页请求
  `page-fn` 请求数据的函数
  `args` 传递给page-fn的参数
  `next-arags-fn` 接受(page-fn args)的结果，并生成下一次page-fn的函数
  `delay` 延时，单位为毫秒
  "
  ([page-fn args next-args-fn] (page-seq page-fn args next-args-fn nil))
  ([page-fn args next-args-fn delay]
   (lazy-seq
    (let [result (page-fn args)]
      (when result
        (cons result
             (do
               (when delay (Thread/sleep delay))
               (page-seq page-fn
                         (next-args-fn result)
                         next-args-fn
                         delay))))))))

(defn get-page-cursor
  [page-info]
  (when (:hasNextPage page-info)
    (:endCursor page-info)))

(defn get-all-programs
  "获取指定页数的program信息"
  [opts pages]
  (some->> (page-seq query-dirs opts
                     #(some->> (select-one [:teams :pageInfo] %)
                               get-page-cursor
                               (hash-map :cursor)
                               (merge opts))
                     500)
           (take pages)
           (map #(select [:teams :edges ALL :node] %1))
           (apply concat)))

(defn get-scope-info
  "获取程序的范围信息"
  ([handle] (get-scope-info handle nil))
  ([handle {:keys [first]
            :or {first 500}}]
   (let [args {:first first
               :handle handle}
         team-info (hackerone-graphql-query program-query :team-assets args)]
     (when team-info
       {:last-update (select-one [:team :scope_version :max_updated_at] team-info)
       :in-scopes (select [:team :in_scopes :edges ALL :node] team-info)
       :out-scopes (select [:team :out_scopes :edges ALL :node] team-info)}))))

(comment

  (config/set-config! :default-http-option {:proxy-host "127.0.0.1"
                                            :proxy-port 8080
                                            :insecure? true})

  (def ds (get-all-programs {:asset-type "URL"
                             :bounties true}
                            2))


  (def s1 (get-scope-info "pixiv"))


  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

