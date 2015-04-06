(ns flow.datetime)

(defn now [] (js/Date.))
(defn now-ms [] (.getTime (js/Date.)))

(defn pad-date [i]
  (if (< i 10)
    (str "0" i)
    (str i)))

(defn year [dt] (.getFullYear dt))
(defn year-short [dt] (.getYear dt))
(defn month [dt] (.getMonth dt))
(defn day-of-month [dt] (.getDate dt))
(defn day-of-week [dt] (.getDay dt))
(defn hours [dt] (.getHours dt))
(defn minutes [dt] (.getMinutes dt))
(defn seconds [dt] (.getSeconds dt))
(defn ms [dt] (.getMilliseconds dt))

(defn get-month-name
  ([month] (get-month-name month true))
  ([month capitalize]
     (if capitalize
       (get ["January" "February" "March" "April" "May" "June" "July"
             "August" "September" "October" "November" "December"] month)
       (get ["january" "february" "march" "april" "may" "june" "july"
             "august" "september" "october" "november" "december"] month))))

(defn get-month-name-short
  ([month] (get-month-name-short month true))
  ([month capitalize]
     (if capitalize
       (get ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
             "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"] month)
       (get ["jan" "feb" "mar" "apr" "may" "jun"
             "jul" "aug" "sep" "oct" "nov" "dec"] month))))

(defn get-weekday-name
  ([weekday] (get-weekday-name weekday true))
  ([weekday capitalize]
     (if capitalize
       (get ["Sunday" "Monday" "Tuesday" "Wednesday"
             "Thursday" "Friday" "Saturday"] weekday)
       (get ["sunday" "monday" "tuesday" "wednesday"
             "thursday" "friday" "saturday"] weekday))))

(defn get-weekday-name-short
  ([weekday] (get-weekday-name-short weekday true))
  ([weekday capitalize]
     (if capitalize
       (get ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"] weekday)
       (get ["sun" "mon" "tue" "wed" "thu" "fri" "sat"] weekday))))

(defn ms->display [ms seconds]
  (let [h (int (/ ms 3600000))
        rem (- ms (* h 3600000))
        m (int (/ rem 60000))
        s (int (/ (- rem (* m 60000)) 1000))]
    (str (pad-date h) ":" (pad-date m) (when seconds (str ":" (pad-date s))))))

(defn date-display
  ([d] (date-display d true))
  ([d day]
     (str (.getFullYear d) "/"
          (pad-date (inc (.getMonth d)))
          (when day (str "/" (pad-date (.getDate d)))))))

(defn time-display
  ([t] (time-display t false))
  ([t seconds]
     (str (pad-date (.getHours t)) ":"
          (pad-date (.getMinutes t))
          (when seconds (str ":" (pad-date (.getSeconds t)))))))

(defn add-days [dt days]
  (let [target (js/Date. (.valueOf dt))]
    (.setDate target (+ (.getDate target) days))
    target))

(defn get-date-for-nearest-weekday
  "Supply a datetime and a weekday (0 for Sunday through 6 for
  Saturday) and get back the day of month for that weekday which is
  closest to the datetime. Note that in the calculation inside, Sunday
  is given the value 7 instead of 0."
  [t weekday]
  (let [day-of-week (day-of-week t)]
    (+ weekday
       (- (day-of-month t)
          (if (zero? day-of-week) 7 day-of-week)))))

(defn get-week-no [t]
  (let [ref (js/Date. t)
        year-start (js/Date. (year t) 0 1)]
    (.setHours ref 0 0 0)
    (.setDate ref (get-date-for-nearest-weekday t 4))
    (.ceil js/Math (-> (- ref year-start)
                       (/ 86400000)
                       inc
                       (/ 7)
                       pad-date))))

(defn daysuf [date]
  (str date (cond
              (some #{1 21 31} [date]) "st"
              (some #{2 22} [date]) "nd"
              (some #{3 23} [date]) "rd"
              :else "th")))
