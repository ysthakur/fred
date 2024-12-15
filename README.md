# Fred

Fred is a programming language made to explore a compiler optimization for lazy mark scan, which is an algorithm for collecting cycles when doing reference counting. See https://github.com/ysthakur/fred/blob/main/writeup/writeup.pdf for more information.

With the lazy mark scan algorithm, whenever an object's reference count is decremented but it doesn't hit 0, it's added to a list of potential cyclic roots (PCRs). Every once in a while, you go through these PCRs (as a group) and perform trial deletion to get rid of cycles. But you need to scan every single object reachable from all of these PCRs. So, I worked on a way to reduce this scanning using type information.

To do this, you can first partition the graph of types into its [strongly-connected components](https://en.wikipedia.org/wiki/Strongly_connected_component) (SCCs). If you have two objects object `a` and `b` of types `A` and `B` respectively, and `A` and `B` are not from the same SCC, you know that `a` and `b` cannot possibly form a cycle. This is important.

Now that you have these SCCs, you don't need to maintain a flat list of PCRs. Instead, you can maintain a list of PCR buckets, with each bucket containing PCRs from a different SCC (bucket 0 contains all the PCRs from SCC 0, bucket 1 contains all the PCRs from SCC 1, ...). When processing the PCRs, you process one bucket at a time, rather than the entire list of PCRs. This makes the algorithm more incremental. That's not the main improvement, though.

The main improvement is that when processing a PCR from, say, SCC 5, if it has any references to objects outside of SCC 5, you don't need to scan those objects. This does require sorting the buckets according to the SCC they're for. And SCCs are sorted topologically, so types from SCC 2 can only have references to types from SCC 2, 3, etc. but they can't have references to types from SCC 0 and 1.

This has probably been done already, so if you come across such a paper or project, please let me know, I'd be very interested.

## Why name it Fred?

When I was young, I had a cute little hamster called Freddie Krueger, so named because of the hamster-sized striped red sweater my grandmother had knitted for him, as well as his proclivity for murdering small children. In his spare time, Fred would exercise on his hamster wheel, or as he liked to call it, his Hamster Cycle.

But one day, I came home to find Fred lying on the hamster cycle, unresponsive. The vet said that he'd done too much running and had had a heart attack. I was devastated. It was then that I decided that, to exact my revenge on the cycle that killed Fred, I would kill all cycles.
