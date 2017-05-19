package org.spixi.flink.handson

object VerifyPartitioner {

  def main(args: Array[String]): Unit = {

    val one = scala.io.Source.fromFile("/Users/aspina/Desktop/partition/1").getLines().map(_.toInt).toSet
    val two = scala.io.Source.fromFile("/Users/aspina/Desktop/partition/2").getLines().map(_.toInt).toSet

    one.foreach { el =>
      println(el)
      assert(!two.contains(el), "Partition doesn't work")
    }

    println("Partition works")
  }
}
