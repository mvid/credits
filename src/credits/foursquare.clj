(ns credits.foursquare
  (:require [clj-http.client :as client]
            [clj-json.core :as json]
            [credits.redis :as redis]
            [credits.stinger :as stinger])
  (:use [clojure.tools.logging :only (debug info error)]))

(def client-id (System/getenv "FSQ_CLIENT_ID"))
(def client-secret (System/getenv "FSQ_CLIENT_SECRET"))
(def push-secret (System/getenv "FSQ_PUSH_SECRET"))
(def redirect-uri (System/getenv "FSQ_REDIRECT_URI"))

(defn token-from-id [^String user-id]
  (redis/retrieve user-id 0))

(defn event-from-checkin [checkin token]
  (if (contains? checkin "event")
    (try
      (let [event-id (get (get checkin "event") "id")
            response (client/get (str "https://api.foursquare.com/v2/events/" event-id)
          {:throw-entire-message? true
           :as :json
           :query-params {"oauth_token" token}})]
        (:event (:response (:body response))))
      (catch Exception e
        (error (str "Unable to retrieve event " (get checkin "event") " for token " token) e)))))

(defn event-is-movie? [event]
  (let [categories (:categories event)]
    (>
      (count (filter
               (fn [c] (= "Movies" (:name c)))
               categories))
      0)))

(defn reply-on-checkin [opts]
  (let [params {"oauth_token" (:token opts)
                "text" (:text opts)}]
    (try
      (client/post (str "https://api.foursquare.com/v2/checkins/" (:id opts) "/reply")
        {:throw-entire-message? true
         :as :json
         :query-params (if (:url opts)
                         (merge {"url" (:url opts)} params)
                         params)})
      (catch Exception e
        (error (str "Unable to comment on checkin with opts " opts) e)))))

(defn handle-checkin [push-body]
  {:pre [(= (:secret push-body) push-secret)]}
  (let [checkin (json/parse-string (:checkin push-body))
        checkin-id (get checkin "id")
        user-id (get (get checkin "user") "id")
        event (event-from-checkin checkin (token-from-id user-id))
        name (:name event)]
    (if (and event (event-is-movie? event))
      (do
        (info (str "id: " user-id " name: " name))
        (let [film (stinger/film-from-title name)
              token (token-from-id user-id)]
          (if (nil? film)
            (reply-on-checkin {:id checkin-id
                               :name name
                               :text "Sorry, we don't have info on this movie. Submit a report?"
                               :url "http://aftercredits.com/contact/"
                               :token token})
            (if (stinger/stinger? film)
              (reply-on-checkin {:id checkin-id
                                 :name name
                                 :text "Stick around, this movie should have something after the credits"
                                 :url (film :url )
                                 :token token})
              (reply-on-checkin {:id checkin-id
                                 :name name
                                 :text "Doesn't look like there is anything after this movie"
                                 :url (film :url )
                                 :token token}))))
        (str "OK"))
      (str "NOK"))))

(defn store-user [^String user-id ^String access-token]
  (redis/store user-id access-token 0))

(defn access-token-from-code [^String code]
  (client/get "https://foursquare.com/oauth2/access_token"
    {:throw-entire-message? true
     :as :json
     :query-params {"client_id" client-id
                    "client_secret" client-secret
                    "grant_type" "authorization_code"
                    "redirect_uri" redirect-uri
                    "code" code}}))

(defn code-to-token [^String code]
  (let [response (access-token-from-code code)]
    (:access_token (:body response))))

(defn user-profile [^String token]
  {:post [(contains? % :user )]}
  (let [response (client/get "https://api.foursquare.com/v2/users/self"
    {:throw-entire-message? true
     :as :json
     :query-params {"oauth_token" token}})]
    (:response (:body response))))

(defn callback [params]
  (try (let [token (code-to-token (:code params))
             user-id (:id (:user (user-profile token)))
             stored? (store-user user-id token)]
         nil)
    (catch Exception e
      (error (str "Error with foursquare o-auth") e)
      "Error communicating with foursquare. Please try again later.")))