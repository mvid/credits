(ns credits.test.core
  (:use [credits.core]
        [clojure.test]
        [credits.stinger]))



(deftest lookup-movie-status
  (= true (stinger? "The Avengers")))