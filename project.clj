(defproject flow "0.1.4"
  :description "Clocks, timers and workflows in Clojure(Script)"
  :url "http://github.com/goldfeld/flow"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-npm "0.5.0"]
            [com.cemerick/clojurescript.test "0.3.3"]]
  :test-paths ["test"]
  :aliases {"cleantest" ["do" "clean," "cljsbuild" "once," "test,"]
            "autotest" ["do" "clean," "cljsbuild" "auto" "test"]}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3165"]
                 [prismatic/schema "0.4.0"]]
  :node-dependencies [[phantomjs "1.9.x"]]
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test"]
                        :notify-command ["phantomjs" :cljs.test/runner
                                         "target/flow_test.js"]
                        :compiler {:output-to "target/flow_test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :test-commands {"unit-tests" ["phantomjs" :runner
                                            "target/flow_test.js"]}}
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.0"]
                                  [org.clojure/tools.nrepl "0.2.10"]]
                   :repl-options {:nrepl-middleware
                                  [cemerick.piggieback/wrap-cljs-repl]}}})
