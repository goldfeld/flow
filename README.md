# flow

A library for defining, running and displaying timers and workflows.

## Usage

The main two functions are on `flow.core`: `flow` and
`config->flow`. the former function runs a workflow proper, which
should be in flow structure (see below.) It can also be passed a
`clock-name` so it keeps whatever UI you hook up in sync with the
workflow. The latter function converts a workflow in config format to
the flow format.

Here is an example of a simple config:

``` Clojure
[["working.." [25 25 :min] :exec "emacs my-work-file.clj"]
 ["playing!" [5 5 :min] :exec "emacs -f tetris"]
 ["break" [:.. 15]]]
```

The action to be executed at the end (e.g. :exec "(shell command)")
is optional.

The config will translate into the following flow structure:

``` Clojure
[[25 :min (fn [] #(exec "emacs my-work-file.clj") "working..")]
 [5 :min (fn [] #(exec "emacs -f tetris")) "playing!"]
 [25 :min (fn [] #(exec "emacs my-work-file.clj") "working..")]
 [5 :min (fn [] #(exec "emacs -f tetris")) "playing!"]
 [15 :min]]
```

As you can see, the first two "tracks" (work and play) had two blocks,
so they were perfectly interleaved, and the single break block was put
last by using the ":.." syntax. When one track has more blocks than
the others, it will predictably go on to finish all its extra blocks
in a row after the other tracks are done. If a track has less blocks
than the others, it will end sooner. However, you can do this:

``` Clojure
[["work track" [25 :. 25]
 ["play track" [5  5  5]]
```

Which will effectively execute two play blocks in a row after the
first work block, and only then execute the last work and play
blocks. Note that ":." is not the same as "0"; the former will skip
the block's action and/or alert, whereas the latter will still execute
those.

If you just want a single track (i.e. no interleaving), you can do
either:

``` Clojure
[["single track" [25 25 25 25 15 :min] :exec "emacs work.clj"]]
```

Or:

``` Clojure
[["job" [25 :min] :exec "emacs app.clj"]
 ["job" [35 :min] :exec "emacs app.clj"]
 ["20% time" [15 :min] :exec "emacs cute-project.clj"]]
```

## License

Copyright © 2014 Vic Goldfeld

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
