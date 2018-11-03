package pl.oen.pi.vehicle.console.simple

import cats.effect.ExitCase.Canceled
import cats.effect._
import cats.effect.syntax.bracket.catsEffectSyntaxBracket
import cats.implicits._
import example.config.AppConfig
import pl.oen.pi.vehicle.hardware.GpioController

object App extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    createMainLoop[IO]()
  }

  def createMainLoop[F[_] : Effect](): F[ExitCode] = {
    for {
      conf <- AppConfig.read[F]()
      gpioController <- GpioController[F](conf.gpio)
      _ <- Effect[F].delay(println("↱ ↰ ↑ ↓ a z <space> q"))
      exitCode <- MainLoop.mainLoop[F](gpioController).guaranteeCase {
        case Canceled =>
          Effect[F].delay(println("Interrupted: releasing and exiting!")).flatMap(_ => gpioController.shutdown())
        case _ =>
          Effect[F].delay(println("Normal exit!"))
      }
    } yield exitCode
  }
}
