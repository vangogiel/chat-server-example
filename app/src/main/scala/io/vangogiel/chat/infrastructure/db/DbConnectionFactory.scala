package io.vangogiel.chat.infrastructure.db

import cats.effect.{Async, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor

import scala.concurrent.duration.MINUTES

object DbConnectionFactory {
  def createTransactor[F[_]: Async](dbConfig: DbConfig): Resource[F, HikariTransactor[F]] =
    for {
      hikariConfig <- Resource.pure {
        val config = new HikariConfig()
        config.setDriverClassName("org.postgresql.Driver")
        config.setJdbcUrl(dbConfig.dbUrl)
        config.setUsername(dbConfig.user)
        config.setPassword(dbConfig.password)
        config.setMaximumPoolSize(dbConfig.connectionPoolSize)
        config.setConnectionTimeout(MINUTES.toMillis(1L))
        config.setMaxLifetime(MINUTES.toMillis(10))
        config
      }
      xa <- HikariTransactor.fromHikariConfig[F](hikariConfig)
    } yield xa
}
