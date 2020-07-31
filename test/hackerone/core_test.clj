(ns hackerone.core-test
  (:require [midje.sweet :refer :all]
            [hackerone.core :refer :all]))
(fact "page-seq test"
      (def pages (partition 4 (range 20)))
      (defn get-page
        [n]
        (prn "get page:" n)
        (when (< n (count pages))
          {:cursor n
           :data (nth pages n)}))

      (->> (page-seq get-page 0 (comp inc :cursor))
           (take 1)
           count) => 1

      (->> (page-seq get-page 0 (comp inc :cursor) 200)
           count) => 5

      )


