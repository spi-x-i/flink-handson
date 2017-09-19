package org.spixi

import org.apache.flink.ml.math.DenseVector
import org.spixi.flink.generation.models.Pixel

package object flink {
  implicit class VectorConverter(pixel: Pixel) {
    def toVector = DenseVector(pixel.g, pixel.r)
  }
}
