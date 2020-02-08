(ns hackerone.core-test
  (:require [midje.sweet :refer :all]
            [hackerone.core :refer :all]))


(fact
 (->graphql-exp (:eq 3 2)) =>  {2 {"_eq" 3}}

 (->graphql-exp (eq "open" "submission_state")) => {"submission_state" {"_eq" "open"}}

 (->graphql-exp (is_null false "id" "external_program")) =>
 {"external_program" {"id" {"_is_null" false}}}

 (->graphql-exp (and (:neq "id" "5") (or (:is_null false "id")(:eq 3 2)))) =>
 {"_and" [{"5" {"_neq" "id"}} {"_or" [{"id" {"_is_null" false}} {2 {"_eq" 3}}]}]}

 (->graphql-exp (and (or (eq "open" "submission_state")
                         (eq "api_only" "submission_state")
                         (is_null false "id" "external_program"))
                     (is_null true "id" "external_program")
                     (or (and (neq "sandboxed" "state")
                              (neq "soft_launched" "state"))
                         (is_null false "id" "external_program")))) =>
 {"_and" [{"_or" [{"submission_state" {"_eq" "open"}}
                   {"submission_state" {"_eq" "api_only"}}
                   {"external_program" {"id" {"_is_null" false}}}]}
           {"external_program" {"id" {"_is_null" true}}}
           {"_or" [{"_and" [{"state" {"_neq" "sandboxed"}}
                            {"state" {"_neq" "soft_launched"}}]}
                   {"external_program" {"id" {"_is_null" false}}}]}]}

 )
