package pl.oen.pi.vehicle.hardware

import cats.effect.IO
import cats.effect.concurrent.Ref
import example.config.Gpio

trait GpioController {
  def conf: Gpio

  def speedRef: Ref[IO, Int]

  val speedStep = 50
  val maxSpeed = 1024
  val minSpeed = 0

  def speedUp: IO[Int] = setSpeed(_ + _)

  def speedDown: IO[Int] = setSpeed(_ - _)

  def rideForward(): IO[Unit] = for {
    _ <- stop()
    _ <- simpleRideForward()
  } yield ()

  def rideBackward(): IO[Unit] = for {
    _ <- stop()
    _ <- simpleRideBackward()
  } yield ()

  def stop(): IO[Unit]

  def shutdown(): IO[Unit]

  def turnRight(): IO[Unit]

  def turnLeft(): IO[Unit]

  protected[this] def simpleRideForward(): IO[Unit]

  protected[this] def simpleRideBackward(): IO[Unit]

  protected[this] def setGpioSpeed(newSpeed: Int): IO[Unit]

  protected[this] def adjustSpeed(v: Int): (Int, Int) = {
    val adjusted =
      if (v >= maxSpeed) maxSpeed
      else if (v <= minSpeed) minSpeed
      else v

    (adjusted, adjusted)
  }

  protected[this] def setSpeed(f: (Int, Int) => Int): IO[Int] = for {
      newSpeed <- speedRef.modify(v => adjustSpeed(f(v, speedStep)))
      _ <- setGpioSpeed(newSpeed)
    } yield newSpeed
}

object GpioController {
  def apply(conf: Gpio): IO[GpioController] = for {
      speedRef <- Ref.of[IO, Int](conf.startSpeed)
    } yield
      if (conf.isDummy) new DummyGpio(conf, speedRef)
      else new HwGpio(conf, speedRef)
}
