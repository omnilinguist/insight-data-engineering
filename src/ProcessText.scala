import java.io.{File, PrintWriter}
import java.nio.charset.CodingErrorAction
import java.util.{HashMap => JHashMap, Map => JMap, TreeMap => JTreeMap}
import scala.collection.JavaConverters._
import scala.collection.mutable.PriorityQueue
import scala.io.{Codec, Source}

object ProcessText {
  val WordCountFilename = "wc_result.txt"
  val RunningMedianFilename = "med_result.txt"

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: scala ProcessText [input_dir] [output_dir]")
    } else {
      val input = new File(args(0))
      val output = new File(args(1))
      if (!input.exists || !input.isDirectory || !output.exists || !output.isDirectory) {
        println("Both input and output directories must be valid!")
      }
      processFiles(input, output) 
    }
  }
 
  /**
   * Process text files from the input directory and write the word count and
   * running median to the output directory.
   */
  def processFiles(input: File, output: File): Unit = {
    val wordCounts = new JHashMap[String, Int]
    val runningMedian = new RunningMedian

    val wordCountFile = new File(output.getPath + File.separator + WordCountFilename)
    val runningMedianFile = new File(output.getPath + File.separator + RunningMedianFilename)
    wordCountFile.createNewFile()
    runningMedianFile.createNewFile()
    val wordCountWriter = new PrintWriter(wordCountFile)
    val runningMedianWriter = new PrintWriter(runningMedianFile)

    // (Thanks to http://stackoverflow.com/questions/13625024/how-to-read-a-text-file-with-mixed-encodings-in-scala-or-java)
    // Though we may assume Unicode characters, some custom text data might have special characters or encodings
    // that must be handled correctly.
    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    val files = input.listFiles.sorted    // per specification
    files.foreach { file =>
      println("Processing file: " + file + "...")
      val lines = Source.fromFile(file).getLines
      lines.foreach { line =>
        // Tokenize line, lowercase tokens while removing all punctuation, hyphens, numbers, and other non-alphabetic characters
        val tokens = line.replaceAll("[^A-Za-z\\s]*", "").toLowerCase.split("\\s+").filter(_.length > 0)
        // Handle blank lines. If blank lines should be ignored, the processLine() command could be restricted to only
        // be executed when tokens.length > 0.
        processLine(tokens, wordCounts, runningMedian)
        runningMedianWriter.println("%.1f".format(runningMedian.currentMedian))
      }
    }

    // Sort word counts and write to file
    wordCounts.entrySet.asScala.toList.sortBy(_.getKey).foreach { entry =>
      wordCountWriter.println(entry.getKey + "\t" + entry.getValue)
    }

    wordCountWriter.close()
    runningMedianWriter.close()

    val (maxHeap, minHeap) = runningMedian.dequeueAll()
    println("Sanity check:" +
      "\nSize: max heap (left side) = " + maxHeap.size + ", min heap (right side) = " + minHeap.size +
      "\nRange: max heap (left side) = [" + maxHeap.min + ", " + maxHeap.asInstanceOf[TraversableOnce[Int]].max + "], min heap (right side) = [" + minHeap.min + ", " + minHeap.asInstanceOf[TraversableOnce[Int]].max + "]")
    val allCounts = maxHeap ++ minHeap
    val countOfCounts = allCounts.groupBy(identity).map { case (k, v) => (k, v.length) }
    val distribution = new JTreeMap[Int, Int] 
    distribution.putAll(countOfCounts.asJava)
    println("Distribution: " + distribution)
  }

  /**
   * Process a single line as defined by its constituent tokens
   * @param tokens
   */
  def processLine(tokens: Seq[String], wordCounts: JMap[String, Int], runningMedian: RunningMedian): Unit = {
    // Increment the count of the word in the hashtable by 1
    tokens.foreach { token =>
      if (!wordCounts.containsKey(token))
        wordCounts.put(token, 1)
      else
        wordCounts.put(token, wordCounts.get(token) + 1)
    }

    runningMedian.add(tokens.length)
  }
}

/**
 * Implement an efficient running median using a manually balanced max heap (for the left side) together with a min heap (for the right side).
 */
class RunningMedian {
  private val maxHeap: PriorityQueue[Int] = PriorityQueue.empty(Ordering[Int])
  private val minHeap: PriorityQueue[Int] = PriorityQueue.empty(Ordering[Int].reverse)

  /**
   * Add a value to the running median by adding it to the correct heap and rebalancing if necessary.
   * @param value
   */
  def add(value: Int): Unit = {
    val currentMax = maxHeap.headOption.getOrElse(Integer.MIN_VALUE)
    val currentMin = minHeap.headOption.getOrElse(Integer.MAX_VALUE)

    // The sizes of the heaps should always differ by at most 1.
    if (maxHeap.size > minHeap.size) {
      if (value >= currentMax)
        minHeap += value
      else {
        minHeap += maxHeap.dequeue
        maxHeap += value
      }
    } else if (maxHeap.size < minHeap.size) {
      if (value <= currentMin)
        maxHeap += value
      else {
        maxHeap += minHeap.dequeue
        minHeap += value
      }
    } else {
      // Break ties in favour of max heap
      if (value > currentMax)
        minHeap += value
      else
        maxHeap += value
    }
  }

  /**
   * Get the current median.
   */
  def currentMedian: Double = {
    if (minHeap.size > maxHeap.size)
      minHeap.head
    else if (maxHeap.size > minHeap.size)
      maxHeap.head
    else if (maxHeap.isEmpty && minHeap.isEmpty)
      0   // Default vacuous value
    else
      (minHeap.head + maxHeap.head) / 2.0
  }

  /**
   * Dequeue both heaps and returns a tuple of the two element lists, effectively resetting this data structure to a clean state.
   */
  def dequeueAll(): (Seq[Int], Seq[Int]) = (maxHeap.dequeueAll, minHeap.dequeueAll)
}