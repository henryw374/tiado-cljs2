(ns com.widdindustries.vanilla-release-compile
  "fns to do non-shadow compile - handy for lib testing"
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

(defn html-to-serve-tests [build-name]
  (spit (str "web-target/" build-name "/index.html")
    (str "<!DOCTYPE html>
    <html><head>
    <title>kaocha.cljs2.shadow-runner</title>
    <meta charset=\"utf-8\">
    </head>
    <body>
    <script src=\"" build-name "/main.js"  "\"></script><script>kaocha.cljs2.shadow_runner.init();</script></body></html>")
    )
  )

(comment
  (build)
  )

