(ns credits.redis
  (:require [redis.core :as redis])
  (:import java.net.URI))

; 0 - foursquare user id : foursquare ouath token
; 1 - movie title : movie json object

(defn redis-uri-to-options [uri]
  (let [host (.getHost uri)
        port (.getPort uri)
        user-pass (.getUserInfo uri)
        pass (and user-pass (last (clojure.string/split user-pass #":")))
        options {:host host}]
    (merge {:host host}
      (if (> port 0) {:port port} {})
      (if pass {:password pass}))))

(def redis-opts (redis-uri-to-options (new URI (or (System/getenv "REDISTOGO_URL") ""))))

(defn retrieve [key db]
  (redis/with-server (merge redis-opts {:db db})
    (redis/get key)))

(defn store [key value db]
  (redis/with-server (merge redis-opts {:db db})
    (redis/set key value)))

(defn redis-test []
  (redis/with-server redis-opts
    (str (count (redis/keys "*")))))