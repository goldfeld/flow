(ns flow.core
  (:require-macros [schema.macros :refer [defschema =>]])
  (:require [schema.core :as s]
            [flow.datetime :refer [now-ms]]
            [flow.clock :refer [set-timer set-current-time]]))

(defschema DurationUnit
  (s/one (s/enum :min :s :h :d :ms) "DurationUnit"))

(defn ->ms [d unit]
  (case unit
    :min (* d 1000 60)
    :s (* d 1000)
    :h (* d 1000 60 60)
    :d (* d 1000 60 60 24)
    d))

(defn flow
  "Starts a workflow, and expects a flow seq as a minimum argument.
  It's also useful to provide a clock as second argument, otherwise
  the flow will run in \"headless\" mode. The workflow seq is made up
  of blocks, which are tuples (vectors) of a specific format: the
  first element is the duration of the block; the second is a keyword
  indicating the unit of the duration (:min[utes], :s[econds],
  :h[ours], :d[ays] or :ms [milliseconds]) and the third and last is a
  function to be executed when the block starts, which can contain
  side-effects and can return a string to be used as a status
  message/label while the block is running. Here's a realistic, if
  contrived, example:
  [[25 :min go-do-work]
   [5 :min go-to-web-browser]
   [25 :min go-do-work]
   [5 :min go-to-web-browser]]"
  ([flow-seq options os-fns last-timer]
     (js/clearInterval last-timer)
     (flow flow-seq options os-fns))
  ([[now & then] {:keys [clock-id callback]} os-fns]
     (if now
       (let [status ((last now))
             delta (->ms (first now) (second now))
             args (if-not clock-id
                    [then nil nil]
                    [then clock-id os-fns (set-timer
                                           clock-id (+ (now-ms) delta)
                                           status os-fns :seconds)])]
         (when callback (callback))
         (js/setTimeout #(apply flow args) delta))
       (when clock-id (set-current-time clock-id os-fns :seconds))))
  ([flow-seq options] (flow flow-seq options {:set-clock-content identity
                                              :render-clock identity}))
  ([flow-seq] (flow flow-seq nil nil)))

(defn has-lead-up?
  [block-seq]
  "The lead up is the \":..\" syntax a block-seq accepts to indicate
  it must be scheduled last."
  (= :.. (first block-seq)))

(defn sans-lead-up
  ([block-seq] (sans-lead-up block-seq (has-lead-up? block-seq)))
  ([block-seq has-lead-up] (if has-lead-up (rest block-seq) block-seq)))

(defschema BlockSeq
  [(s/optional (s/eq :..) "LeadUp")
   (s/either s/Num (s/eq :.))
   DurationUnit])

(defschema ConfigSeq
  [(s/one s/Str "Label")
   (s/one BlockSeq "blockseq")
   (s/one s/Keyword "ActionType")
   s/Any])

(def Block
  [(s/one s/Num "Duration")
   DurationUnit
   (=> (s/maybe s/Str))])

(defn config-seq->blocks
  "Expands a config-seq into a sequence of blocks, which will all have
  the same properties except for possibly different durations as
  dictated by the block-seq (e.g. \"[1 3 8 10 :min]\") embedded in the
  config-seq. Takes a create-action function which should return a
  function to be executed at each block's start; a target-length which
  will inform how many nil-blocks to pad at the end of the returned
  sequence (used to generate equal-length sequences for later
  interleaving); a lead-up-length which informs if we should add lead
  up nil-block padding (when the block-seq starts with \":..\") and
  how much; and finally the config-seq itself."
  ([create-action target-length lead-up-length config-seq]
     (recur create-action target-length lead-up-length nil config-seq))
  ([create-action target-length lead-up-length trackno [label block-seq & args]]
     (let [durations (butlast (sans-lead-up block-seq (> lead-up-length 0)))
           duration-unit (last block-seq)
           action (apply create-action (if (= 1 (count args))
                                         (:on-start args)
                                         args))]
       (concat
        (repeat lead-up-length nil)
        (map-indexed (fn [i d] (when (not= :. d) [d duration-unit
                                                  (fn []
                                                    {:output (action)
                                                     :track-label label
                                                     :track-number trackno
                                                     :block-number (inc i)
                                                     :block-length d
                                                     :block-unit duration-unit
                                                     :block-ms
                                                     (->ms d duration-unit)})]))
                     durations)
        (repeat (- target-length (count durations)) nil)))))

(defn get-target-length
  "Gets the biggest length of blocks (durations) among a group of
  block-seqs."
  [block-seqs]
  (->> block-seqs
       (map (comp count butlast sans-lead-up))
       (apply max)))

(defschema Flow
  [s/Block])

(defn config->flow
  "Converts from workflow config format to the flow seq accepted by
  the flow.core/flow function. The config format is nicer for humans
  because it's less repetitive and more declarative, while the proper
  execution works better with a more procedural structure. See the
  README for example config data and what flow structures they
  produce."
  ([config-seqs] (config->flow #(fn [] nil) config-seqs))
  ([create-action config-seqs]
     (let [{track-config-seqs false
            trail-config-seqs true} (group-by #(has-lead-up? (second %))
            config-seqs)
            lead-up-length (get-target-length (map second track-config-seqs))
            trail-length (get-target-length (map second trail-config-seqs))
            ->flow (partial config->flow create-action)]
       (concat
        (->flow lead-up-length 0 :add-trackno track-config-seqs)
        (->flow trail-length lead-up-length false trail-config-seqs))))
  ([create-action target-length lead-up-length add-trackno? config-seqs]
   (let [mapper (if add-trackno? map-indexed map)]
     (->> config-seqs
          (mapper (partial config-seq->blocks create-action
                           target-length lead-up-length))
          (#(if (> (count %) 1)
              (apply interleave %)
              (first %)))
          (keep identity)))))
