package reclaim;

/** A staff user who can review whether a claim is awaiting a decision. */
public final class Staff extends User {
    private String employeeID;
    private String department;

    public Staff(String userID, String fullName, String contactNumber, String employeeID, String department) {
        super(userID, fullName, contactNumber);
        this.employeeID = requireText(employeeID, "Employee ID");
        this.department = requireText(department, "Department");
    }

    @Override
    /** Returns the concrete role for this User subtype. */
    public String getRole() { return "Staff"; }

    /** A pending claim is eligible for staff review. */
    public boolean verifyClaim(ClaimRecord claim) {
        return claim != null && claim.getStatus() == ClaimRecord.Status.PENDING;
    }

    public String getEmployeeID() { return employeeID; }
    public String getDepartment() { return department; }
    public void setEmployeeID(String employeeID) { this.employeeID = requireText(employeeID, "Employee ID"); }
    public void setDepartment(String department) { this.department = requireText(department, "Department"); }
}
