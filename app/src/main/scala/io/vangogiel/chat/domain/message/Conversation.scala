package io.vangogiel.chat.domain.message

case class Conversation(conversationId: ConversationId, messages: List[Message])
