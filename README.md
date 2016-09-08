#### Main updates
20/03/2016: Pool hierarchy management (e.g.) foo.bar.baz is a logical thread pool contained in foo.bar + other minor changes
15/12/2015: First version

# reactiveWM: a reactive framework for webMethods
### I am already using the *doThreadInvoke()* standard API, why should I use reactiveWM?
That's already a good start :)

Basically reactiveWM **extends** standard webMethods capabilities. It allows to create native webMethods threads within an IS thread pool (just like *doThreadInvoke()* method) but the main difference is that you can create you own logical thread pool and limit the maximum number of threads running in parallel.

Let's take an example: a developer must handle a file and would like to parallelize the execution to create one thread for each line. With standard webMethods API, there is no way to limit the number of parallel threads. The only limit is the IS service thread pool. But if the number of lines is higher than this limit, it will simply block the IS.

With reactiveWM, the developer can simply create its own logical thread pool and limit the number of parallel threads (let's say in our example 20). reactiveWM will then make sure that at any moment, the maximum value is not exceeded.
So definitely, the first capability is the **security**.

In terms of **timeout** management, reactiveWM goes a step further as well. Currently, the webMethods API offers only a timeout to obtain a thread from the IS pool thread. There is no timeout related to threads execution.

reactiveWM allows to set a timeout on the execution of thread list. If a timeout is reached, reactiveWM returns an exception to the client, cancel the threads which are in wainting mode (submitted in a logical thread pool but not yet started) and waits for the completion of the running ones. It is a graceful way to limit the execution time.

reactiveWM includes also a **failfast** mechanism. If a developer submits a thread list with the failfast option checked, when one threads fails (due to a ServiceException for instance), reactiveWM will rethrow the exception up to the client. Then it will be similar than in case of timeout, reactiveWM cancel the waiting threads and wait for the running ones.

There are other options available such as having the capability to manage volatile thread pool (temporary with an explicit TTL), to manage pool hierarchy (foo.bar is the parent of foo.bar.baz) etc...

### Why is reactiveWM reactive?

Mainly because reactiveWM provides **non-blocking methods**. When a thread is submitted, reactiveWM returns a *ListenableFuture.* This class is a Guava implementation of the *Future* interface allowing a developer to register listeners.

reactiveWM is built on top of Guava and allows developers to create callbacks and to chain dynamically and asynchronously webMethods service executions. 

## How do I build the solution?
The solution is easily buildable from your local IDE. You simply need to import the following libraries:
* wm-isserver.jar: server webMethods API
* wm-isclient.jar: client webMethods API
* guava-*.jar: Guava library (tested with version 18)

## Where do I start?
After having built the solution, you can take a look at the ReactiveWMFacade class in the org.reactivewm.facade package.

This class is a facade to all reactiveWM functionalities:
* Thread pool management and thread creation
* Parallelize: submit a list of ServiceThread and waits for the completion of all of them
* Submit: submit a new ServiceThread and returns a ListenableFuture
* Wait: wait for the completion of a Future list
* Chain: asynchronous and dynamic chaining of a Future

## How can I test it?
In the Guithub repository you will find two directories:
* src: source code of reactiveWM
* wM: a webMethods package containing a client to reactiveWM and embedding all required runtime libraries (commons-lang, guava, joda-time, mvel2, reactiveWM)

## Technical questions

#### Why to use Guava instead of native Java capabilities?
Because the non-blocking functions are coming only with Java 8. 

First of all webMethods Java 8 support is relatively recent and also I believe that so far most of the customers are running ISs on top of Java 6 or 7 only.

#### Why have you created your own ThreadPoolExecutor instead of simply reusing the Java standard one?
The ThreadPoolExecutor Java class runs directly a Runnable though the *run()* method. In webMethods, you cannot simply run a ServiceThread like this. Instead, you have to pass your ServiceThread reference to the IS ThreadManager and call the *runTarget()* method. So I had to create my own ThreadPoolExecutor (ISThreadPoolExecutor).

#### What is a Controller?
A controller is an internal asynchronous function used in case of a timeout to cancel the waiting threads.

#### What about error management?
All errors (timeout, service execution exceptions) are thrown to the client except for the uncaught ones. In the current implementation, the uncaught exceptions are managed by the ISThreadFactory class which uses the JournalLogger to log them. 

If you want to bypass the JournalLogger system (for instance if you developed your own logging mechanism), you have to create your own ThreadFactory and reference it during the thread pool creation (*ReactiveWMFacade.createPool()*).

#### Why have you created your own ServiceThread class?

I had to create an extension of the ServiceThread class (called ReactiveServiceThread) for two reasons:
* To manage the cancel order
* To manage priorities within a logical IS thread pool (the priorities are not set to the Thread objects though)
