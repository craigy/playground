(ns ip.core
  (:require
    [boot.cli :as cli]
    [compojure.core :refer [defroutes routes ANY GET]]
    [compojure.route :refer [resources not-found]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.util.response :as r]
    [org.httpkit.server :as h]
    [instaparse.core :as insta])
  (:gen-class))


(defonce state (atom {}))


(def index
  (->
    (r/resource-response "index.html" {:root "public"})
    (r/header "Content-Type" "text/html; charset=utf-8")))

(def as-and-bs
  (insta/parser
    "S = AB*
     AB = A B
     A = 'a'+
     B = 'b'+"))

(defn ab [request]
  (str (as-and-bs "aaaaabbbaaaabb")))

(def number
  (insta/parser
    "S = n
     n = #'[0-9]+'"))

(defn numb [request]
  (str (number "1")))

(defroutes http-routes
  (resources "/")
  (resources "/public")
  (resources "/" {:root "/META-INF/resources"})
  (ANY "/" [] index)
  (GET "/ab" [] ab)
  (GET "/num" [] numb)
  (not-found "404"))

(def handler
  (-> (routes
        http-routes)
      wrap-params))

(defn init [])

(defn -main [& args]
  (let [port (if (seq args) (read-string (first args)) 3000)]
    (h/run-server handler {:port port})
    (println "Started server on port" port)))

