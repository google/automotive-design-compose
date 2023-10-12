---
Title: Tips and Tricks
nav_order: 15
layout: page
---

{% include toc.md %}

# Tips and Tricks

## Compile time optimizations

Try to separate your `@DesignDoc()` declarations into individual files. The
declarations must be reprocessed and recompiled any time a change is detected in
it's containing file, so minimizing the amount of code that could change will
minimize the amount of time spent recompiling your source.
