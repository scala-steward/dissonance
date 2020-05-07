package dissonance

import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype

package object model {
  // TODO: Implement
  type Snowflake = Long
  // TODO: newtypes for all kinds of Snowflakes
  @newtype case class DiscordId(value: Snowflake) // TODO: Change to Snowflake

  object DiscordId {
    implicit val idDecoder: Decoder[DiscordId] = Decoder[Long].map(DiscordId.apply)
    implicit val idEncoder: Encoder[DiscordId] = Encoder[Long].contramap(_.value)
  }
  // TODO: newtypes for all plain Strings
  @newtype case class AccountName(value: String)
}
