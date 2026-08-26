package de.bydora.tes.command.debug;

/**
 * The result of a {@code /debug dump}: a short chat-friendly {@code summary} (e.g.
 * {@code "9x4, 12 Items"}) and the full {@code plainText} dump copied to the tester's clipboard.
 */
public record ContainerDump(String summary, String plainText) {
}
