(ns com.widdindustries.vanilla-release-compile
  "WIP - doesnt work yet
  
  fns to do non-shadow compilation - handy for lib testing"
  (:require [cljs.build.api :as cljs]))

(defn browse []
  (clojure.java.browse/browse-url "http://localhost:8000"))

(def debug-opts
  {:pseudo-names true
   :pretty-print true})

; like this? https://github.com/lambdaisland/kaocha-cljs2-demo/blob/main/repl_sessions/build_api.clj
(defn build [build-name opts]
  (cljs/build
    (->
      {:optimizations :advanced
       :infer-externs true
       ;:verbose true
       :process-shim  false
       :output-dir    (str "web-target/" build-name)
       :output-to     (str "web-target/" build-name "/main.js")}
      (merge opts))))

(comment
  (build)
  )

