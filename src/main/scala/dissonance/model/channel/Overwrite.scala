package dissonance.model.channel

import dissonance.model.Event.Snowflake
import dissonance.model.Permission
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

case class Overwrite(
    id: Snowflake,
    `type`: OverwriteType,
    allow: List[Permission],
    deny: List[Permission]
)

object Overwrite {
  implicit val config: Configuration                = Configuration.default.withSnakeCaseMemberNames
  implicit val overwriteDecoder: Decoder[Overwrite] = deriveConfiguredDecoder
}
