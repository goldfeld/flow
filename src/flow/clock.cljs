(ns flow.clock)

(defn now [] (js/Date.))
(defn now-ms [] (.getTime (js/Date.)))

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

(defn pad-date [i]
  (if (< i 10)
    (str "0" i)
    (str i)))

(defn format-date
  ([d] (format-date d true))
  ([d day]
     (str (.getFullYear d) "/"
          (pad-date (inc (.getMonth d)))
          (when day (str "/" (pad-date (.getDate d)))))))

(defn format-time
  ([t] (format-time t false))
  ([t seconds]
     (str (pad-date (.getHours t)) ":"
          (pad-date (.getMinutes t))
          (when seconds (str ":" (pad-date (.getSeconds t)))))))

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
                  (display handle (format-time (js/Date.) seconds))
                  (render))
                seconds)))

(defn ms->display [ms seconds]
  (let [h (int (/ ms 3600000))
        rem (- ms (* h 3600000))
        m (int (/ rem 60000))
        s (int (/ (- rem (* m 60000)) 1000))]
    (str (pad-date h) ":" (pad-date m) (when seconds (str ":" (pad-date s))))))

(defn set-timer
  ([name target label os-fns] (set-timer name target label os-fns false))
  ([name target label os-fns seconds]
     (set-clock name
                (fn [handle]
                  (let [left (- target (now-ms))
                        show (ms->display left seconds)
                        prefix (when (count label) (str label " "))
                        execute-alert (:execute-alert os-fns)]
                    (when (and (< left 10000) (= (rem (quot left 1000) 3) 1))
                      (execute-alert))
                    ((:set-clock-content os-fns) handle (str prefix show)))
                  ((:render-clock os-fns)))
                seconds)))
