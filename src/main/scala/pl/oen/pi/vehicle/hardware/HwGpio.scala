package pl.oen.pi.vehicle.hardware

import cats.effect.IO
import cats.effect.concurrent.Ref
import com.pi4j.io.gpio.{GpioFactory, PinState, RaspiPin}
import example.config.Gpio

class HwGpio(val conf: Gpio, val speedRef: Ref[IO, Int]) extends GpioController {

  import HwGpio._

  private[this] val gpio = GpioFactory.getInstance()

  private[this] val motorForward = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(conf.pinForward), "forward", PinState.LOW)
  private[this] val motorBackward = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(conf.pinBackward), "backward", PinState.LOW)
  private[this] val speedController = gpio.provisionPwmOutputPin(RaspiPin.getPinByAddress(conf.pinSpeed), "speed", conf.startSpeed)

  private[this] val motorPins = List(
    gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(conf.stepMotor.pin01), "motor01", PinState.LOW),
    gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(conf.stepMotor.pin02), "motor02", PinState.LOW),
    gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(conf.stepMotor.pin03), "motor03", PinState.LOW),
    gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(conf.stepMotor.pin04), "motor04", PinState.LOW)
  )

  (motorPins ++ List(
    motorForward,
    motorBackward,
    speedController
  )).foreach(p => p.setShutdownOptions(true, PinState.LOW))

  override def stop(): IO[Unit] = IO {
    motorForward.setState(PinState.LOW)
    motorBackward.setState(PinState.LOW)
  }

  override def shutdown(): IO[Unit] = IO(gpio.shutdown())

  override protected[this] def simpleRideForward(): IO[Unit] = IO(motorForward.setState(PinState.HIGH))

  override protected[this] def simpleRideBackward(): IO[Unit] = IO(motorBackward.setState(PinState.HIGH))

  override protected[this] def setGpioSpeed(newSpeed: Int): IO[Unit] = IO(speedController.setPwm(newSpeed))

  override def turnRight(): IO[Unit] = IO {
    (0 until stepCount).foreach { _ =>
      smSequencesReversed.foreach { smSeq =>
        smSeq.zip(motorPins).foreach(v => v._2.setState(v._1))
        Thread.sleep(2)
      }
    }
  }

  override def turnLeft(): IO[Unit] = IO {
    (0 until stepCount).foreach { _ =>
      smSequences.foreach { smSeq =>
        smSeq.zip(motorPins).foreach(v => v._2.setState(v._1))
        Thread.sleep(2)
      }
    }
  }
}

object HwGpio {
  val smSequences = List(
    List(PinState.HIGH, PinState.LOW, PinState.LOW, PinState.LOW),
    List(PinState.HIGH, PinState.HIGH, PinState.LOW, PinState.LOW),
    List(PinState.LOW, PinState.HIGH, PinState.LOW, PinState.LOW),
    List(PinState.LOW, PinState.HIGH, PinState.HIGH, PinState.LOW),
    List(PinState.LOW, PinState.LOW, PinState.HIGH, PinState.LOW),
    List(PinState.LOW, PinState.LOW, PinState.HIGH, PinState.HIGH),
    List(PinState.LOW, PinState.LOW, PinState.LOW, PinState.LOW),
    List(PinState.HIGH, PinState.LOW, PinState.LOW, PinState.HIGH)
  )

  val smSequencesReversed = smSequences.reverse
  val stepCount = 10
}