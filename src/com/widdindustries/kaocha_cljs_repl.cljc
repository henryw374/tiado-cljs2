(ns com.widdindustries.kaocha-cljs-repl
  #?(:clj (:require [com.widdindustries.tiado-cljs2 :as util])
     :cljs (:require-macros [com.widdindustries.kaocha-cljs-repl :refer [run-all]])))

(defmacro run-all []
  (util/run-tests))
