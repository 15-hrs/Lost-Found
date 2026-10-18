package reclaim;

/** Common identity and contact information shared by student and staff users. */
public abstract class User {
    // Private fields demonstrate encapsulation; the ID is fixed after construction.
    private final String userID;
    private String fullName;
    private String contactNumber;

    /** Validates and stores the details common to every user. */
    protected User(String userID, String fullName, String contactNumber) {
        this.userID = requireText(userID, "User ID");
        this.fullName = requireText(fullName, "Full name");
        this.contactNumber = requireText(contactNumber, "Contact number");
    }

    /** Each concrete user type supplies its own role. */
    public abstract String getRole();

    /** Provides a simple base action that user subclasses can use. */
    public String submitReport() {
        return fullName + " submitted a report.";
    }

    public String getUserID() { return userID; }
    public String getFullName() { return fullName; }
    public String getContactNumber() { return contactNumber; }
    public void setFullName(String fullName) { this.fullName = requireText(fullName, "Full name"); }
    public void setContactNumber(String contactNumber) { this.contactNumber = requireText(contactNumber, "Contact number"); }

    /** Rejects missing values and trims extra spaces before storing user input. */
    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }
}
