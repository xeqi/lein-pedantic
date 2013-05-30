# Deprecation Notice

This project is now deprecated as leiningen 2.2.0 includes a large portion of its features.  Please see [this thread on the clojure group](https://groups.google.com/d/msg/clojure/9cA5hvFJTkw/fnWwxvALd64J) for more details.





# lein-pedantic

A Leiningen plugin to reject dependency graphs with common user surprises.

## Example

```clojure
(defproject sample "0.0.1"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.cemerick/friend "0.0.9"]
                 [noir "1.3.0-beta9"]])
```

Normally with lein this project will use [ring/ring-core "1.0.2"] due to friend, and will use [ring/ring-jetty "1.1.0"] due to noir.  These version do not work together and resulted in issue https://github.com/cemerick/friend/issues/15.

Using lein-pedantic will produce a message and fail the dependency resolution.

```
Failing dependency resolution because:

[com.cemerick/friend "0.0.9"] -> [ring/ring-core "1.0.2"]
  is overruling
[noir "1.3.0-beta9"] -> [compojure "1.0.4"] -> [ring/ring-core "1.1.0"]

Please use [com.cemerick/friend "0.0.9" :exclusions [ring/ring-core]] to get [ring/ring-core "1.1.0"] or use [noir "1.3.0-beta9" :exclusions [ring/ring-core]] to get [ring/ring-core "1.0.2"].
```

## Usage

lein-pedantic requires leiningen 2.

Put `[lein-pedantic "0.0.5"]` into the `:plugins` vector of your `:user` profile.  It automatically hooks into leiningen and will run any time leiningen tries to pull dependencies.

If you would prefer a warning instead of a failure then add `:pedantic :warn` to the project.clj.  The `lein repl` task will default to a warning, as it adds dependencies to the project that you do not have control over.

## Rules

The rules lein-pedantic uses to fail a dependency resolution are approximately:

1. A top level dependency is overruled by another version.

2. A transitive dependency is overruled by an older version.

## License

Copyright © 2012 Nelson Morris

Distributed under the Eclipse Public License, the same as Clojure.
