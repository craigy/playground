(set-env!
 :source-paths #{"src/clj"}
 :resource-paths #{"resources"}

 :dependencies '[[org.clojure/clojure        "1.7.0"     :scope "provided"]
                 [org.clojure/clojurescript  "1.7.228"    :scope "provided"]
                 [adzerk/boot-cljs           "1.7.228-1" :scope "test"]
                 [pandeiro/boot-http         "0.7.3"     :scope "test"]
                 [adzerk/boot-reload         "0.4.7"     :scope "test"]
                 [adzerk/boot-cljs-repl      "0.3.0"     :scope "test"]
                 [com.cemerick/piggieback    "0.2.1"     :scope "test"]
                 [weasel                     "0.7.0"     :scope "test"]
                 [org.clojure/tools.nrepl    "0.2.12"    :scope "test"]
                 [jonase/eastwood            "0.2.1"     :scope "test" :exclusions [org.clojure/clojure]]
                 [boot/core                  "2.5.5"]
                 [ring/ring-core             "1.4.0"]
                 [ring/ring-jetty-adapter    "1.4.0"]
                 [http-kit                   "2.1.19"]
                 [compojure                  "1.5.0"]
                 [com.cognitect/transit-clj  "0.8.285"]
                 [com.cognitect/transit-cljs "0.8.237"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[eastwood.lint :refer :all])


(deftask dev []
  (comp
    (serve
      :httpkit true
      :handler 'imdb-parser.core/handler
      :reload true
      :init 'imdb-parser.core/init)
    (watch)
    (reload)))

