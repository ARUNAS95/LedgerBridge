package dev.ledgerbridge;
import jakarta.validation.constraints.*;
public record Transfer(@NotBlank @Size(max=80) String reference, @Positive long amountMinor, @Pattern(regexp="[A-Z]{3}") @NotNull String currency) {}
