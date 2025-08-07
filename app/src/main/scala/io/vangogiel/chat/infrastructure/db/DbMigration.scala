package io.vangogiel.chat.infrastructure.db

import cats.effect.Async
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

trait DbMigration {
  def migrate[F[_]: Async](jdbcUrl: String, user: String, password: String): F[MigrateResult] =
    Async[F].pure {
      Flyway
        .configure()
        .dataSource(jdbcUrl, user, password)
        .load()
        .migrate()
    }
}
