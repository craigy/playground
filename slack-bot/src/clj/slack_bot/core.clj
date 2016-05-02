(ns slack-bot.core
  (:require
    [boot.cli :as cli]
    [compojure.core :refer [defroutes routes ANY GET]]
    [compojure.route :refer [resources not-found]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.util.response :as r]
    [org.httpkit.server :as hk]
    [org.httpkit.client :as http]
    [clj-slack.users :as users]
    [clj-slack.channels :as channels]
    [http.async.client :as h]
    [clojure.data.json :as json])
  (:gen-class))


(defonce state (atom {}))

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

(defn get-websocket-url [token]
  (let [{:keys [body]}
        @(http/get
          "https://slack.com/api/rtm.start"
          {:query-params {:token token :no_unreads true}})]
    (let [json (json/read-str body :key-fn keyword)]
      (println (:channels json))
      (swap! state assoc :my-id (:id (:self json)))
      (:url json))))

(defn generate-response!
  ([m]
    (let [id (or (:id @state) 1)]
      (swap! state assoc :id (inc id))
      (json/write-str (merge m {:id id}))))
  ([channel text]
   (generate-response!
     {:type "message"
      :channel channel
      :text text})))

(defn handle-to-me [ws msg]
  (if (= (:user msg) (:master @state))
    (do
      (println "message from master to me:" msg)
      (let [response (generate-response! (:channel msg) "yes master")]
        (println "response:" response)
        (h/send ws :text response)))
    (do
      (println "message to me" msg)
      (h/send ws :text (generate-response! (:channel msg) "I don't know you")))))

(defn handle-message [ws json]
  (let [msg (json/read-str json :key-fn keyword)]
    (if (= "message" (:type msg))
      (if (.contains (:text msg) (str "<@" (:my-id @state) ">"))
        (handle-to-me ws msg)
        (println "message:" msg))
      (println "non-message:" msg))))

(defn on-open [ws]
  (println "open"))

(defn on-close [ws code reason]
  (println "close" code reason))

(defn on-error [ws e]
  (println "error" e))

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defn ping! []
  (when-let [ws (:ws @state)]
    (let [ping (generate-response! {:type "ping"})]
      (println "pinging:" ping)
      (h/send ws :text ping))))

(defn restart-ping! []
  (when-let [ping (:ping @state)]
    (future-cancel ping))
  (let [ping (set-interval ping! 10000)]
    (swap! state assoc :ping ping)))

(defn rtm-start
  ([request]
    (when-let [ws (:ws @state)]
      (h/close (:ws @state)))
    (let [token (:token (:params request))
          url (get-websocket-url token)
          client (h/create-client)
          ws (h/websocket client
               url
               :open on-open
               :close on-close
               :error on-error
               :byte handle-message
               :text handle-message)]
          (swap! state assoc :ws ws)
          (restart-ping!)
          "success")))

(defn set-master! [request]
  (let [mn (:name (:params request))]
    (swap! state assoc :master mn)
    (println "master set to" (:master @state))
    "success"))

(defroutes http-routes
  (resources "/")
  (resources "/public")
  (resources "/" {:root "/META-INF/resources"})
  (ANY "/" [] index)
  (GET "/users/list/:token" [] users-list)
  (GET "/channels/list/:token" [] channel-list)
  (GET "/channels/history/:channel/:token" [] channel-history)
  (GET "/rtm/start/:token" [] rtm-start)
  (GET "/master/:name" [] set-master!)
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
    (hk/run-server handler {:port port})
    (println "Started server on port" port)))

