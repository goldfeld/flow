(ns flow.clock
  (:require #_[cljs.core.typed :as t :include-macros true]
            [flow.datetime :as dt]))

#_(t/defalias Clock
  t/Any)

#_(t/defalias Timer
  t/Any)

(def *clocks* (atom {}))

(defn create-clock
  ([positioning create-fn name]
     (let [clock {:ticker nil :handle (create-clock positioning create-fn)}]
       (swap! *clocks* assoc name clock)
       clock))
  ([positioning create-fn] (create-fn (conj positioning {:content "88:88"}))))

(defn set-ticker [name ticker]
  (swap! *clocks* assoc-in [name :ticker] ticker))

(defn get-clock [name]
  (@*clocks* name))

(defn set-clock
  ([name set-clock-fn] (set-clock name set-clock-fn false))
  ([name set-clock-fn seconds]
     (let [{handle :handle ticker :ticker} (get-clock name)]
       (set-clock-fn handle)
       (js/clearInterval ticker)
       (set-ticker name (js/setInterval #(set-clock-fn handle)
                                        (if seconds 1000 60000))))))

(defn set-current-time
  ([name os-fns] (set-current-time name os-fns false))
  ([name {display :set-clock-content render :render-clock} seconds]
     (set-clock name
                (fn [handle]
                  (display handle (dt/time-display (js/Date.) seconds))
                  (render))
                seconds)))

(defn set-timer
  ([name target label os-fns] (set-timer name target label os-fns false))
  ([name target label os-fns seconds]
     (set-clock name
                (fn [handle]
                  (let [left (- target (dt/now-ms))
                        show (dt/ms->display left seconds)
                        prefix (when (count label) (str label " "))
                        execute-alert (:execute-alert os-fns)]
                    (when (and (< left 10000) (= (rem (quot left 1000) 3) 1))
                      (execute-alert))
                    ((:set-clock-content os-fns) handle (str prefix show)))
                  ((:render-clock os-fns)))
                seconds)))
