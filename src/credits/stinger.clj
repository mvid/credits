(ns credits.stinger
  (:require [clj-http.client :as client]
            [ring.util.codec :as codec]
            [clojurewerkz.spyglass.client :as memcache]
            [clj-json.core :as json])
  (:use [clojure.tools.logging :only (debug info error)]))

(def category-ids
  {:stinger 7
   :non-stinger 6
   :coming-soon 1633
   :during-and-after-credits 16
   :confirmed 12})
(def month-in-seconds 2592000)

(defn address [^String title]
  (str "http://aftercredits.com/?json=1&s=" (codec/url-encode
                                              (str "\"" title "\""))))

(defn search-results [^String title]
  (:posts (:body (client/get (address title) {:as :json}))))

(defn now-showing [^String title]
  (let [results (search-results title)]
    (filter
      (fn [film] (some #{(category-ids :stinger)
                         (category-ids :non-stinger)}
                   (map :id (:categories film))))
      results)))

(defn film-from-title [^String title]
  (let [tmc (memcache/text-connection (or (System/getenv "MEMCACHIER_SERVERS") ""))
        cached-film (if tmc (try
                              (memcache/get tmc (codec/url-encode title))
                              (catch Exception e
                                (do
                                  (error (str "error reading from memcache") e)
                                  nil))) nil)]
    (if cached-film
      ; deserialize, create appropriate response
      (let [deserialized (json/parse-string cached-film)
            film {:title (get deserialized "title")
                  :url (get deserialized "url")
                  :categories (get deserialized "categories")}]
        film)
      ; retrieve film, store to memcache for a month
      (let [films (now-showing title)
            film (first films)]
        (if film
          (let [response {:title title
                          :url (film :url)
                          :categories (map :id (:categories film))}]
            (do
              (memcache/set
                tmc
                (codec/url-encode title)
                month-in-seconds
                (json/generate-string response))
              response))
          nil)))))

;(defn film-from-title [^String title]
;  (let [films (now-showing title)
;        film (first films)]
;    (first films)))

(defn stinger? [film]
  (if film
    (boolean (some #{(category-ids :stinger )} (:categories film)))
    nil))

(defn stinger-test []
  (do
    (println (stinger? (film-from-title "The Grey")))
    "OK"))