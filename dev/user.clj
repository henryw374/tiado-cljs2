(ns user)

(defn dev []
  (require 'compilation)
  (eval '(compilation/start-live-compilation))
  )

;(dev)
