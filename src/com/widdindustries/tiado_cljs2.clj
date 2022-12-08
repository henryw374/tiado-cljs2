(ns com.widdindustries.tiado-cljs2
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [shadow.cljs.devtools.api :as api]
            [shadow.cljs.devtools.server :as server]
            [shadow.cljs.devtools.server.npm-deps :as npm-deps]
            [shadow.cljs.build-report :as build-report]
    ;[clojure.java.shell :as sh]
            [babashka.process :as process]
            [lambdaisland.funnel :as funnel]
            [shadow.cljs.devtools.config :as config]
            [kaocha.repl]
            [clojure.string :as string]))

(def dev-server-port 9000)
(def funnel-port 9010)
(def funnel-uri (str "ws://localhost:" funnel-port))
(def shadow-local (str "http://localhost:" dev-server-port))
(def test-url (str shadow-local "/browser-test/index.html"))

(defonce funnel-server nil)

(defn stop-funnel []
  (when funnel-server
    (.close funnel-server)
    (alter-var-root #'funnel-server (constantly nil))))

(defn start-funnel []
  (System/setProperty "lambdaisland.funnel.uri" funnel-uri)
  (if (and funnel-server @funnel-server)
    nil
    (when-let [server (funnel/start-server (funnel/websocket-server
                                             {:state (atom {})
                                              :port funnel-port}))]
      (funnel/init-logging 1 nil)
      (alter-var-root #'funnel-server (constantly server)))))

(def server-config
  (merge config/default-config
    {:deps     {:aliases [:client :dev]}
     :http     {:port 9020}
     :dev-http {dev-server-port
                {:root "classpath:public" :use-index-files true}}
     }))

(defn npm-i 
  "the call to shadow `npm-deps/main` installs deps.cljs dependencies iff they are not already in package.json. 
  
  If the version in package.json is different to deps.cljs, the package.json version remains
  untouched. This is the normal shadow-cljs behaviour.
  
  Additionally `npm i` is called to install anything else in package.json but not in the classpath
  "
  [opts]
  (npm-deps/main opts {})
  @(process/process ["npm" "i"]))

(defn stop-server []
  (stop-funnel)
  (server/stop!))

(defn start-server
  ([] (start-server {}))
  ([opts]
   (let [server-config (merge server-config opts)]
     (npm-i server-config)
     (server/start! server-config)
     (start-funnel))))

(defn restart-server
  ([] (restart-server {}))
  ([opts]
   (stop-server)
   (start-server opts)))

(defn cljs-ns-available? [ns-sym]
  (-> ns-sym
      str
      (string/replace "." "/")
      (string/replace "-" "_")
      (str ".cljs")
      (io/resource)))

(defn browser-app-config [& {:keys [watch-dir asset-path]
                             :or {watch-dir "web-target/public"
                                  asset-path "/cljs-out"}}]
  {:build-id        :app-dev
   :target          :browser
   :output-dir      (str watch-dir asset-path)
   :closure-defines {}
   :devtools        {:watch-dir watch-dir
                     :preloads  (cond-> []
                                  (cljs-ns-available? 'devtools.preload)
                                  (conj 'devtools.preload))}
   :asset-path asset-path})

(defn clean-dir [d]
  (process/check (process/process ["rm" "-rf" d])))

(defn clean-npm []
  (clean-dir "node_modules"))

(defn clean-build
  ([] (clean-build ""))
  ([path]
   (clean-dir (str "web-target/public/" path))))

(defn clean-shadow-cache []
  (clean-dir ".shadow-cljs/builds"))

(defn prod-build
  ([build]
   (prod-build build {:source-maps true}))
  ([build opts]
   (start-server opts)
   (api/stop-worker :app-dev)
   (clean-build (name (get build :build-id build)))
   (api/release* build opts)
   nil))

(defn watch
  ([build] (watch build {}))
  ([build opts]
   (start-server opts)
   (api/stop-worker (get build :build-id build))
   (clean-build (name (get build :build-id build)))
   (api/watch build (merge {:verbose false} opts))))

(defn stop-watch [build]
  (api/stop-worker (get build :build-id build)))

(defn repl
  ([] (repl :app-dev))
  ([build-name]
   (api/repl build-name)))

(defn browser-test-config []
  {:build-id :browser-test-build
   :target :browser-test
   :compiler-options {:data-readers true}
   :runner-ns 'kaocha.cljs2.shadow-runner
   :test-dir "web-target/public/browser-test"
   :asset-path "/browser-test/js"
   :build-options {}
   :closure-defines {'lambdaisland.funnel-client/FUNNEL_URI funnel-uri}
   :devtools {:preloads (cond-> ['lambdaisland.chui.remote ]
                          (cljs-ns-available? 'devtools.preload)
                          (conj 'devtools.preload))}})

(def compile-fns
  {:watch watch
   :once api/compile*
   :release api/release*})

(defn browser-test-build [compile-mode opts]
  (.mkdirs (io/file "web-target" "public" "browser-test"))
  (spit "web-target/public/browser-test/index.html"
    "<!DOCTYPE html>
    <html><head>
    <title>kaocha.cljs2.shadow-runner</title>
    <meta charset=\"utf-8\">
    </head>
    <body>
    <script src=\"/browser-test/js/test.js\"></script><script>kaocha.cljs2.shadow_runner.init();</script></body></html>"
    )
  ((get compile-fns compile-mode)
   (browser-test-config) opts)
  (println "for tests, open " test-url))

(defn run-tests []
  (let [result-fut
        (future
          (kaocha.repl/run
            :browser-tests
            #:kaocha{:tests
                     [#:kaocha.testable{:id   :browser-tests
                                        :type :kaocha.type/cljs2}]}))
        result (deref result-fut 10000 ::timeout)]
    (if (= ::timeout result)
      (throw (Exception. "timeout waiting for test result"))
      result)))

(def ^:dynamic *chrome-command*
  (case (System/getProperty "os.name")
    "Mac OS X" "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    "chrome"))

(defn chrome-headless-proc [url]
  (process/process [*chrome-command* "--disable-gpu"
               "--remote-debugging-socket-fd=0"
               "--headless" 
               "--remote-debugging-port=0"
               "--no-sandbox" "--enable-logging=stderr" "--v=1"
               url]
    {:out *out*
     :err *out*}))

(defn compile-and-run-tests-headless* []
  (let [proc (chrome-headless-proc test-url)]
    (Thread/sleep 2000)
    (try
      (run-tests)
      (finally
        ;(println "killing pid " (.pid chrome))
        (process/destroy proc)
        ))))

(defn kaocha-exit-if-fail [result]
  (if (or (some pos? ((juxt :kaocha.result/error :kaocha.result/fail :kaocha.result/pending)
                      result)))
    (System/exit 1)
    (System/exit 0)))

(defn tests-ci-shadow [{:keys [compile-mode]}]
  (start-server)
  (browser-test-build compile-mode {})
  (try
    (kaocha-exit-if-fail (compile-and-run-tests-headless*))
    (catch Exception e 
      (println e)
      (System/exit 1))))

(defn build-report [build file-name]
  (build-report/generate build
    {:report-file file-name
     :inline true}))

(defn show-npm-deps []
  (clojure.pprint/print-table (npm-deps/get-deps-from-classpath)))

(comment 
  
  server/stop!
  
  (defn dev-http []
    (-> @shadow.cljs.devtools.server.runtime/instance-ref
        :dev-http deref :servers first))
  
  (-> (dev-http))

  (-> (dev-http) :instance :handler-state :managers
      second ;first 
      (.getResource "index.html"))
  )














