
package my.edu.taylors.ap.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class FileChatMemoryStore implements ChatMemoryStore {

    private final Path dir;

    public FileChatMemoryStore(Path dir) {
        this.dir = dir;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create memory directory " + dir, e);
        }
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Path file = fileFor(memoryId);

        if (!Files.exists(file)) {
            return new ArrayList<>();
        }

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return new ArrayList<>(ChatMessageDeserializer.messagesFromJson(json));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read memory for " + memoryId, e);
        } catch (RuntimeException e) {
            quarantine(file);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Path file = fileFor(memoryId);
        Path tmp = null;

        try {
            tmp = Files.createTempFile(dir, "mem", ".tmp");

            Files.writeString(
                    tmp,
                    ChatMessageSerializer.messagesToJson(messages),
                    StandardCharsets.UTF_8
            );

            try {
                Files.move(
                        tmp,
                        file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save memory for " + memoryId, e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // Best effort cleanup
                }
            }
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            Files.deleteIfExists(fileFor(memoryId));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete memory for " + memoryId, e);
        }
    }

    Path fileFor(Object memoryId) {
        String userId = String.valueOf(memoryId);

        String safe = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(userId.getBytes(StandardCharsets.UTF_8));

        return dir.resolve("user-" + safe + ".json");
    }

    private void quarantine(Path file) {
        try {
            Files.move(
                    file,
                    file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis()),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException ignored) {
            // Best effort
        }
    }
}
