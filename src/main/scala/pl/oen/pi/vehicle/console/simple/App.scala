package pl.oen.pi.vehicle.console.simple

import java.util.concurrent.Executors

import cats.effect.ExitCase.Canceled
import cats.effect._
import cats.effect.syntax.bracket.catsEffectSyntaxBracket
import cats.implicits._
import example.config.AppConfig
import pl.oen.pi.vehicle.hardware.GpioController

import scala.concurrent.ExecutionContext

object App extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    createSingleThreadContextShift[IO].use(cs => createMainLoop[IO](cs))
  }

  def createMainLoop[F[_] : Effect](turningCS: ContextShift[IO]): F[ExitCode] = {
    for {
      conf <- AppConfig.read[F]()
      gpioController <- GpioController[F](conf.gpio, turningCS)
      _ <- Effect[F].delay(println("↱ ↰ ↑ ↓ a z <space> q"))
      exitCode <- MainLoop.mainLoop[F](gpioController).guaranteeCase {
        case Canceled =>
          Effect[F].delay(println("Interrupted: releasing and exiting!")).flatMap(_ => gpioController.shutdown())
        case _ =>
          Effect[F].delay(println("Normal exit!"))
      }
    } yield exitCode
  }

  def createSingleThreadContextShift[F[_] : Effect]: Resource[F, ContextShift[IO]] = {
    Resource[F, ContextShift[IO]](
      Effect[F].delay {
        val executor = Executors.newSingleThreadExecutor()
        val ec = ExecutionContext.fromExecutor(executor)
        val ioContextShift = IO.contextShift(ec)
        (ioContextShift, Effect[F].delay(executor.shutdown()))
      }
    )
  }
}
