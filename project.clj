(defproject flow "0.1.5"
  :description "Clocks, timers and workflows in Clojure(Script)"
  :url "http://github.com/goldfeld/flow"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-typed "0.3.5"]
            [com.cemerick/clojurescript.test "0.3.3"]]
  :test-paths ["test"]
  :aliases {"cleantest" ["do" "clean," "typed" "check-cljs,"
                         "cljsbuild" "once," "test,"]
            "autotest" ["do" "clean," "typed" "check-cljs,"
                        "cljsbuild" "auto" "test"]}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3165"]
                 [org.clojure/core.typed "0.2.84"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  ;;:core.typed {:check-cljs [flow.core flow.clock flow.datetime]}
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/flow_test.js"
                                   :optimizations :simple
                                   :pretty-print true}}]
              :test-commands {"unit-tests" ["node" :node-runner
                                            "target/flow_test.js"]}}
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.0"]
                                  [org.clojure/tools.nrepl "0.2.10"]]
                   :repl-options {:nrepl-middleware
                                  [cemerick.piggieback/wrap-cljs-repl]}}})
