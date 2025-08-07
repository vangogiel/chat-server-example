package io.vangogiel.chat.infrastructure.db

case class DbConfig(
    user: String,
    password: String,
    dbUrl: String,
    connectionPoolSize: Int
)
