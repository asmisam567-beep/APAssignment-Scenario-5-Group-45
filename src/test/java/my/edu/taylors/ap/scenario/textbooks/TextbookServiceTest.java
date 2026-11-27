
package my.edu.taylors.ap.scenario.textbooks;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextbookServiceTest {

    @Test
    void suggestedPriceAppliesGroup45Rules() throws Exception {

        TextbookRules rules = new TextbookRules(9, 20, 500, 4);

        Clock clock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        TextbookService service = new TextbookService(
                List.of(), rules, clock
        );

        Book book = new Book(
                "1234567890",
                "Test Textbook",
                1,
                2024,
                10000,
                "9876543210"
        );

        long result = service.suggestedSen(book, "B");

        // Expected suggested price = RM56.00
        assertEquals(5600L, result);
    }

    @Test
    void suggestedPriceNeverFallsBelowMinimum() throws Exception {

        TextbookRules rules = new TextbookRules(9, 20, 500, 4);

        Clock clock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        TextbookService service = new TextbookService(
                List.of(), rules, clock
        );

        Book book = new Book(
                "1111111111",
                "Very Old Textbook",
                1,
                1900,
                1000,
                null
        );

        long result = service.suggestedSen(book, "D");

        // Minimum suggested price = RM5.00
        assertEquals(500L, result);
    }

    @Test
    void askingPriceAtMaximumMarkupIsAccepted() throws Exception {

        TextbookRules rules = new TextbookRules(9, 20, 500, 4);

        Clock clock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        TextbookService service = new TextbookService(
                List.of(), rules, clock
        );

        Book book = new Book(
                "1234567890",
                "Test Textbook",
                1,
                2024,
                10000,
                "9876543210"
        );

        // Suggested price = RM56.00
        // Maximum asking price = RM67.20
        assertDoesNotThrow(() ->
                service.validateListing(book, "B", 6720, null)
        );
    }

    @Test
    void askingPriceAboveMaximumMarkupIsRejected() throws Exception {

        TextbookRules rules = new TextbookRules(9, 20, 500, 4);

        Clock clock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        TextbookService service = new TextbookService(
                List.of(), rules, clock
        );

        Book book = new Book(
                "1234567890",
                "Test Textbook",
                1,
                2024,
                10000,
                "9876543210"
        );

        // Maximum = RM67.20, so RM67.21 must be rejected.
        assertThrows(
                TextbookExceptions.OverpricedListingException.class,
                () -> service.validateListing(book, "B", 6721, null)
        );
    }

    @Test
    void buyerTotalIncludesFourPercentPlatformFee() {

        TextbookRules rules = new TextbookRules(9, 20, 500, 4);

        Clock clock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        TextbookService service = new TextbookService(
                List.of(), rules, clock
        );

        // Asking price = RM100.00
        // Platform fee = 4% = RM4.00
        // Buyer pays = RM104.00

        long result = service.buyerTotalSen(10000);

        assertEquals(10400L, result);
    }

    @Test
    void conditionDWithDefectsDescriptionIsAccepted() throws Exception {

        TextbookRules rules = new TextbookRules(9, 20, 500, 4);

        Clock clock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        TextbookService service = new TextbookService(
                List.of(), rules, clock
        );

        Book book = new Book(
                "1234567890",
                "Test Textbook",
                1,
                2024,
                10000,
                "9876543210"
        );

        // Condition D maximum asking price = RM31.20.
        // RM30.00 is within the limit and defects are disclosed.
        assertDoesNotThrow(() ->
                service.validateListing(
                        book, "D", 3000, "Torn cover and handwritten notes"
                )
        );
    }

    @Test
    void conditionDWithoutDefectsDescriptionIsRejected() throws Exception {

        TextbookRules rules = new TextbookRules(9, 20, 500, 4);

        Clock clock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        TextbookService service = new TextbookService(
                List.of(), rules, clock
        );

        Book book = new Book(
                "1234567890",
                "Test Textbook",
                1,
                2024,
                10000,
                "9876543210"
        );

        assertThrows(
                TextbookExceptions.MissingDisclosureException.class,
                () -> service.validateListing(book, "D", 3000, null)
        );
    }

    @Test
    void zeroAskingPriceIsRejected() throws Exception {
        TextbookService service = createService();
        Book book = createBook();

        assertThrows(TextbookExceptions.InvalidPriceException.class,
                () -> service.validateListing(book, "B", 0, null));
    }

    @Test
    void negativeAskingPriceIsRejected() throws Exception {
        TextbookService service = createService();
        Book book = createBook();

        assertThrows(TextbookExceptions.InvalidPriceException.class,
                () -> service.validateListing(book, "B", -100, null));
    }

    @Test
    void parseRinggitRejectsZeroAndNegativeAmounts() {
        assertThrows(TextbookExceptions.InvalidPriceException.class,
                () -> TextbookService.parseRinggit("0.00"));
        assertThrows(TextbookExceptions.InvalidPriceException.class,
                () -> TextbookService.parseRinggit("-10.00"));
    }

    @Test
    void parseRinggitRejectsMalformedAmount() {
        assertThrows(TextbookExceptions.InvalidPriceException.class,
                () -> TextbookService.parseRinggit("not-a-price"));
    }

    @Test
    void allConditionMultipliersAreAppliedCorrectly() throws Exception {
        TextbookService service = createService();

        Book book = new Book(
                "1234567890", "Test Book", 1, 2026, 10000, null
        );

        assertEquals(10000L, service.suggestedSen(book, "A"));
        assertEquals(8500L, service.suggestedSen(book, "B"));
        assertEquals(6500L, service.suggestedSen(book, "C"));
        assertEquals(4000L, service.suggestedSen(book, "D"));
    }

    @Test
    void supersededBookReceivesAdditionalTwentyPercentReduction() throws Exception {
        TextbookService service = createService();

        Book currentBook = new Book(
                "1234567890", "Current Book", 1, 2026, 10000, null
        );

        Book supersededBook = new Book(
                "1111111111", "Older Book", 1, 2026, 10000, "1234567890"
        );

        assertEquals(10000L, service.suggestedSen(currentBook, "A"));
        assertEquals(8000L, service.suggestedSen(supersededBook, "A"));
    }

    @Test
    void suggestedPriceRoundsDownToNearestFiftySen() throws Exception {
        TextbookService service = createService();

        Book book = new Book(
                "1234567890", "Test Book", 1, 2026, 10003, null
        );

        assertEquals(10000L, service.suggestedSen(book, "A"));
    }

    @Test
    void buyerFeeRoundsFractionalSenHalfUp() {
        TextbookService service = createService();

        // 4% of 13 sen = 0.52 sen, rounded to 1 sen.
        assertEquals(14L, service.buyerTotalSen(13));

        // 4% of 12 sen = 0.48 sen, rounded to 0 sen.
        assertEquals(12L, service.buyerTotalSen(12));
    }

    @Test
    void conditionDRejectsBlankDefectsDescription() {
        TextbookService service = createService();
        Book book = createBook();

        assertThrows(
                TextbookExceptions.MissingDisclosureException.class,
                () -> service.validateListing(book, "D", 3000, "   ")
        );
    }


    private TextbookService createService() {
        TextbookRules rules = new TextbookRules(9, 20, 500, 4);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        return new TextbookService(List.of(), rules, clock);
    }

    private Book createBook() {
        return new Book("1234567890", "Test Textbook", 1, 2024, 10000, "9876543210");
    }

}
