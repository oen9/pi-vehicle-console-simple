package pl.oen.pi.vehicle.hardware

import cats.effect.Effect
import cats.effect.concurrent.Ref
import example.config.Gpio

class DummyGpio[F[_]: Effect](val conf: Gpio, val speedRef: Ref[F, Int]) extends GpioController[F] {
  override def stop(): F[Unit] = Effect[F].delay(println("motor stopped"))

  override def shutdown(): F[Unit] = Effect[F].delay(println("gpio.shutdown()"))

  override def simpleRideForward(): F[Unit] = Effect[F].delay(println("Brum brum forward!"))

  override def simpleRideBackward(): F[Unit] = Effect[F].delay(println("Brum brum backward!"))

  override protected[this] def setGpioSpeed(newSpeed: Int): F[Unit] = Effect[F].unit

  override def turnRight(): F[Unit] = Effect[F].delay(println("Turning right!"))

  override def turnLeft(): F[Unit] = Effect[F].delay(println("Turning left!"))
}
