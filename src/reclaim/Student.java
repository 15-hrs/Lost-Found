package reclaim;

/** A school student with student-number and section details. */
public final class Student extends User {
    private String studentNumber;
    private String section;

    public Student(String userID, String fullName, String contactNumber, String studentNumber, String section) {
        super(userID, fullName, contactNumber);
        this.studentNumber = requireText(studentNumber, "Student number");
        this.section = requireText(section, "Section");
    }

    @Override
    /** Returns the concrete role for this User subtype. */
    public String getRole() { return "Student"; }

    public String getStudentNumber() { return studentNumber; }
    public String getSection() { return section; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = requireText(studentNumber, "Student number"); }
    public void setSection(String section) { this.section = requireText(section, "Section"); }
}
