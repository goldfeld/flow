(ns flow.core
  (:require #_[cljs.core.typed :as t :include-macros true]
            [cljs.core.async :as a]
            #_[cljs.core.typed.async :as ta :include-macros true]
            [flow.datetime :as dt]
            [flow.clock :as clock]))

#_(t/defalias DurationUnit
  "A keyword indicating the unit of the duration (:min[utes],
  :s[econds], :h[ours], :d[ays] or :ms [milliseconds])"
  (t/U ':min ':s ':h ':d ':ms))

#_(t/ann ->ms [t/Int DurationUnit -> dt/Milliseconds])
(defn ->ms [d unit]
  (case unit
    :min (* d 1000 60)
    :s (* d 1000)
    :h (* d 1000 60 60)
    :d (* d 1000 60 60 24)
    d))

#_(t/defalias MaybeString
  (t/U nil t/Str))

#_(t/defalias Action
  (t/Fn [-> MaybeString]))

#_(t/defalias Block
  "A block is a tuple where the first element is the duration of the
  block with the second being its time unit, and the third and last is
  a function to be executed when the block starts, which can contain
  side-effects and can return a string to be used as a status
  message/label while the block is running."
  (t/HVec t/Int DurationUnit Action))

#_(t/defalias FlowSeq
  "A flow seq is simply a seq of blocks. Here's a realistic, if
  contrived, example:
  [[25 :min go-do-work]
   [5 :min go-to-web-browser]
   [25 :min go-do-work]
   [5 :min go-to-web-browser]]"
  (t/Seq Block))

#_(t/defalias CreateAction
  [t/Kw t/Any * -> t/Fn])

#_(t/defalias OSFunctions
  (t/HMap :mandatory {:render-clock t/Fn
                      :set-clock-content [clock/Clock t/Str -> t/Any]
                      :create-action CreateAction
                      :execute-alert t/Fn}))

#_(t/defalias FlowOptions
  (t/HMap :optional {:clock-id t/Kw}))

#_(t/defalias FlowEvent
  (t/HMap :mandatory {:flow (t/U ':block-start ':block-end ':end)}
          :optional {:msg t/Any}))

#_(t/defalias FlowChannel
  (ta/Chan FlowEvent))

#_(t/ann flow (t/Fn [FlowSeq FlowOptions OSFunctions clock/Timer -> FlowChannel]
                  [FlowSeq FlowOptions OSFunctions -> FlowChannel]

                  [FlowSeq FlowOptions -> FlowChannel]
                  [FlowSeq -> FlowChannel]))
(defn flow
  "Starts a workflow, and expects a flow seq as a minimum argument.
  Returns a channel which will output each workflow event as it
  happens (like the change of a block.)"
  ([flow-seq options os-fns last-timer]
   (js/clearInterval last-timer)
   (flow flow-seq options os-fns))
  ([[now & then] {:keys [clock-id] :as options} os-fns]
   (let [chan (a/chan) #_(ta/chan :- FlowChannel)]
     (if-let [[duration unit action] now]
       (let [info (action)
             delta (->ms duration unit)
             next-args (concat [then options os-fns]
                               (when clock-id
                                 (clock/set-timer
                                  clock-id (+ (dt/now-ms) delta)
                                  (:output info) os-fns :seconds)))]
         (a/put! chan {:flow :block-start :msg info})
         (js/setTimeout (fn []
                          (a/put! chan {:flow :block-end :msg info})
                          (a/pipe (apply flow next-args) chan false))
                        delta))
       (do (a/put! chan {:flow :end})
           (a/close! chan)
           (when clock-id
             (clock/set-current-time clock-id os-fns :seconds))))
     chan))
  ([flow-seq options] (flow flow-seq options {:set-clock-content identity
                                               :render-clock identity}))
  ([flow-seq] (flow flow-seq {})))

#_(defalias BlockSeqSansLeadUp
  (t/Vec (t/U t/Int ':. DurationUnit)))

#_(defalias BlockSeq
  "A block seq can start with a lead-up keyword (:..) and then have
  any number of integers (blocks) and :. keywords (skips), and
  finishes with the duration unit. Since core.typed can't (I guess)
  represent a given type in the last element of an unknown-length
  heterogeneous vector, this can't be notated as strictly as it was
  previously when it used Prismatic's Schema."
  (t/U (t/HVec ':.. (t/U t/Int ':. DurationUnit) *)
       BlockSeqSansLeadUp))

#_(t/ann has-lead-up? [BlockSeq -> t/Bool])
(defn has-lead-up?
  [block-seq]
  "The lead-up is the \":..\" syntax a block-seq accepts to indicate
  it must be scheduled last."
  (= :.. (first block-seq)))

#_(t/ann sans-lead-up (t/Fn [BlockSeq -> BlockSeqSansLeadUp]
                          [BlockSeq Bool -> BlockSeqSansLeadUp]))
(defn sans-lead-up
  ([block-seq] (sans-lead-up block-seq (has-lead-up? block-seq)))
  ([block-seq has-lead-up] (if has-lead-up (rest block-seq) block-seq)))

#_(defalias ConfigSeq
  "The config seq has a label, a block seq, an action type keyword
  followed by any number of arguments to that action type. Action
  types and their arguments are fed into the create-action function,
  which is itself (optionally) supplied by the library's user,
  otherwise defaulting to 'identity'."
  (t/HVec t/Str BlockSeq t/Kw t/Any *))

#_(t/ann config-seq->blocks (t/Fn
                           [CreateAction t/Int t/Int t/Int ConfigSeq -> FlowSeq]
                           [CreateAction t/Int t/Int ConfigSeq -> FlowSeq]))
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
   (config-seq->blocks
    create-action target-length lead-up-length nil config-seq))
  ([create-action target-length lead-up-length trackno [label block-seq & args]]
   (let [durations (butlast (sans-lead-up block-seq (> lead-up-length 0)))
         duration-unit (last block-seq)
         action (apply create-action (if (= 1 (count args))
                                       (:on-start args)
                                       args))]
     (concat
      (repeat lead-up-length nil)
      (map-indexed (fn [i d] (when (not= :. d)
                               [d duration-unit
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

#_(t/ann get-target-length [(t/Seq BlockSeq) -> t/Int])
(defn get-target-length
  "Gets the biggest length of blocks (durations) among a group of
  block-seqs."
  [block-seqs]
  (->> block-seqs
       (map (comp count butlast sans-lead-up))
       (apply max)))

#_(defalias Config
  (t/Seq ConfigSeq))

#_(t/ann config->flow (t/Fn [Config -> FlowSeq]
                          [CreateAction Config -> FlowSeq]
                          [CreateAction t/Int t/Int t/Bool Config -> FlowSeq]))
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
