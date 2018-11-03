package pl.oen.pi.vehicle.console.simple

import cats.effect.ExitCase.Canceled
import cats.effect._
import example.config.AppConfig
import jline.console.{ConsoleReader, KeyMap, Operation}
import pl.oen.pi.vehicle.hardware.GpioController

object MainApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    def mainLoop(gpioController: GpioController): IO[ExitCode] = {
      val reader = new ConsoleReader()
      val km = KeyMap.keyMaps().get("vi-insert")

      def loop(n: Long): IO[ExitCode] = {
        IO().flatMap { _ =>
          val c = reader.readBinding(km)
          val k: Either[Operation, String] =
            if (c == Operation.SELF_INSERT) Right(reader.getLastBinding)
            else Left(c match { case op: Operation => op })

          k match {
            case Right("q") => gpioController.shutdown().flatMap(_ => IO.pure(ExitCode.Success))
            case Left(Operation.VI_EOF_MAYBE) => IO.pure(ExitCode.Success)
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
                    _ <- IO(println("stop!"))
                    _ <- gpioController.stop()
                  } yield ()

                case Left(Operation.FORWARD_CHAR) =>
                  for {
                    _ <- IO(print("↱"))
                    _ <- gpioController.turnRight()
                  } yield ()

                case Left(Operation.BACKWARD_CHAR) =>
                  for {
                    _ <- IO(print("↰"))
                    _ <- gpioController.turnLeft()
                  } yield ()

                case Left(Operation.PREVIOUS_HISTORY) =>
                  for {
                    _ <- IO(print("↑"))
                    _ <- gpioController.rideForward()
                  } yield ()

                case Left(Operation.NEXT_HISTORY) =>
                  for {
                    _ <- IO(print("↓"))
                    _ <- gpioController.rideBackward()
                  } yield ()

                case _ => IO(println("unknown command: " + k))
              }
              res.flatMap(_ => loop(n + 1))
          }
        }
      }

      loop(0)
    }

    for {
      conf <- AppConfig.read[IO]()
      gpioController <- GpioController(conf.gpio)
      _ <- IO(println("↱ ↰ ↑ ↓ a z <space> q"))
      exitCode <- mainLoop(gpioController).guaranteeCase {
        case Canceled =>
          IO(println("Interrupted: releasing and exiting!")).flatMap(_ => gpioController.shutdown())
        case _ =>
          IO(println("Normal exit!"))
      }
    } yield exitCode
  }
}

