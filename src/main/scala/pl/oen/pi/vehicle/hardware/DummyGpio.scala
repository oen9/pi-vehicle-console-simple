package pl.oen.pi.vehicle.hardware

import cats.effect.IO
import cats.effect.concurrent.Ref
import example.config.Gpio

class DummyGpio(val conf: Gpio, val speedRef: Ref[IO, Int]) extends GpioController {
  override def stop(): IO[Unit] = IO(println("motor stopped"))

  override def shutdown(): IO[Unit] = IO(println("gpio.shutdown()"))

  override def simpleRideForward(): IO[Unit] = IO(println("Brum brum forward!"))

  override def simpleRideBackward(): IO[Unit] = IO(println("Brum brum backward!"))

  override protected[this] def setGpioSpeed(newSpeed: Int): IO[Unit] = IO.pure()

  override def turnRight(): IO[Unit] = IO(println("Turning right!"))

  override def turnLeft(): IO[Unit] = IO(println("Turning left!"))
}
