(ns com.widdindustries.shadow-info
  (:require [shadow.cljs.devtools.server.runtime :as runtime]
            [shadow.cljs.devtools.server.npm-deps :as npm-deps]))

(defn running-config []
  (:config (runtime/get-instance!)))

(defn show-npm-deps []
  (clojure.pprint/print-table (sort-by :id (npm-deps/get-deps-from-classpath))))
