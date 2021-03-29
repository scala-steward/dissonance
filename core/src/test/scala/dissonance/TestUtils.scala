package dissonance

import scala.io.Source
import cats.effect.IO

object TestUtils {
  // TODO: Return Stream[IO, String] of lines from file
  def readFileFromResource(path: String): IO[List[String]] = {
    val acquire = IO.shift *> IO(Source.fromURL(getClass.getResource(path)))

    acquire.bracket { in =>
      IO(in.getLines().toList)
    } { in =>
      IO(in.close()).void
    }
  }
}
