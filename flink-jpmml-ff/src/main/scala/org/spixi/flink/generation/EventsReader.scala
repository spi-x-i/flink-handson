package org.spixi.flink.generation

import org.spixi.flink.generation.models.Pixel

object EventsReader {

  def fromSource(modelPath: String): List[Pixel] = {

    val source =
      scala.io.Source
        .fromInputStream(getClass.getResourceAsStream(modelPath))
        .getLines()
        .toList
        .tail
        .map(_.split(",").toList)

    val out = source
      .collect {
        case red :: green :: clazz :: Nil =>
          Pixel(red.toInt, green.toInt, clazz.toInt, "", System.currentTimeMillis())
      }
    scala.util.Random.shuffle(out)
  }

}
