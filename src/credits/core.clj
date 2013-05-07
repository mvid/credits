(ns credits.core
  (:require [credits.stinger :as stinger]
            [credits.router :as router]))

(defn -main [port]
  (router/start (Integer. port)))