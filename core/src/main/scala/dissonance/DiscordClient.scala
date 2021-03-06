package dissonance

import java.io.File

import cats.Applicative
import cats.effect._
import cats.syntax.all._
import dissonance.Discord._
import dissonance.DiscordClient.WebhookMessage
import dissonance.data._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.http4s.Method._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.client.jdkhttpclient.JdkHttpClient
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{Request, Status, Uri}

class DiscordClient[F[_]: Async](token: String, client: Client[F]) {

  def sendMessage(message: String, channelId: Snowflake, tts: Boolean = false): F[Message] =
    client
      .fetchAs[Message](
        Request[F]()
          .withMethod(POST)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/messages"))
          .withEntity(
            // TODO Case class here
            Json.obj(
              "content" -> message.asJson,
              "tts"     -> tts.asJson
            )
          )
          .putHeaders(headers(token))
      )

  def deleteMessage(channelId: Snowflake, messageId: Snowflake): F[Unit] =
    client
      .expect[Unit](
        Request[F]()
          .withMethod(DELETE)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/messages/$messageId"))
          .putHeaders(headers(token))
      )
      .handleErrorWith(_ => Applicative[F].unit) // Throws: java.io.IOException: unexpected content length header with 204 response

  def sendEmbed(embed: Embed, channelId: Snowflake): F[Message] =
    client
      .fetchAs[Message](
        Request[F]()
          .withMethod(POST)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/messages"))
          .withEntity(
            // TODO Case class here
            Json.obj("embed" -> embed.asJson)
          )
          .putHeaders(headers(token))
      )

  def sendInteractionResponse(interactionResponse: InteractionResponse, interactionId: Snowflake, interactionToken: String): F[Unit] =
    client
      .expect[Unit](
        Request[F]()
          .withMethod(POST)
          .withUri(apiEndpoint.addPath(s"interactions/$interactionId/$interactionToken/callback"))
          .withEntity(interactionResponse)
      )
      .handleErrorWith(_ => Applicative[F].unit) // Throws: java.io.IOException: unexpected content length header with 204 response

  def sendEmbedWithFileImage(embed: Embed, file: File, channelId: Snowflake): F[Message] = {
    val multipart = Multipart[F](
      Vector(
        Part.fileData[F]("file", file),
        Part.formData("payload_json", Json.obj("embed" -> embed.withImage(Image(Some(Uri.unsafeFromString(s"attachment://${file.getName}")), None, None, None)).asJson).noSpaces)
      )
    )
    client
      .fetchAs[Message](
        Request[F]()
          .withMethod(POST)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/messages"))
          .withEntity(multipart)
          .putHeaders(multipart.headers.headers, headers(token))
      )
  }

  def sendFile(file: File, channelId: Snowflake): F[Message] = {
    val multipart = Multipart[F](Vector(Part.fileData[F]("file", file)))
    client
      .fetchAs[Message](
        Request[F]()
          .withMethod(POST)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/messages"))
          .withEntity(multipart)
          .putHeaders(multipart.headers.headers, headers(token))
      )
  }

  def createReaction(channelId: Snowflake, messageId: Snowflake, emoji: String): F[Unit] =
    client
      .expect[Unit](
        Request[F]()
          .withMethod(PUT)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/messages/$messageId/reactions/$emoji/@me"))
          .putHeaders(headers(token))
      )
      .handleErrorWith(_ => Applicative[F].unit) // Throws: java.io.IOException: unexpected content length header with 204 response

  def addEmoji(guildId: Snowflake, name: String, emojiData: Array[Byte], roles: List[Snowflake] = Nil): F[Emoji] = {
    val imageData = "data:;base64," + fs2.Stream.emits(emojiData).through(fs2.text.base64.encode).compile.foldMonoid
    client
      .fetchAs[Emoji](
        Request[F]()
          .withMethod(POST)
          .withUri(apiEndpoint.addPath(s"guilds/$guildId/emojis"))
          .withEntity(
            // TODO: Create case class for this
            Json.obj(
              "name"  -> name.asJson,
              "image" -> imageData.asJson,
              "roles" -> roles.asJson
            )
          )
          .putHeaders(headers(token))
      )
  }

  def listEmojis(guildId: Snowflake): F[List[Emoji]] =
    client
      .fetchAs[List[Emoji]](
        Request[F]()
          .withMethod(GET)
          .withUri(apiEndpoint.addPath(s"guilds/$guildId/emojis"))
          .putHeaders(headers(token))
      )

  def getChannelMessage(channelId: Snowflake, messageId: Snowflake): F[Message] =
    client
      .fetchAs[Message](
        Request[F]()
          .withMethod(GET)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/messages/$messageId"))
          .putHeaders(headers(token))
      )

  def createWebhook(name: String, avatar: Option[ImageDataUri], channelId: Snowflake): F[Webhook] =
    client
      .fetchAs[Webhook](
        Request[F]()
          .withMethod(POST)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/webhooks"))
          .withEntity( // TODO Case class here
            Json.obj(
              "name"   -> name.asJson,
              "avatar" -> avatar.asJson
            )
          )
          .putHeaders(headers(token))
      )

  def getChannelWebhooks(channelId: Snowflake): F[List[Webhook]] =
    client
      .fetchAs[List[Webhook]](
        Request[F]()
          .withMethod(GET)
          .withUri(apiEndpoint.addPath(s"channels/$channelId/webhooks"))
          .putHeaders(headers(token))
      )

  def getGuildWebhooks(guildId: Snowflake): F[List[Webhook]] =
    client
      .fetchAs[List[Webhook]](
        Request[F]()
          .withMethod(GET)
          .withUri(apiEndpoint.addPath(s"guilds/$guildId/webhooks"))
          .putHeaders(headers(token))
      )

  def getWebhook(webhookId: Snowflake): F[Webhook] =
    client
      .fetchAs[Webhook](
        Request[F]()
          .withMethod(GET)
          .withUri(apiEndpoint.addPath(s"webhooks/$webhookId"))
          .putHeaders(headers(token))
      )

  def getWebhookWithToken(webhookId: Snowflake, token: String): F[Webhook] =
    client
      .fetchAs[Webhook](
        Request[F]()
          .withMethod(GET)
          .withUri(apiEndpoint.addPath(s"webhooks/$webhookId/$token"))
      )

  def modifyWebhook(webhookId: Snowflake, name: Option[String], avatar: Option[ImageDataUri], channelId: Option[Snowflake]): F[Webhook] =
    client
      .fetchAs[Webhook](
        Request[F]()
          .withMethod(PATCH)
          .withUri(apiEndpoint.addPath(s"webhooks/$webhookId"))
          .withEntity(
            // TODO Case class here
            Json.obj(
              "name"       -> name.asJson,
              "avatar"     -> avatar.asJson,
              "channel_id" -> channelId.asJson
            )
          )
          .putHeaders(headers(token))
      )

  def modifyWebhookWithToken(webhookId: Snowflake, name: Option[String], avatar: Option[ImageDataUri], channelId: Option[Snowflake], token: String): F[Webhook] =
    client
      .fetchAs[Webhook](
        Request[F]()
          .withMethod(PATCH)
          .withUri(apiEndpoint.addPath(s"webhooks/$webhookId/$token"))
          .withEntity(
            // TODO Case class here
            Json.obj(
              "name"       -> name.asJson,
              "avatar"     -> avatar.asJson,
              "channel_id" -> channelId.asJson
            )
          )
      )

  def deleteWebhook(webhookId: Snowflake): F[Status] =
    client
      .status(
        Request[F]()
          .withMethod(DELETE)
          .withUri(apiEndpoint.addPath(s"webhooks/$webhookId"))
          .putHeaders(headers(token))
      )

  def deleteWebhookWithToken(webhookId: Snowflake, token: String): F[Status] =
    client
      .status(
        Request[F]()
          .withMethod(DELETE)
          .withUri(apiEndpoint.addPath(s"webhooks/$webhookId/$token"))
      )

  def executeWebhookWithResponse(webhook: Webhook, webhookMessage: WebhookMessage): F[Message] =
    client
      .fetchAs[Message](
        createExecuteWebhookRequest(webhook, webhookMessage, wait = true)
      )

  def executeWebhook(webhook: Webhook, webhookMessage: WebhookMessage): F[Status] =
    client
      .status(
        createExecuteWebhookRequest(webhook, webhookMessage, wait = false)
      )

  // TODO: Handle uploading files which requires multipart/form-data
  private def createExecuteWebhookRequest(webhook: Webhook, webhookMessage: WebhookMessage, wait: Boolean): Request[F] =
    Request[F]()
      .withMethod(POST)
      .withUri(
        apiEndpoint
          .addPath(s"webhooks/${webhook.id}/${webhook.token.get}")
          .withQueryParam("wait", wait)
      )
      .withEntity(webhookMessage)
      .putHeaders(headers(token))

  // TODO: Add Slack and Github Webhooks
}

object DiscordClient {
  def make[F[_]: Async](token: String): Resource[F, DiscordClient[F]] =
    for {
      javaHttpClient <- Resource.eval(utils.javaClient)
      javaClient     <- JdkHttpClient[F](javaHttpClient)
    } yield new DiscordClient(token, javaClient)

  type AllowedMentions = Unit // TODO: Implement this

  case class WebhookMessage(
      content: String,
      username: Option[String],
      avatarUrl: Option[String],
      tts: Option[Boolean],
      file: Unit,          // TODO: Figure out what this is supposed to be
      embeds: List[Embed], // TODO: Configure this to be max length of 10
      payloadJson: Unit,   // TODO: Figure out what this is supposed to be
      allowedMentions: AllowedMentions
  )

  object WebhookMessage {
    implicit val config: Configuration            = Configuration.default.withSnakeCaseMemberNames
    implicit val encoder: Encoder[WebhookMessage] = deriveConfiguredEncoder
  }
}
