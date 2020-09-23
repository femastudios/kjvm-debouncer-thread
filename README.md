# Debouncer thread
A very simple and easy to use Kotlin JVM implementation of a debouncer thread.

A debouncer thread controls the execution of a given operation using two parameters:
* `waitTime`: controls the time to wait before executing the operation. If another request is made while waiting, the wait will start again.
* `maxWaitTime`: if not `null`, provides a way to bound the wait to a maximum value.

Example using `waitTime=250` and `maxWaitTime=1500`:
```kotlin
debounce() // Requests operation execution
// after 100ms
debounce()
// after 100ms
debounce()
// after 250ms the operation is executed

debounce()
// after 1s  
debounce()
// after 500ms the operation is executed
debounce()
// after 250ms the operation is executed
```

## Getting started
The most basic usage is the following:
```kotlin
val dt = DebouncerThread(waitTime=250, maxWaitTime=1500) {
    // Your code
}
// Thread is started automatically (controlled with start constructor argument)

// Later...
dt.debounce() // Requests operation execution

// ..or to trigger immediately
dt.debounceNow()
```
The `maxWaitTime` parameter can be ombitted and will be null by default.

## Parameterized example
A more complex usage is using parameters:
```kotlin
val dt = DebouncerThread<Int>(waitTime=250, maxWaitTime=1500) {
    // The only argument of this lambda is a List<Int>
    // This list will contain the list of parameters passed to debounce()
}

// Later...
dt.debounce(1)
dt.debounce(2)
dt.debounce(3)
dt.debounce() // If no parameter provided, nothing is added to the list

// To call immediately you can still use debounceNow
dt.debounceNow(5)
// This will trigger the operation immediately, passing all the accumulated parameters
```