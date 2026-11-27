package my.edu.taylors.ap.scenario.textbooks;

import my.edu.taylors.ap.data.JsonStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextbookToolsTest {

    @TempDir
    Path tempDir;

    private TextbookTools createTools() throws Exception {
        JsonStore<Listing> listings =
                JsonStore.of(tempDir.resolve("listings.json"), Listing.class);

        JsonStore<Interest> interests =
                JsonStore.of(tempDir.resolve("interests.json"), Interest.class);

        Listing listing = new Listing(
                "TX-0001",
                "1234567890",
                "SELLER-1",
                "",
                "",
                "B",
                5600,
                ""
        );

        listings.append(listing);

        TextbookRules rules = new TextbookRules(9, 20, 500, 4);

        TextbookService service = new TextbookService(
                List.of(),
                rules,
                Clock.systemUTC()
        );

        return new TextbookTools(service, listings, interests);
    }

    @Test
    void expressingInterestSavesRecord() throws Exception {
        TextbookTools tools = createTools();

        String result = tools.expressInterest("BUYER-1", "TX-0001");

        assertTrue(result.contains("has been recorded"));

        JsonStore<Interest> interests =
                JsonStore.of(tempDir.resolve("interests.json"), Interest.class);

        List<Interest> saved = interests.findAll();

        assertEquals(1, saved.size());
        assertEquals("TX-0001", saved.get(0).listingId());
        assertEquals("BUYER-1", saved.get(0).buyerId());
    }

    @Test
    void expressingInterestTwiceDoesNotCreateDuplicate() throws Exception {
        TextbookTools tools = createTools();

        tools.expressInterest("BUYER-1", "TX-0001");
        String secondResult = tools.expressInterest("BUYER-1", "TX-0001");

        assertTrue(secondResult.contains("already expressed interest"));

        JsonStore<Interest> interests =
                JsonStore.of(tempDir.resolve("interests.json"), Interest.class);

        assertEquals(1, interests.findAll().size());
    }

    @Test
    void expressingInterestInUnknownListingIsRejected() throws Exception {
        TextbookTools tools = createTools();

        assertThrows(
                TextbookExceptions.UnknownListingException.class,
                () -> tools.expressInterest("BUYER-1", "TX-9999")
        );
    }
}