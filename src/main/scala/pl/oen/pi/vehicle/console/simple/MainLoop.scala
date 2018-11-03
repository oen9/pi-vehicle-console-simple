package pl.oen.pi.vehicle.console.simple

import cats.effect.{Effect, ExitCode}
import jline.console.{ConsoleReader, KeyMap, Operation}
import pl.oen.pi.vehicle.hardware.GpioController
import cats.implicits._

object MainLoop {

  def mainLoop[F[_] : Effect](gpioController: GpioController[F]): F[ExitCode] = {
    val reader = new ConsoleReader()
    val km = KeyMap.keyMaps().get("vi-insert")

    def loop(): F[ExitCode] = {
      Effect[F].unit.flatMap { _ =>
        val c = reader.readBinding(km)
        val k: Either[Operation, String] =
          if (c == Operation.SELF_INSERT) Right(reader.getLastBinding)
          else Left(c match { case op: Operation => op })

        k match {
          case Right("q") => gpioController.shutdown().flatMap(_ => Effect[F].delay(ExitCode.Success))
          case Left(Operation.VI_EOF_MAYBE) => Effect[F].delay(ExitCode.Success)
          case _ =>
            val res = k match {
              case Right("a") =>
                for {
                  speed <- gpioController.speedUp
                } yield println("incresed speed: " + speed)

              case Right("z") =>
                for {
                  speed <- gpioController.speedDown
                } yield println("decreased speed: " + speed)

              case Right(" ") =>
                for {
                  _ <- Effect[F].delay(println("stop!"))
                  _ <- gpioController.stop()
                } yield ()

              case Left(Operation.FORWARD_CHAR) =>
                for {
                  _ <- Effect[F].delay(print("↱"))
                  _ <- gpioController.turnRight()
                } yield ()

              case Left(Operation.BACKWARD_CHAR) =>
                for {
                  _ <- Effect[F].delay(print("↰"))
                  _ <- gpioController.turnLeft()
                } yield ()

              case Left(Operation.PREVIOUS_HISTORY) =>
                for {
                  _ <- Effect[F].delay(print("↑"))
                  _ <- gpioController.rideForward()
                } yield ()

              case Left(Operation.NEXT_HISTORY) =>
                for {
                  _ <- Effect[F].delay(print("↓"))
                  _ <- gpioController.rideBackward()
                } yield ()

              case _ => Effect[F].delay(println("unknown command: " + k))
            }
            res.flatMap(_ => loop())
        }
      }
    }

    loop()
  }
}
