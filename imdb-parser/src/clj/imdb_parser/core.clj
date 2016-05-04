(ns imdb-parser.core
  (:require
    [boot.cli :as cli]
    [compojure.core :refer [defroutes routes ANY GET]]
    [compojure.route :refer [resources not-found]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.util.response :as r]
    [org.httpkit.server :as h])
  (:gen-class))


(def index
  (->
    (r/resource-response "index.html" {:root "public"})
    (r/header "Content-Type" "text/html; charset=utf-8")))

(defn count-lines [request]
  (with-open [rdr (clojure.java.io/reader "/home/craigy/Downloads/quotes.list")]
    (str (count (filter #(.startsWith % "#") (line-seq rdr))) " movies")))

(defroutes http-routes
  (resources "/")
  (resources "/public")
  (resources "/" {:root "/META-INF/resources"})
  (ANY "/" [] index)
  (GET "/count" [] count-lines)
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

