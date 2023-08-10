[![Tests build](https://github.com/henryw374/tiado-cljs2/actions/workflows/tests.yaml/badge.svg)](https://github.com/henryw374/tiado-cljs2/actions/workflows/tests.yaml)

# Portable Shadow-cljs and Kaocha-Cljs2 setup

Managing multiple Clojurescript builds (whether apps or libraries) can be burdensome if each one has its own config. The actual differences between similar builds are usually small, but buried amongst other boilerplate and hard to see. Additionally
as tools, libraries and preferences change over time, each build requires individual effort to update.

Shadow-cljs and Kaocha-Cljs2 are currently my preferred tools for Clojurescript web development. This project uses those and
just adds a small amount of glue to make a portable build/test/deploy setup for easy setup and maintenance.

Both Shadow-cljs and Kaocha offer comprehensive CLI tools. Personally I rarely want that flexibility at the
command line when using them. I prefer to use a REPL at development time, and using Clojure tools.deps `-X` I can execute required build/test functions at CI stage. As a result of that, this setup uses no `shadow-cljs.edn` file or Kaocha `tests.edn` file: all config is done in Clojure code. Note that this is not a hack: as well as a CLI these tools have a Clojure API. This is handy for portability, but also for dynamic changes. For example, to use Kaocha-cljs2's `Funnel` requires
that a port number be referred to from 3 separate places. To change that port from the default, here we just need to bind one variable.  

This project can be used as-is or just as inspiration or a starter template for your builds. For example I used to use a similar no-config-file setup with Figwheel. 

It is quite common that I come across Cljs libraries on Github with no running CI or even tests that I can run locally. Or if I can run the tests locally and they fail, how do I fire up a cljs REPL and work out what went wrong? I don't know but maybe it has been too much effort for the authors to set up or document... this project might help there.

# Usage

To use this library you will need `node` and clojure CLI (`clj`)

## Quick start Clojurescript app

* git clone https://github.com/henryw374/tiado-cljs2.git
* cd -r tiado-cljs2/examples/a-cljs-app
* `clj` 
* in started REPL: `(require 'cljs)`
* `(cljs/test-watch)`
* follow printed instructions to see testing page and run a test
* `(cljs/app-watch)`
* visit http://localhost:9000/ to see the app
* see the comment block in cljs.clj for examples of how to connect a repl. stop/start builds and so on

If you want to keep developing on the example app long-term, move the tiado-cljs2/examples/a-cljs-app directory to your normal dev folder and edit its deps.edn file to point to tiado-cljs2 on github

## Quick start Clojurescript library

Similar to above, but contents in `examples/a-cljs-library`

Also note that this example has an existing `Github Actions` testing setup.

## Add tiado-cljs2 build to an existing Repo

To create from scratch, follow these instructions:

Within a directory containing a deps.edn file:

```
mkdir -p web-target/public
touch web-target/public/.gitkeep
git add -f web-target/public/.gitkeep
```

`web-target` is the default output dir for javascript. It can be whatever you want, just make sure
it :

* is on your classpath (in :paths list in deps.edn)
* contains a `public` dir.
* exists at REPL startup

Add `tiado-cljs2` as a git dep in a tools.deps project and add `web-target` (or equiv) to the `:paths` / `:extra-paths`.

The tiado-cljs2 dep includes Clojurescript (via shadow-cljs) so if that is already in your deps.edn, you should remove it, since Shadow versions are tightly coupled to Clojurescript versions.


Create a Clojure dev namespace from where you will drive actions such as stop/start/release etc with 
contents shown below:

```clojure 

(require '[com.widdindustries.tiado-cljs2 :as util])

; start watch of cljs tests
(util/browser-test-build :watch {})
; open the url printed to console to see kaocha/chui testing page

; run cljs tests (currently just chrome, which needs to be installed already) from clj REPL
(util/run-tests)
; start a cljs repl session in the test build. :cljs/quit to exit
; this works from a regular nREPL cmdline or e.g. with Cursive. It seems like Vim and Emacs users have to do some further backflips (see for example [lambdaisland REPL guide](https://lambdaisland.com/guides/clojure-repls/clojure-repls#orge15e92d))
(util/repl :browser-test-build)

see `.github/workflows` for examples of running these same tests in Github Actions

```

## Example libraries that use this with Github Actions CI

* [cljc.java-time](https://github.com/henryw374/cljc.java-time)
* ... in fact, all cljs libraries I maintain and have got round to adding this to

## Example apps that use this

* [cljs-date-lib-comparison](https://github.com/henryw374/cljs-date-lib-comparison)
* [Firebase todo list](https://github.com/henryw374/firebase-clojurescript-todo-list)

## Rationale continued..

* Stay DRY wrt any config or build related code within and across projects
* avoid separate processes/jvms for things that can run in one jvm (i.e. no separate Funnel process or shadow server jvm). Typically I will run a separate vm for `server-side` Clojure code as that will have a different classpath to cljs build.

### REPL first

I like having the ability to do everything from a Clojure REPL. For example, 

* stop/start any servers (shadow, funnel etc),
* change config, 
* run release builds, 
* run clj and cljs tests etc
* enter and exit cljs REPL

## Testing aims 

- run tests under advanced compilation
- run in browser & other targets

## Future work 

Add more testing targets and build setups so that this can be used to test any Clojurescript library
such as discussed in the [Library Consumers Test](https://github.com/henryw374/clojurescript-library-consumers-test)

## License

Copyright © 2022 [Widd Industries](http://widdindustries.com/about/)

Distributed under the [MIT License](/LICENSE)


