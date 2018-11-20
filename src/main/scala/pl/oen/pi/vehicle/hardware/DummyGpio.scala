package pl.oen.pi.vehicle.hardware

import cats.effect.concurrent.Ref
import cats.effect.{Effect, _}
import example.config.Gpio
import pl.oen.pi.vehicle.hardware.GpioController.State

class DummyGpio[F[_] : Effect](val conf: Gpio, val stateRef: Ref[F, State], val turningCS: ContextShift[IO]) extends GpioController[F] {

  override def shutdown(): F[Unit] = Effect[F].delay(println("gpio.shutdown()"))

  override def simpleRideForward(): F[Unit] = Effect[F].delay(println("Brum brum forward!"))

  override def simpleRideBackward(): F[Unit] = Effect[F].delay(println("Brum brum backward!"))

  override def simpleStop(): F[Unit] = Effect[F].delay(println("motor stopped"))

  override protected[this] def setGpioSpeed(newSpeed: Int): F[Unit] = Effect[F].unit

  override def simpleTurnRight(): F[Unit] = Effect[F].delay({
    println("Turning right!")
    Thread.sleep(1000)
  })

  override def simpleTurnLeft(): F[Unit] = Effect[F].delay({
    println("Turning left!")
    Thread.sleep(1000)
  })
}
