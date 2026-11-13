package com.ulearn.accounts.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A user's credential, stored only as a salted one-way BCrypt hash. It is a one-to-one extension of
 * {@link UserEntity}: {@code user_id} is the primary key and a foreign key to {@code users}, so a
 * user has at most one credential. Kept separate so password material has its own lifecycle
 * (ADR-0008; {@code user-accounts} spec, "Password storage").
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "credentials")
public class CredentialEntity {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public CredentialEntity(UUID userId, String passwordHash) {
    this.userId = userId;
    this.passwordHash = passwordHash;
    this.updatedAt = Instant.now();
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
    this.updatedAt = Instant.now();
  }
}
