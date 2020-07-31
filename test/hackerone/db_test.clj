(ns hackerone.db-test
  (:require [hackerone.db :refer :all]
            [midje.sweet :refer :all]))

(fact "make id"
      (make-id "test" "id") => :test/id
      (make-id :test :id) => :test/id
      (make-id "ids.test" :obj) :ids.test/obj
      )

(fact "submap?"
      (submap? {:test 1} {:entity/type :test :test 1}) => true
      (submap? {:test 1} {:test 1}) => true
      (submap? {} {:entity/type :test}) => true
      )
