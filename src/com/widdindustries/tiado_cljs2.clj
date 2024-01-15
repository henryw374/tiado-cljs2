(ns com.widdindustries.tiado-cljs2
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [shadow.cljs.devtools.api :as api]
            [shadow.cljs.devtools.server :as server]
            [shadow.cljs.devtools.server.npm-deps :as npm-deps]
            [shadow.cljs.build-report :as build-report]
            [clj-chrome-devtools.automation :as cdp-automation]
            [clj-chrome-devtools.automation.fixture :as cdp-fixture]
            [clj-chrome-devtools.automation.launcher :as cdp-launcher]
            [clj-chrome-devtools.commands.browser]
            [clj-chrome-devtools.commands.console :as console]
            [clj-chrome-devtools.core :as cdp-core]
            [clj-chrome-devtools.events :as events]
            [clj-chrome-devtools.impl.connection :as cdp-connection]
            [clojure.core.async :as async]
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
                                              :port  funnel-port}))]
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
                             :or   {watch-dir  "web-target/public"
                                    asset-path "/cljs-out"}}]
  {:build-id        :app-dev
   :target          :browser
   :output-dir      (str watch-dir asset-path)
   :closure-defines {}
   :devtools        {:watch-dir watch-dir
                     :preloads  (cond-> []
                                  (cljs-ns-available? 'devtools.preload)
                                  (conj 'devtools.preload))}
   :asset-path      asset-path})

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
  {:build-id         :browser-test-build
   :target           :browser-test
   :compiler-options {:data-readers true}
   :runner-ns        'kaocha.cljs2.shadow-runner
   :test-dir         "web-target/public/browser-test"
   :asset-path       "/browser-test/js"
   :build-options    {}
   :closure-defines  {'lambdaisland.funnel-client/FUNNEL_URI funnel-uri}
   :devtools         {:preloads (cond-> ['lambdaisland.chui.remote]
                                  (cljs-ns-available? 'devtools.preload)
                                  (conj 'devtools.preload))}})

(def compile-fns
  {:watch   watch
   :once    api/compile*
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

(defn with-chrome-session [f]
  #_(let [make-ws-delegate cdp-connection/make-ws-client]
      (with-redefs [cdp-connection/make-ws-client #(make-ws-delegate opts) cdp-launcher/launch-chrome launch-chrome]))
  (let [fixture (cdp-fixture/create-chrome-fixture {:headless?   true
                                                    :no-sandbox? true
                                                    ;:remote-debugging-port 0
                                                    ;"--remote-debugging-socket-fd=0"
                                                    ;"--no-sandbox" "--enable-logging=stderr" "--v=1"
                                                    })]
    (fixture
      (fn []
        (println "chrome launched")
        (let [{:keys [connection] :as automation} @cdp-automation/current-automation]
          (def automation automation)
          (cdp-core/set-current-connection! connection)
          (f automation))))))

(defn log-console-messages [f]
  (console/enable)
  (let [stop (events/listen :console :message-added
                 (fn [v]
                   (when-let [m (some->> v :params :message)]
                     (println "console" m))))]
    (try (f)
         (finally
           (println "Unregistering console logging")
           (stop)))))

(defn run-tests-headless [_suite-name]
  ;(chrome-headless/kill-all-chromes)
  (chrome-fixture
    (fn [session]
      (try
        #_(clj-chrome-devtools.commands.browser/grant-permissions
            ;this might be useful if tests needed to use the real clipboard
            {:origin      shadow-local
             :permissions ["clipboardReadWrite", "clipboardSanitizedWrite"]})
        (log-console-messages
          (fn []
            (println "connecting to " test-url)
            (cdp-automation/to session test-url)
            ;(Thread/sleep 50000)
            (println "running tests") ;suite-name
            (let [kaocha-options (if true ; (is-gitlab?)
                                   {:kaocha/plugins                      [:kaocha.plugin/junit-xml]
                                    :koocha.plugin.junit-xal/target-file (str (name _suite-name) ".xml")}
                                   {})]
              (try
                (kaocha.repl/run
                  :browser-tests
                  #:kaocha{:tests
                           [#:kaocha.testable{:id   :browser-tests
                                              :type :kaocha.type/cljs2}]})
                (catch Exception e
                  (println (.getMessage e)))))))
        (catch Throwable t
          (println " problem with chrome" (.getMessage t))))))
  )

(def *chrome-command* (cdp-launcher/find-chrome-binary))

(defn chrome-headless-proc []
  (process/process [*chrome-command* "--disable-gpu"
                    "--remote-debugging-socket-fd=0"
                    "--headless"
                    "--remote-debugging-port=53354"
                    "--no-sandbox" "--enable-logging=stderr" "--v=1"
                    ]
    {:out *out*
     :err *out*}))

(comment
  (start-server)
  (browser-test-build :once {})
  (run-tests-headless nil)
  (defonce chrome-fixture (cdp-fixture/create-chrome-fixture {:url-to-open (str shadow-local)}))
  (def a (clj-chrome-devtools.automation.launcher/launch-automation
           {:headless?   true
            :no-sandbox? true
            :idle-timeout 0
            ;:url-to-open (str shadow-local)
            }))
  (def c (clj-chrome-devtools.core/connect "localhost" 53354))
  (def x (chrome-headless-proc))
  (process/destroy x)
  )

#_(def ^:dynamic *chrome-command*
    (case (System/getProperty "os.name")
      "Mac OS X" "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
      "chrome"))

#_(defn chrome-headless-proc [url]
    (process/process [*chrome-command* "--disable-gpu"
                      "--remote-debugging-socket-fd=0"
                      "--headless"
                      "--remote-debugging-port=0"
                      "--no-sandbox" "--enable-logging=stderr" "--v=1"
                      url]
      {:out *out*
       :err *out*}))

#_(defn compile-and-run-tests-headless* []
    (let [proc (chrome-headless-proc test-url)]
      (Thread/sleep 2000)
      (try


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
  ;(browser-test-build :once {})
  (browser-test-build compile-mode {})
  (try
    (kaocha-exit-if-fail (run-tests-headless nil))
    (catch Exception e
      (println e)
      (System/exit 1))))

(defn build-report [build file-name]
  (build-report/generate build
    {:report-file file-name
     :inline      true}))

(defn show-npm-deps []
  (clojure.pprint/print-table (npm-deps/get-deps-from-classpath)))

(comment

  (tests-ci-shadow {:compile-mode :once})

  server/stop!

  (defn dev-http []
    (-> @shadow.cljs.devtools.server.runtime/instance-ref
        :dev-http deref :servers first))

  (-> (dev-http))

  (-> (dev-http) :instance :handler-state :managers
      second ;first 
      (.getResource "index.html"))
  )