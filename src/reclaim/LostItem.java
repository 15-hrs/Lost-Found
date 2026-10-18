package reclaim;

import java.time.LocalDate;

/** An item report describing something that has been lost. */
public final class LostItem extends Item {
    // Details that apply to lost reports but not to found-item reports.
    private String ownerName;
    private LocalDate dateLost;
    private String lastSeenLocation;

    public LostItem(String itemID, String itemName, String description, String location,
                    LocalDate dateReported, String reportedBy, String ownerName,
                    LocalDate dateLost, String lastSeenLocation) {
        super(itemID, itemName, description, location, dateReported, reportedBy, Status.LOST);
        this.ownerName = requireText(ownerName, "Owner name");
        this.dateLost = requireDate(dateLost, "Date lost");
        this.lastSeenLocation = requireText(lastSeenLocation, "Last-seen location");
    }

    LostItem(String itemID, String itemName, String description, String location,
             LocalDate dateReported, String reportedBy, String ownerName,
             LocalDate dateLost, String lastSeenLocation, Status status) {
        super(itemID, itemName, description, location, dateReported, reportedBy, status);
        this.ownerName = requireText(ownerName, "Owner name");
        this.dateLost = requireDate(dateLost, "Date lost");
        this.lastSeenLocation = requireText(lastSeenLocation, "Last-seen location");
    }

    @Override
    /** Identifies this subclass when the manager handles an Item reference. */
    public String getItemType() { return "Lost"; }

    @Override
    /** Adds the owner and last-seen information to the shared item details. */
    public String displayDetails() {
        return super.displayDetails() + "\nOwner: " + ownerName + "\nDate lost: " + dateLost
                + "\nLast seen: " + lastSeenLocation;
    }

    public String getOwnerName() { return ownerName; }
    public LocalDate getDateLost() { return dateLost; }
    public String getLastSeenLocation() { return lastSeenLocation; }

    public void updateDetails(String ownerName, LocalDate dateLost, String lastSeenLocation) {
        this.ownerName = requireText(ownerName, "Owner name");
        this.dateLost = requireDate(dateLost, "Date lost");
        this.lastSeenLocation = requireText(lastSeenLocation, "Last-seen location");
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
