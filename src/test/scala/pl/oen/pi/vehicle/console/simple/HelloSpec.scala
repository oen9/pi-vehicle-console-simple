package pl.oen.pi.vehicle.console.simple

import org.scalatest._

class HelloSpec extends FlatSpec with Matchers {
  "The Hello object" should "say hello" in {
    "hello" shouldEqual "hello"
  }
}
