
package my.edu.taylors.ap.memory;

import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileChatMemoryStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsMessages() {
        FileChatMemoryStore store = new FileChatMemoryStore(tempDir);

        store.updateMessages("user1", List.of(UserMessage.from("Hello")));

        assertEquals(
                List.of(UserMessage.from("Hello")),
                store.getMessages("user1")
        );
    }

    @Test
    void messagesSurviveStoreRestart() {
        FileChatMemoryStore firstStore = new FileChatMemoryStore(tempDir);
        firstStore.updateMessages("user1", List.of(UserMessage.from("Remember this")));

        FileChatMemoryStore restartedStore = new FileChatMemoryStore(tempDir);

        assertEquals(
                List.of(UserMessage.from("Remember this")),
                restartedStore.getMessages("user1")
        );
    }

    @Test
    void keepsDifferentUsersSeparate() {
        FileChatMemoryStore store = new FileChatMemoryStore(tempDir);

        store.updateMessages("user1", List.of(UserMessage.from("Message one")));
        store.updateMessages("user2", List.of(UserMessage.from("Message two")));

        assertEquals(List.of(UserMessage.from("Message one")), store.getMessages("user1"));
        assertEquals(List.of(UserMessage.from("Message two")), store.getMessages("user2"));
    }

    @Test
    void deletesUserMessages() {
        FileChatMemoryStore store = new FileChatMemoryStore(tempDir);

        store.updateMessages("user1", List.of(UserMessage.from("Delete me")));
        store.deleteMessages("user1");

        assertTrue(store.getMessages("user1").isEmpty());
    }

    @Test
    void corruptedFileIsQuarantinedAndReturnsEmptyMessages() throws Exception {
        FileChatMemoryStore store = new FileChatMemoryStore(tempDir);

        Path file = store.fileFor("broken-user");
        Files.writeString(file, "this is not valid JSON");

        assertTrue(store.getMessages("broken-user").isEmpty());
        assertFalse(Files.exists(file));

        try (var files = Files.list(tempDir)) {
            assertTrue(files.anyMatch(path ->
                    path.getFileName().toString().contains(".corrupt-")
            ));
        }
    }

    @Test
    void userIdsCannotEscapeMemoryDirectory() {
        FileChatMemoryStore store = new FileChatMemoryStore(tempDir);

        Path file = store.fileFor("../../outside");

        assertEquals(tempDir.toAbsolutePath().normalize(),
                file.toAbsolutePath().normalize().getParent());
    }

    @Test
    void differentUserIdsHaveDifferentFiles() {
        FileChatMemoryStore store = new FileChatMemoryStore(tempDir);

        assertNotEquals(
                store.fileFor("user/a"),
                store.fileFor("user_a")
        );
    }
}
