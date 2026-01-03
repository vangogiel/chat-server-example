package io.vangogiel.chat.domain

case class AppRequest(correlationId: String, payload: CommandRequest)
