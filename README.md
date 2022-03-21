# tiadough

The last ever Clojurescript (web) build setup.

## Problems 

I have used many Cljs build configurations over time and here are some of the issues I have had:

* Hard to test advanced-compiled code. tl;dr if you are not running your tests 

*

Use Shadow-CLJS, but from a Clojure REPL.

## General aims

Would rather avoid: 

- separate processes/jvms for things that can run in one jvm 
- yet more config files 
   - prefer clojure data to config shadow-cljs.edn & kaocha tests.edn
   - package.json and package-lock.json written to by shadow only - use deps.cljs for npm deps 

what I do want: 

- a Clojure repl (& to be able to enter and quit cljs repls from that)

- ability to stop/start any servers etc running in that

- trigger test runs (Clojure and Cljs) from Clojure repl  

## Testing aims 

- run tests under advanced compilation
- run in browser & node