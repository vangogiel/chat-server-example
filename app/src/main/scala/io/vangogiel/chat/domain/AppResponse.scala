package io.vangogiel.chat.domain

case class AppResponse(correlationId: String, payload: CommandResponse)
