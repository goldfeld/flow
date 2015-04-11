(ns flow.datetime-test
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]])
  (:require [flow.datetime :as dt]
            [cemerick.cljs.test]))

(deftest datetime-components-test
  (let [t (dt/now)]
    #_(is (s/validate s/Int (dt/year t)))
    #_(is (s/validate s/Int (dt/year-short t)))
    (is (and (< (dt/month t) 12) (> (dt/month t) -1)))
    (is (and (< (dt/day-of-month t) 32) (> (dt/day-of-month t) 0)))
    (is (and (< (dt/day-of-week t) 7) (> (dt/day-of-week t) -1)))
    (is (and (< (dt/hours t) 23) (> (dt/hours t) -1)))
    (is (and (< (dt/minutes t) 60) (> (dt/minutes t) -1)))
    (is (and (< (dt/seconds t) 60) (> (dt/seconds t) -1)))
    #_(is (s/validate s/Int (dt/ms t)))))

(deftest datetime-names-test
  (let [t (dt/now)
        m (dt/month t)]
    (is (some #{"January" "February" "March" "April" "May" "June" "July"
                "August" "September" "October" "November" "December"}
              [(dt/get-month-name m true)]))
    (is (some #{"january" "february" "march" "april" "may" "june" "july"
                "august" "september" "october" "november" "december"}
              [(dt/get-month-name m false)]))
    (is (some #{"Jan" "Feb" "Mar" "Apr" "May" "Jun"
                "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"}
              [(dt/get-month-name-short m true)]))
    (is (some #{"jan" "feb" "mar" "apr" "may" "jun"
                "jul" "aug" "sep" "oct" "nov" "dec"}
              [(dt/get-month-name-short m false)]))
    (is (some #{"Sunday" "Monday" "Tuesday" "Wednesday"
                "Thursday" "Friday" "Saturday"}
              [(dt/get-weekday-name m true)]))
    (is (some #{"sunday" "monday" "tuesday" "wednesday"
                "thursday" "friday" "saturday"}
              [(dt/get-weekday-name m false)]))
    (is (some #{"Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"}
              [(dt/get-weekday-name-short m true)]))
    (is (some #{"sun" "mon" "tue" "wed" "thu" "fri" "sat"}
              [(dt/get-weekday-name-short m false)]))))

(deftest datetime-display-test
  (let [now (dt/now)]
    (is (re-find #"^\d\d:\d\d:\d\d$" (dt/ms->display (dt/ms now) true)))
    (is (re-find #"^\d\d:\d\d$" (dt/ms->display (dt/ms now) false)))
    (is (re-find #"^\d\d\d\d/\d\d/\d\d$" (dt/date-display now true)))
    (is (re-find #"^\d\d\d\d/\d\d$" (dt/date-display now false)))
    (is (re-find #"^\d\d:\d\d:\d\d$" (dt/time-display now true)))
    (is (re-find #"^\d\d:\d\d$" (dt/ms->display (dt/ms now) false)))))
