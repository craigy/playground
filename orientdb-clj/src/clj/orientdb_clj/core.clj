(ns orientdb-clj.core
  (:require
    [boot.cli :as cli]
    [compojure.core :refer [defroutes routes ANY GET]]
    [compojure.route :refer [resources not-found]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.util.response :as r]
    [org.httpkit.server :as h])
  (:import
    (com.tinkerpop.blueprints.impls.orient OrientGraph)
    (com.orientechnologies.orient.core.db.document ODatabaseDocumentTx)
    (com.orientechnologies.orient.core.record.impl ODocument)
    (com.orientechnologies.orient.client.remote OServerAdmin)
    (com.orientechnologies.orient.server OServerMain))
  (:gen-class))


(defonce state (atom {}))


(def index
  (->
    (r/resource-response "index.html" {:root "public"})
    (r/header "Content-Type" "text/html; charset=utf-8")))

(defn connect! [location username password]
  (-> (OServerAdmin. location) (.connect username password)))

(defn connect-guest! [request]
  (println request)
  (let [o (connect! "localhost" "guest" "guest")]
    (str (.listDatabases o))))

(defn create-document! [request]
  (let [db (-> (ODatabaseDocumentTx. "remote:localhost/petshop") (.open "admin" "admin"))
        doc (-> (ODocument. "Person"))]
    (.field doc "name" "Luke")
    (.field doc "surname" "Skywalker")
    (.field doc "city" (-> (.ODocument "City") (.field "name" "Rome") (.field "country" "Italy")))))

(defn create-graph! [request]
  (let [graph (-> (OrientGraph. "memory:test", "admin", "admin"))
        v (.addVertex graph nil)
        id (.getId v)]
    (.shutdown graph)
    (str id)))

(defn create-server!
  ([request]
    (create-server!))
  ([]
    (let [server (OServerMain/create)]
      (.startup server)
      (.activate server)
      (swap! state assoc :server server)
      "success")))

(defroutes http-routes
  (resources "/")
  (resources "/public")
  (resources "/" {:root "/META-INF/resources"})
  (ANY "/" [] index)
  (GET "/connect" [] connect-guest!)
  (GET "/document" [] create-document!)
  (GET "/graph" [] create-graph!)
  (GET "/server" [] create-server!)
  (not-found "404"))

(def handler
  (-> (routes
        http-routes)
      wrap-params))

(defn init []
  (do
    (println "Starting orientdb-clj server")
    (create-server!)
    (println "Starting orientdb-clj client")
    (connect! "localhost" "guest" "guest")))

(defn -main [& args]
  (let [port (if (seq args) (read-string (first args)) 3000)]
    (h/run-server handler {:port port})
    (println "Started server on port" port)))

