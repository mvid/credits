(ns credits.router
  (:use [compojure.core :only [defroutes GET POST]]
        [compojure.handler :only [api]])
  (:require [ring.adapter.jetty :as ring]
            [credits.foursquare :as foursquare]
            [credits.redis :as redis]
            [credits.stinger :as stinger]))

(def privacy "No data is stored, except for your foursquare user id on authentication.")

(def home "The After Credits App: Warns you when you check in to a movie with something after the credits.
  <a href=https://foursquare.com/oauth2/authenticate?client_id=TOW5VTUUKO44O2PUNK04VQLLREF4BZKINYGAYOT1VPB3HTUE&response_type=code&redirect_uri=http://credits.facelessmegacorp.com/foursquare/redirect>Authenticate 4sq</a>")


(defn response [title message]
  (str "<html>
<head>
  <title>After Credits - All Set!</title>
  <style>
    body {
      background-color: #faf9f7;
      font-family:\"Helvetica Neue\",Helvetica,Arial,sans-serif;
    }
    h1 {
      padding-top: 20px;
      text-align: center;
    }
    .container {
      padding: 0 20px;
      max-width: 500px;
      margin-left: auto;
      margin-right: auto;
    }
    .message {
      text-align: center;
    }
  </style>
  <meta content=\"width=320\" name=\"viewport\" />
</head>
<body>
<h1>" title "</h1>
<div class=\"container\">
<p class=\"message\">" message "</p>
</div>
</body>
</html>"))

(defroutes routes
  (GET "/foursquare/redirect" {params :params}
    (let [message (foursquare/callback params)
          title (if message "Error" "All Set")]
      (response title (or message "Now go watch some movies!"))))
  (POST "/foursquare/" {params :params}
    (foursquare/handle-checkin params))
  (GET "/test" {params :params}
    (redis/redis-test))
  (GET "/stinger-test" {params :params}
    (stinger/stinger-test))
  (GET "/privacy" {params :params}
    privacy)
  (GET "/" {params :params}
    home))

(def handler (api routes))

(defn start [port]
  (ring/run-jetty #'handler {:port port
                             :join? false}))
