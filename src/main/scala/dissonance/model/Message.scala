package dissonance.model

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import dissonance.model.user.User

case class Message(
    content: String,
    channelId: String,
    author: User
)

object Message {
  implicit val config: Configuration            = Configuration.default.withSnakeCaseMemberNames
  implicit val messageDecoder: Decoder[Message] = deriveConfiguredDecoder
}