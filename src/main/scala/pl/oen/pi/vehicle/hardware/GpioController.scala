package pl.oen.pi.vehicle.hardware

import cats.effect.Effect
import cats.effect.concurrent.Ref
import example.config.Gpio
import cats.implicits._

abstract class GpioController[F[_] : Effect] {

  def conf: Gpio

  def speedRef: Ref[F, Int]

  val speedStep = 50
  val maxSpeed = 1024
  val minSpeed = 0

  def speedUp: F[Int] = setSpeed(_ + _)

  def speedDown: F[Int] = setSpeed(_ - _)

  def rideForward(): F[Unit] = for {
    _ <- stop()
    _ <- simpleRideForward()
  } yield ()

  def rideBackward(): F[Unit] = for {
    _ <- stop()
    _ <- simpleRideBackward()
  } yield ()

  def stop(): F[Unit]

  def shutdown(): F[Unit]

  def turnRight(): F[Unit]

  def turnLeft(): F[Unit]

  protected[this] def simpleRideForward(): F[Unit]

  protected[this] def simpleRideBackward(): F[Unit]

  protected[this] def setGpioSpeed(newSpeed: Int): F[Unit]

  protected[this] def adjustSpeed(v: Int): (Int, Int) = {
    val adjusted =
      if (v >= maxSpeed) maxSpeed
      else if (v <= minSpeed) minSpeed
      else v

    (adjusted, adjusted)
  }

  protected[this] def setSpeed(f: (Int, Int) => Int): F[Int] = for {
      newSpeed <- speedRef.modify(v => adjustSpeed(f(v, speedStep)))
      _ <- setGpioSpeed(newSpeed)
    } yield newSpeed
}

object GpioController {
  def apply[F[_] : Effect](conf: Gpio): F[GpioController[F]] = for {
      speedRef <- Ref.of[F, Int](conf.startSpeed)
    } yield
      if (conf.isDummy) new DummyGpio(conf, speedRef)
      else new HwGpio(conf, speedRef)
}
