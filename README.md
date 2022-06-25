[![Tests build](https://github.com/henryw374/tiado-cljs2/actions/workflows/tests.yaml/badge.svg)](https://github.com/henryw374/tiado-cljs2/actions/workflows/tests.yaml)

# ready-made Shadow-cljs and Kaocha-Cljs2 setup

Managing multiple Clojurescript builds, either apps, libraries or both can be tricky if each one has its own config. There may well need to be differences across builds, but in many
cases the actual differences will be small, but buried amongst other boilerplate and hard to see. Additionally
as tools, libraries and preferences change over time, each requires individual effort to update.

Shadow-cljs and Kaocha-Cljs2 are currently my preferred tools for Clojurescript web development. This project uses those and
just adds a bit of glue to make a portable build/test/deploy setup for easy setup and maintenance. This gets more useful the more builds under management.

Both Shadow-cljs and kaocha offer comprehensive CLI tools. Personally I rarely want that flexibility at the
command line when using them. A REPL is fine when developing, and using Clojure tools.deps I can execute required build/test functions at CI stage. As a result of that, this setup uses no `shadow-cljs.edn` file or Kaocha `tests.edn` file: all config is done in Clojure code. This is hany for portability, but also for dynamic changes. For example, Kaocha-cljs requires
that a port number is referred to from 3 separate places. If that port needs settting, with code rather than config files, it is just done in one place.

This project can be used as-is or just as inspiration or a starter template for your builds. For example I used to use a similar no-config-file setup with Figwheel. 

It is quite common that I come across Cljs libraries on Github with no running CI or even tests that I can run locally. I don't know but maybe it has been too much effort to set up... this project might help there.

# Usage

See examples dir for:

* an example cljs lib with `Github Actions` setup.
* an example cljs app with test, devcards and release build setup 

## Setup

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

Add `tiado-cljs2` as a git dep in a tools.deps project. 

```clojure 

(require '[com.widdindustries.tiado-cljs2 :as util])

; start watch of cljs tests
(util/browser-test-build :watch {})
; open the url printed to console to see kaocha testing page

; run cljs tests from clj REPL
(util/run-tests)
; start a cljs repl session in the test build. :cljs/quit to exit
(util/repl :browser-test-build)

```

## Example libraries that use this with Github Actions CI

* [cljc.java-time](https://github.com/henryw374/cljc.java-time)
* ... in fact, all cljs libraries I maintain and have got round to adding this to

## Example apps that use this

* [cljs-date-lib-comparison](https://github.com/henryw374/cljs-date-lib-comparison)
* [Firebase todo list](https://github.com/henryw374/firebase-clojurescript-todo-list)

## Rationale continued..

* Stay DRY wrt any config or build related code within and across projects

* I would rather avoid: 

- separate processes/jvms for things that can run in one jvm (i.e. no separate Funnel process) 
- yet more config files 
   - prefer clojure code to config files, so no `shadow-cljs.edn` or kaocha `tests.edn`
   - package.json and package-lock.json written to by shadow only - use deps.cljs for npm deps 

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
