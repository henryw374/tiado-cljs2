(ns cljs
  (:require [util]
            [kaocha.repl]))

(defn test-watch []
  (util/browser-test-build :watch {}))

(comment

  (test-watch)
  (util/restart-server)
  (kaocha.repl/run
    :browser-tests
    #:kaocha{:tests
             [#:kaocha.testable{:id   :browser-tests
                                :type :kaocha.type/cljs2}]})

  )