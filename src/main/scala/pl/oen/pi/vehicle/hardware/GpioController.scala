package pl.oen.pi.vehicle.hardware

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import example.config.Gpio
import pl.oen.pi.vehicle.hardware.GpioController._

abstract class GpioController[F[_] : Effect] {

  def conf: Gpio

  def stateRef: Ref[F, State]

  def turningCS: ContextShift[IO]

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

  def stop(): F[Unit] = for {
    _ <- stateRef.update(s => s.copy(turningStatus = TurningNone))
    _ <- simpleStop()
  } yield ()

  def shutdown(): F[Unit] = for {
    _ <- stop()
    _ <- simpleShutdown()
  } yield()

  def turnRight(): F[Unit] = for {
    _ <- stateRef.update(s => s.copy(turningStatus = TurningRight))
    _ <- scheduleTurningInAsync(TurningRight, simpleTurnRight())
  } yield ()

  def turnLeft(): F[Unit] = for {
    _ <- stateRef.update(s => s.copy(turningStatus = TurningLeft))
    _ <- scheduleTurningInAsync(TurningLeft, simpleTurnLeft())
  } yield ()

  protected[this] def simpleShutdown(): F[Unit]

  protected[this] def simpleTurnRight(): F[Unit]

  protected[this] def simpleTurnLeft(): F[Unit]

  protected[this] def simpleRideForward(): F[Unit]

  protected[this] def simpleRideBackward(): F[Unit]

  protected[this] def setGpioSpeed(newSpeed: Int): F[Unit]

  protected[this] def simpleStop(): F[Unit]

  private[this] def adjustSpeed(v: Int): Int = {
    if (v >= maxSpeed) maxSpeed
    else if (v <= minSpeed) minSpeed
    else v
  }

  private[this] def modifySpeed(state: State, op: (Int, Int) => Int): (State, Int) = {
    val rawNewSpeed = op(state.speed, speedStep)
    val adjusted = adjustSpeed(rawNewSpeed)
    (state.copy(speed = adjusted), adjusted)
  }

  private[this] def setSpeed(op: (Int, Int) => Int): F[Int] = for {
    newSpeed <- stateRef.modify(state => modifySpeed(state, op))
    _ <- setGpioSpeed(newSpeed)
  } yield newSpeed

  private[this] def scheduleTurningInAsync(tStatus: TurningStatus, simpleTurn: => F[Unit]): F[Unit] = {
    Effect[F].toIO({
      for {
        state <- stateRef.get
        _ <- if (state.turningStatus == tStatus) simpleTurn else Effect[F].unit
      } yield ()
    }).start(turningCS).to[F].map(_ => Unit)
  }
}

object GpioController {
  def apply[F[_] : Effect](conf: Gpio, turningCS: ContextShift[IO]): F[GpioController[F]] = for {
    stateRef <- Ref.of[F, State](State(conf.startSpeed))
    controller = {
      if (conf.isDummy) new DummyGpio(conf, stateRef, turningCS)
      else new HwGpio(conf, stateRef, turningCS)
    }
  } yield controller

  sealed trait TurningStatus
  case object TurningLeft extends TurningStatus
  case object TurningRight extends TurningStatus
  case object TurningNone extends TurningStatus

  case class State(speed: Int, turningStatus: TurningStatus = TurningNone)
}
