package io.vangogiel.chat.domain.conversation

import io.vangogiel.chat.domain.message.Message

case class Conversation(conversationId: ConversationId, messages: List[Message])
