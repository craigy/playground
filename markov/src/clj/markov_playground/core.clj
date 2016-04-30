(ns markov-playground.core
  (:require
    [boot.cli :as cli]
    [compojure.core :refer [defroutes routes ANY GET]]
    [compojure.route :refer [resources not-found]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.util.response :as r]
    [org.httpkit.server :as h]
    [markov.core :as m])
  (:gen-class))


(def index
  (->
    (r/resource-response "index.html" {:root "public"})
    (r/header "Content-Type" "text/html; charset=utf-8")))

(defn bfc [request]
  (str (m/build-from-coll [:a :b :c])))

(defn bfs [request]
  (str (m/build-from-string 2 "A B C A C A B")))

(defroutes http-routes
  (resources "/")
  (resources "/public")
  (resources "/" {:root "/META-INF/resources"})
  (ANY "/" [] index)
  (GET "/bfc" [] bfc)
  (GET "/bfs" [] bfs)
  (not-found "404"))

(def handler
  (-> (routes
        http-routes)
      wrap-params))

(defn init []
  (do
    (println "Initializing")))

(defn -main [& args]
  (let [port (if (seq args) (read-string (first args)) 3000)]
    (h/run-server handler {:port port})
    (println "Started server on port" port)))

