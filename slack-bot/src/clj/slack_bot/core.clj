(ns slack-bot.core
  (:require
    [boot.cli :as cli]
    [compojure.core :refer [defroutes routes ANY GET]]
    [compojure.route :refer [resources not-found]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.util.response :as r]
    [org.httpkit.server :as h]
    [clj-slack.users :as users]
    [clj-slack.channels :as channels])
  (:gen-class))


(def index
  (->
    (r/resource-response "index.html" {:root "public"})
    (r/header "Content-Type" "text/html; charset=utf-8")))


(defn get-user-names [users]
  (map
    #(get % :name)
    (:members users)))

(defn users-list [request]
  (let [token (:token (:params request))
        connection {:api-url "https://slack.com/api" :token token}
        users (users/list connection)]
    (clojure.pprint/write (get-user-names users) :pretty true :stream nil)))

(defn get-channel-names [channels]
  (map
    #(vector (get % :name) (get % :id))
    (:channels channels)))

(defn channel-list [request]
  (let [token (:token (:params request))
        connection {:api-url "https://slack.com/api" :token token}
        channels (channels/list connection)]
    (clojure.pprint/write (get-channel-names channels) :pretty true :stream nil)))

(defn get-history-text [history]
  (map
    #(get % :text)
    (:messages history)))

(defn channel-history [request]
  (let [token (:token (:params request))
        channel-id (:channel (:params request))
        connection {:api-url "https://slack.com/api" :token token}
        history (channels/history connection channel-id)]
    (clojure.pprint/write (get-history-text history) :pretty true :stream nil)))

(defroutes http-routes
  (resources "/")
  (resources "/public")
  (resources "/" {:root "/META-INF/resources"})
  (ANY "/" [] index)
  (GET "/users/list/:token" [] users-list)
  (GET "/channels/list/:token" [] channel-list)
  (GET "/channels/history/:channel/:token" [] channel-history)
  (not-found "404"))

(def handler
  (-> (routes
        http-routes)
      wrap-params))

(defn init
  ([]
    (do
      (println "Initializing with no token")))
  ([token]
    (do
      (println "Initializing with token" token))))

(defn -main [& args]
  (let [port (if (seq args) (read-string (first args)) 3000)]
    (h/run-server handler {:port port})
    (println "Started server on port" port)))

