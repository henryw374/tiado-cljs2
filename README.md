# The last Clojurescript (web) build setup you'll ever need

well... almost.

Shadow and Kaocha-Cljs2 are amazing tools for Clojurescript web development. This project 
contains some glue that has helped make using them anywhere and everywhere really easy. 

# Usage 

see examples dir

```
mkdir -p web-target/public
touch web-target/public/.gitkeep
git add -f web-target/public/.gitkeep

```






######################### chop 
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