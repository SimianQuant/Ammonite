Ammonite-Kernel
===

A stripped-down fork of [Ammonite](https://github.com/lihaoyi/Ammonite) designed for applications that need an embedded 
REPL-like environment.

[![Build Status](https://travis-ci.org/harshad-deo/Ammonite.svg?branch=master)](https://travis-ci.org/harshad-deo/Ammonite)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.simianquant/ammonite-kernel_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.simianquant/ammonite-kernel_2.11)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/e249028e7b5c445982d5d39d97d1e371)](https://www.codacy.com/app/subterranean-hominid/Ammonite?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=harshad-deo/Ammonite&amp;utm_campaign=Badge_Grade)
[![Gitter](https://badges.gitter.im/harshad-deo/typequux.svg)](https://gitter.im/harshad-deo/typequux?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Need
---

While Ammonite works well as a stand-alone application, it is not well-suited for embedded usage because:

1. **Separation of Concerns**: The code to compile, parse, load and evaluate a statement is not separate from that to read it from an input source and 
  print the result to an output source. Though it is [possible](https://github.com/lihaoyi/Ammonite/blob/master/amm/src/test/scala/ammonite/TestRepl.scala)
  to feed in strings and assign the printed output to a string, doing so is quite convoluted. Also there is no simple way to obtain
  the value of an evaluated expression.
2. **Thread Safety**: The coupled mutable state of the application is spread across several classes and several methods without proper synchronization, 
	making multi-threaded usage tricky at best.
3. **Static leakage**: At the time of the fork, several classes leaked static state, making it complicated to run several instances of ammonite at once. 

Usage
---

To use with sbt, add the following line to your build file:

```scala
libraryDependencies += "com.simianquant" %% "ammonite-kernel" % "0.1.1"
```

Currently, only 2.11 binaries are supported.


The project exposes a class, called a `ReplKernel` that does three things:

1. Process a string of text.  
  The type of the function is `String => Option[ValidationNel[LogError, (List[LogMessage], Any)]]`.  
  Therefore, if the string is empty, there is no output. If it's not empty, the output is either a non-empty list of errors or 
  a list of log messages (for example, from the compiler) and the value of the last expression.
2. Provide semantic code completion, given a string and a caret position
3. Dynamically add Jars and repositories. 

Coupled mutable state is localized to single classes and appropriate mutexes have been placed to guarantee consistency. Static state leakage 
has also been eliminated.  

All other features provided by ammonite can be expressed as a combination of these three and mechanisms to read input and pretty print output


