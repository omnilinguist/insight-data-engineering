# Insight Data Engineering

Problem Statement: https://github.com/InsightDataScience/cc-example

This code uses Scala to solve both the Word Count and Running Median problems in a single pass, owing to the high degree of overlap between the solution strategies. The ``run.sh`` script attempts to use existing Java and Scala runtimes but will attempt to download them if it could not find them.

Due to a stipulation in the problem FAQs, the input data are assumed to be possibly large but not so large that they cannot be processed on a single node with the computing power of a typical modern computer node. As such, a single hash map is used for storing the word counts, with the sorting cost deferred until the map is ready to be written (rather than using a sorted map throughout with an O(log n) access characteristic). A custom class is used to encapsulate the running median data structure, which internally uses two balanced priority queues for efficient processing and computation. These strategies would of course need to be adapted if the data were large enough to require distributed parallel processing.

This code was benchmarked against the NLTK subset of the Gutenberg corpus (http://www.nltk.org/nltk_data/packages/corpora/gutenberg.zip, cf. http://www.nltk.org/book/ch02.html), which after cleaning the data to conform to the specifications of the problem statement, comprise 2,102,546 tokens (53,340 unique - although this number is inflated due to the crude cleaning methodology) over 256,893 lines and 18 files. On a Linode 2GB virtual machine (https://www.linode.com/pricing) running a standard Linux distribution with minimal load, the entirety of ``run.sh`` could usually finish within 15 seconds when using an existing Java+Scala environment.

Note: String interpolation, which was introduced in Scala 2.10, was deliberately avoided to ensure compatibility with older versions of Scala that the test environment(s) might be running.
