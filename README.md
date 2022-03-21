# tiadough

The last Clojurescript (web) build setup you'll ever need ;-)

Shadow and Kaocha-Cljs2 are amazing tools for Clojurescript web development. This project 
contains some glue that has helped me make using them really easy. 

## Problems this aim to address 

I have used many Cljs build configurations over time and here are some of the issues I have had:

* Hard to test `:advanced` compiled code. tl;dr if you release your code having compiled it with
:advanced opt, it should be tested under `:advanced` as well. This is especially important if your code 
is doing interop, but also because [not all valid Clojurescript will survive `:advanced`](https://clojure.atlassian.net/browse/CLJS-3315).

* Not REPL friendly. Having a CLI is good for Continuous Integration (CI), but the rest of the 
time I just want to be calling Clojure functions from the REPL to compile and test.

* Lots of boilerplate/config-files for Cljs or tests. This continues the previous point - some 
people may prefer a special CLI interface for a Clojure tool, but I prefer a Clojure REPL. The contents
of config files like  `shadow-cljs.edn` or `tests.edn` will just become arguments to Clojure functions
invoked by the CLI. Since I'm calling the functions directly, I don't need the config files. 
Config files often contain needless repetition or [provide special workarounds](https://shadow-cljs.github.io/docs/UsersGuide.html#config-merge)
in order to make them dynamic. This is not necessary if doing everything from the REPL. 

* Multiple processes/jvms. Typically with CLI to run Shadow-CLJS and Kaocha-cljs2 
(with Funnel), and my own app server I might end up running 3 jvms, but we actually only need one jvm to do all this.
I can split the processes into separate ones if I want to, but generally having more than one just means more to think about and 
manage.   

* Having to individually understand how to watch+repl+test every single open source Clojurescript project I come across. 
There are many ways of setting up 
a Clojurescript project, but essentially when I open up someone else's project, as long as it has a `deps.edn` file, I want to be 
able to use my own watch+repl+test config with it, without changing the project at all. At the end of the day every Clojurescript project 
is just some source code and maybe deps - I want to be able to treat them all the same.

* Github actions CI setup. I have failed to find a single Clojurescript open source lib on Github that uses Kaocha-cljs2 and 
github actions. The Cljs projects I maintain will be the first then possibly - and they should all require close to zero 
individual configs to do so.  

# Usage 








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