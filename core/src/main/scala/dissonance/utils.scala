package dissonance

import java.net.http.HttpClient

import cats.effect._
import cats.syntax.all._

import scala.concurrent.duration._
import cats.effect.Temporal

object utils {
  def putStrLn[F[_]: Sync](s: String): F[Unit] = Sync[F].delay(println(s))

  def fakeResource[F[_]: Async](i: Int, duration: FiniteDuration) =
    Resource.make {
      putStrLn(s"Acquiring Resource $i...") >> Temporal[F].sleep(duration) >> putStrLn(s"Acquired Resource $i")
    } { _ => putStrLn(s"Releasing Resource $i...") >> Temporal[F].sleep(duration) >> putStrLn(s"Released Resource $i") }

  def javaClient[F[_]: Sync]: F[HttpClient] =
    Sync[F].delay {
      val builder = HttpClient.newBuilder()
      // workaround for https://github.com/http4s/http4s-jdk-http-client/issues/200
      if (Runtime.version().feature() == 11) {
        val params = javax.net.ssl.SSLContext.getDefault().getDefaultSSLParameters()
        params.setProtocols(params.getProtocols().filter(_ != "TLSv1.3"))
        builder.sslParameters(params)
      }
      builder.build()
    }
}
