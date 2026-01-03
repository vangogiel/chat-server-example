package io.vangogiel.chat.application

import io.vangogiel.chat.domain.{AppRequest, AppResponse}

trait MessageStreamHandler[F[_]] {
  def processStream(request: fs2.Stream[F, AppRequest]): fs2.Stream[F, AppResponse]
}
