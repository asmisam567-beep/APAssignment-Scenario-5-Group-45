package my.edu.taylors.ap.scenario.textbooks;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.textbooks.TextbookExceptions.UnknownListingException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Textbook exchange tools.
 * Seller contact details are never returned by these tools (T5).
 */
public class TextbookTools {

    private final TextbookService service;
    private final JsonStore<Listing> listings;
    private final JsonStore<Interest> interests;

    public TextbookTools(
            TextbookService service,
            JsonStore<Listing> listings,
            JsonStore<Interest> interests
    ) {
        this.service = service;
        this.listings = listings;
        this.interests = interests;
    }

    @Tool("Search the textbook catalogue by title keyword or ISBN.")
    public String searchBooks(@P("title keyword or ISBN") String keyword) {
        List<Book> hits = service.search(keyword);

        return hits.isEmpty()
                ? "No books match '" + keyword + "'."
                : hits.stream()
                .map(b -> b.isbn() + " " + b.title()
                        + " (ed. " + b.edition() + ", " + b.year() + ")"
                        + (b.superseded() ? " [superseded]" : ""))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Suggested resale price for a book in a given condition grade (A, B, C or D).")
    public String suggestPrice(
            @P("ISBN-13") String isbn,
            @P("condition grade A, B, C or D") String condition
    ) throws RuleViolationException {

        Book b = service.book(isbn);
        long suggested = service.suggestedSen(b, condition);

        return b.title() + " grade "
                + TextbookService.normaliseCondition(condition)
                + ": suggested " + Money.rm(suggested)
                + ", maximum asking "
                + Money.rm(service.maxAskingSen(suggested)) + ".";
    }

    @Tool("Show listings for a book with asking price and what the buyer pays including the platform fee.")
    public String findListings(
            @P("ISBN-13") String isbn
    ) throws RuleViolationException, DataAccessException {

        Book b = service.book(isbn);
        List<Listing> hits = listings.findWhere(l -> l.isbn().equals(b.isbn()));

        return hits.isEmpty()
                ? "No listings for " + b.title() + "."
                : hits.stream()
                .map(l -> l.listingId()
                        + " grade " + l.condition()
                        + " asking " + Money.rm(l.askingSen())
                        + " -> buyer pays "
                        + Money.rm(service.buyerTotalSen(l.askingSen()))
                        + (l.defects() == null || l.defects().isBlank()
                        ? ""
                        : " (defects: " + l.defects() + ")"))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Create a listing for the current user. Returns the listing ID.")
    public String createListing(
            @ToolMemoryId String sellerId,
            @P("ISBN-13") String isbn,
            @P("condition grade A, B, C or D") String condition,
            @P("asking price in ringgit, e.g. 45.00") String askingPriceRm,
            @P("defects description; empty if none") String defects
    ) throws RuleViolationException, DataAccessException {

        Book b = service.book(isbn);
        long asking = TextbookService.parseRinggit(askingPriceRm);

        service.validateListing(b, condition, asking, defects);

        List<Listing> all = listings.findAll();

        Listing listing = new Listing(
                String.format("TX-%04d", all.size() + 1),
                b.isbn(),
                sellerId,
                "",
                "",
                TextbookService.normaliseCondition(condition),
                asking,
                defects == null ? "" : defects
        );

        listings.append(listing);

        return "Listing " + listing.listingId()
                + " created at " + Money.rm(asking) + ".";
    }

    @Tool("Record a buyer's interest in a textbook listing. Seller contact details are never shared.")
    public String expressInterest(
            @ToolMemoryId String buyerId,
            @P("listing ID, e.g. TX-0003") String listingId
    ) throws RuleViolationException, DataAccessException {

        Listing listing = listings.findWhere(
                        x -> x.listingId().equalsIgnoreCase(listingId)
                ).stream()
                .findFirst()
                .orElseThrow(() -> new UnknownListingException(listingId));

        boolean alreadyRecorded = interests.findWhere(
                interest -> interest.listingId().equalsIgnoreCase(listing.listingId())
                        && Objects.equals(interest.buyerId(), buyerId)
        ).stream().findAny().isPresent();

        if (alreadyRecorded) {
            return "You have already expressed interest in "
                    + listing.listingId()
                    + ". Your interest is recorded on the platform.";
        }

        interests.append(new Interest(listing.listingId(), buyerId));

        return "Your interest in " + listing.listingId()
                + " has been recorded. Arrange any meet-up through the platform.";
    }
}