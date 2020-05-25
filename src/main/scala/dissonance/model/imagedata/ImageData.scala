package dissonance.model.imagedata

case class ImageData(
    imageContentType: ImageContentType,
    base64EncodedData: String
) {
  def toImageDataUri: ImageDataUri = ImageDataUri(s"data:image/$imageContentType;base64,$base64EncodedData")
}
