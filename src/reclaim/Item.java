package reclaim;

import java.time.LocalDate;

/** Shared data and behavior for lost and found reports. */
public abstract class Item {
    /** Lifecycle states used while an item is being returned to its owner. */
    public enum Status {
        LOST, FOUND, CLAIM_PENDING, RETURNED
    }

    // These private fields keep item data encapsulated; the ID and report date do not change.
    private final String itemID;
    private String itemName;
    private String description;
    private String location;
    private final LocalDate dateReported;
    private String reportedBy;
    private Status status;

    /** Initializes the fields common to both kinds of item reports. */
    protected Item(String itemID, String itemName, String description, String location,
                   LocalDate dateReported, String reportedBy, Status status) {
        this.itemID = requireText(itemID, "Item ID");
        this.itemName = requireText(itemName, "Item name");
        this.description = requireText(description, "Description");
        this.location = requireText(location, "Location");
        this.dateReported = requireDate(dateReported, "Date reported");
        this.reportedBy = requireText(reportedBy, "Reporter name");
        this.status = status;
    }

    public abstract String getItemType();

    /** Returns the current status as text for display in the interface. */
    public String getStatus() {
        return status.name();
    }

    /** Formats common item information; subclasses append their own details. */
    public String displayDetails() {
        return "ID: " + itemID + "\nType: " + getItemType() + "\nItem: " + itemName
                + "\nDescription: " + description + "\nLocation: " + location
                + "\nDate reported: " + dateReported + "\nReported by: " + reportedBy
                + "\nStatus: " + getStatus();
    }

    public String getItemID() { return itemID; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDate getDateReported() { return dateReported; }
    public String getReportedBy() { return reportedBy; }
    public Status getStatusValue() { return status; }

    public void setItemName(String itemName) { this.itemName = requireText(itemName, "Item name"); }
    public void setDescription(String description) { this.description = requireText(description, "Description"); }
    public void setLocation(String location) { this.location = requireText(location, "Location"); }
    public void setReportedBy(String reportedBy) { this.reportedBy = requireText(reportedBy, "Reporter name"); }
    void setStatus(Status status) { this.status = status; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private static LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }
}
