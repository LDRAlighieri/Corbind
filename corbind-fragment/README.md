﻿
# corbind-activity

To add androidx fragment bindings, import `corbind-fragment` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-fragment:1.6.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**FragmentManager** | `resultEvents` | Called when any results set by setFragmentResult using the same requestKey.


## Simple examples

```kotlin
lifecycleScope.launchWhenStarted {
    parentFragmentManager.resultEvents(
        requestKey = FRAGMENT_REQUEST_KEY,
        lifecycleOwner = this@CurrentFragment
    )
        .onEach { event -> /* handle result event */ }
        .launchIn(this@launchWhenStarted) // lifecycle-runtime-ktx
}
```

More examples in source code