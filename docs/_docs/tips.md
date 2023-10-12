---
Title: Tips and Tricks
nav_order: 15
layout: page
---

# Tips and Tricks

(More to come!)

## Compile time optimizations

Try to separate your `@DesignDoc()` declarations into individual files. The
declarations must be reprocessed and recompiled any time a change is detected in
it's containing file, so minimizing the amount of code that could change will
minimize the amount of time spent recompiling your source.
