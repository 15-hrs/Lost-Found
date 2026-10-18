package reclaim;

import java.time.LocalDate;

/** Stores one request by a person claiming a found item. */
public final class ClaimRecord {
    /** Claim decisions are separate from the lifecycle status of the item itself. */
    public enum Status { PENDING, APPROVED, REJECTED }

    // The claim and linked item identifiers remain stable after the claim is created.
    private final String claimID;
    private final String itemID;
    private String claimantName;
    private String contactNumber;
    private Status status;
    private final LocalDate dateCreated;

    /** Creates a new claim in the pending state. */
    public ClaimRecord(String claimID, String itemID, String claimantName, String contactNumber,
                       LocalDate dateCreated) {
        this(claimID, itemID, claimantName, contactNumber, dateCreated, Status.PENDING);
    }

    ClaimRecord(String claimID, String itemID, String claimantName, String contactNumber,
                LocalDate dateCreated, Status status) {
        this.claimID = User.requireText(claimID, "Claim ID");
        this.itemID = User.requireText(itemID, "Item ID");
        this.claimantName = User.requireText(claimantName, "Claimant name");
        this.contactNumber = User.requireText(contactNumber, "Contact number");
        if (dateCreated == null) throw new IllegalArgumentException("Claim date is required.");
        this.dateCreated = dateCreated;
        this.status = status;
    }

    /** Changes this claim's decision after review. */
    public void createClaim() { status = Status.PENDING; }
    public void approveClaim() { status = Status.APPROVED; }
    public void rejectClaim() { status = Status.REJECTED; }

    public String getClaimID() { return claimID; }
    public String getItemID() { return itemID; }
    public String getClaimantName() { return claimantName; }
    public String getContactNumber() { return contactNumber; }
    public Status getStatus() { return status; }
    public LocalDate getDateCreated() { return dateCreated; }
}
