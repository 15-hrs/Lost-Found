package reclaim;

import java.time.LocalDate;

/** An item report describing something that has been found. */
public final class FoundItem extends Item {
    // Details that apply to found reports but not to lost-item reports.
    private String finderName;
    private LocalDate dateFound;
    private String foundLocation;

    public FoundItem(String itemID, String itemName, String description, String location,
                     LocalDate dateReported, String reportedBy, String finderName,
                     LocalDate dateFound, String foundLocation) {
        super(itemID, itemName, description, location, dateReported, reportedBy, Status.FOUND);
        this.finderName = requireText(finderName, "Finder name");
        this.dateFound = requireDate(dateFound, "Date found");
        this.foundLocation = requireText(foundLocation, "Found location");
    }

    FoundItem(String itemID, String itemName, String description, String location,
              LocalDate dateReported, String reportedBy, String finderName,
              LocalDate dateFound, String foundLocation, Status status) {
        super(itemID, itemName, description, location, dateReported, reportedBy, status);
        this.finderName = requireText(finderName, "Finder name");
        this.dateFound = requireDate(dateFound, "Date found");
        this.foundLocation = requireText(foundLocation, "Found location");
    }

    @Override
    /** Identifies this subclass when the manager handles an Item reference. */
    public String getItemType() { return "Found"; }

    @Override
    /** Adds the finder and found-location information to the shared item details. */
    public String displayDetails() {
        return super.displayDetails() + "\nFinder: " + finderName + "\nDate found: " + dateFound
                + "\nFound at: " + foundLocation;
    }

    public String getFinderName() { return finderName; }
    public LocalDate getDateFound() { return dateFound; }
    public String getFoundLocation() { return foundLocation; }

    public void updateDetails(String finderName, LocalDate dateFound, String foundLocation) {
        this.finderName = requireText(finderName, "Finder name");
        this.dateFound = requireDate(dateFound, "Date found");
        this.foundLocation = requireText(foundLocation, "Found location");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    private static LocalDate requireDate(LocalDate value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required.");
        return value;
    }
}
