package reclaim;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/** Coordinates item and claim rules and stores their records between app launches. */
public final class LostAndFoundManager {
    // In-memory collections are the working records; save() writes item and claim data to disk.
    private final List<Item> items = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<ClaimRecord> claims = new ArrayList<>();
    private final Path dataFile;

    /** Uses a per-user application data file in the user's home directory. */
    public LostAndFoundManager() throws IOException {
        this(Path.of(System.getProperty("user.home"), ".reclaim", "reclaim-data.tsv"));
    }

    LostAndFoundManager(Path dataFile) throws IOException {
        this.dataFile = dataFile.toAbsolutePath();
        load();
    }

    public List<Item> getItems() { return List.copyOf(items); }
    public List<User> getUsers() { return List.copyOf(users); }
    public List<ClaimRecord> getClaims() { return List.copyOf(claims); }

    public String createItemID() { return "ITM-" + shortID(); }
    public String createClaimID() { return "CLM-" + shortID(); }

    public void addItem(Item item) throws IOException {
        if (item == null) throw new IllegalArgumentException("Item is required.");
        if (findItem(item.getItemID()) != null) throw new IllegalArgumentException("That item ID is already in use.");
        items.add(item);
        save();
    }

    public void addUser(User user) {
        if (user == null) throw new IllegalArgumentException("User is required.");
        users.add(user);
    }

    /** Returns items matching the keyword and optional type/status filters. */
    public List<Item> searchItems(String keyword, String type, String status) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(item -> query.isEmpty()
                        || item.getItemID().toLowerCase(Locale.ROOT).contains(query)
                        || item.getItemName().toLowerCase(Locale.ROOT).contains(query)
                        || item.getLocation().toLowerCase(Locale.ROOT).contains(query))
                .filter(item -> type == null || type.equals("All") || item.getItemType().equalsIgnoreCase(type))
                .filter(item -> status == null || status.equals("All")
                        || item.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    /** Updates an item's status while preventing a pending claim from being bypassed. */
    public void updateStatus(String itemID, Item.Status status) throws IOException {
        Item item = requireItem(itemID);
        if (item.getStatusValue() == Item.Status.CLAIM_PENDING) {
            throw new IllegalArgumentException("Resolve the pending claim before changing this item's status.");
        }
        if (status == null || status == Item.Status.CLAIM_PENDING) {
            throw new IllegalArgumentException("Choose a valid item status.");
        }
        item.setStatus(status);
        save();
    }

    /** Creates a pending claim only for a found item that is still available. */
    public ClaimRecord createClaim(String itemID, String claimantName, String contactNumber) throws IOException {
        Item item = requireItem(itemID);
        if (!(item instanceof FoundItem)) throw new IllegalArgumentException("Claims can only be created for found items.");
        if (item.getStatusValue() != Item.Status.FOUND) {
            throw new IllegalArgumentException("This found item is not available for a new claim.");
        }
        ClaimRecord claim = new ClaimRecord(createClaimID(), itemID, claimantName, contactNumber, LocalDate.now());
        claims.add(claim);
        item.setStatus(Item.Status.CLAIM_PENDING);
        save();
        return claim;
    }

    /** Applies the staff decision to both the claim and its associated item. */
    public void processClaim(String claimID, boolean approve) throws IOException {
        ClaimRecord claim = requireClaim(claimID);
        if (claim.getStatus() != ClaimRecord.Status.PENDING) {
            throw new IllegalArgumentException("This claim has already been processed.");
        }
        Item item = requireItem(claim.getItemID());
        if (approve) {
            claim.approveClaim();
            item.setStatus(Item.Status.RETURNED);
        } else {
            claim.rejectClaim();
            item.setStatus(Item.Status.FOUND);
        }
        save();
    }

    private Item findItem(String itemID) {
        return items.stream().filter(item -> item.getItemID().equals(itemID)).findFirst().orElse(null);
    }

    private Item requireItem(String itemID) {
        Item item = findItem(itemID);
        if (item == null) throw new IllegalArgumentException("Item " + itemID + " was not found.");
        return item;
    }

    private ClaimRecord requireClaim(String claimID) {
        return claims.stream().filter(claim -> claim.getClaimID().equals(claimID))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Claim " + claimID + " was not found."));
    }

    /** Rebuilds the in-memory model objects from the saved text records. */
    private void load() throws IOException {
        if (!Files.exists(dataFile)) return;
        int lineNumber = 0;
        for (String line : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) continue;
            try {
                String[] fields = line.split("\t", -1);
                if (fields[0].equals("I") && fields.length == 12) {
                    String type = decode(fields[1]);
                    String id = decode(fields[2]);
                    String name = decode(fields[3]);
                    String description = decode(fields[4]);
                    String location = decode(fields[5]);
                    LocalDate dateReported = LocalDate.parse(decode(fields[6]));
                    String reportedBy = decode(fields[7]);
                    Item.Status status = Item.Status.valueOf(decode(fields[8]));
                    if (type.equals("Lost")) {
                        items.add(new LostItem(id, name, description, location, dateReported, reportedBy,
                                decode(fields[9]), LocalDate.parse(decode(fields[10])), decode(fields[11]), status));
                    } else if (type.equals("Found")) {
                        items.add(new FoundItem(id, name, description, location, dateReported, reportedBy,
                                decode(fields[9]), LocalDate.parse(decode(fields[10])), decode(fields[11]), status));
                    } else {
                        throw new IllegalArgumentException("Unknown item type.");
                    }
                } else if (fields[0].equals("C") && fields.length == 7) {
                    claims.add(new ClaimRecord(decode(fields[1]), decode(fields[2]), decode(fields[3]),
                            decode(fields[4]), LocalDate.parse(decode(fields[6])),
                            ClaimRecord.Status.valueOf(decode(fields[5]))));
                } else {
                    throw new IllegalArgumentException("Unrecognized data record.");
                }
            } catch (RuntimeException ex) {
                throw new IOException("Could not read saved data at line " + lineNumber + " in " + dataFile, ex);
            }
        }
    }

    /** Writes a complete snapshot to a temporary file before replacing the saved file. */
    private void save() throws IOException {
        Path parent = dataFile.getParent();
        if (parent != null) Files.createDirectories(parent);
        List<String> rows = new ArrayList<>();
        for (Item item : items) {
            String extraOne;
            String extraTwo;
            String extraThree;
            if (item instanceof LostItem lost) {
                extraOne = lost.getOwnerName();
                extraTwo = lost.getDateLost().toString();
                extraThree = lost.getLastSeenLocation();
            } else {
                FoundItem found = (FoundItem) item;
                extraOne = found.getFinderName();
                extraTwo = found.getDateFound().toString();
                extraThree = found.getFoundLocation();
            }
            rows.add(join("I", item.getItemType(), item.getItemID(), item.getItemName(),
                    item.getDescription(), item.getLocation(), item.getDateReported().toString(),
                    item.getReportedBy(), item.getStatus(), extraOne, extraTwo, extraThree));
        }
        for (ClaimRecord claim : claims) {
            rows.add(join("C", claim.getClaimID(), claim.getItemID(), claim.getClaimantName(),
                    claim.getContactNumber(), claim.getStatus().name(), claim.getDateCreated().toString()));
        }
        Path temporary = Files.createTempFile(parent, "reclaim-", ".tmp");
        try {
            Files.write(temporary, rows, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Encodes each value so user-entered tabs or line breaks cannot split a record. */
    private static String join(String... fields) {
        return fields[0] + "\t" + java.util.Arrays.stream(fields, 1, fields.length)
                .map(LostAndFoundManager::encode).collect(Collectors.joining("\t"));
    }

    /** Base64 URL encoding keeps each field safe for the tab-separated file format. */
    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /** Reverses the field encoding used by save(). */
    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String shortID() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
