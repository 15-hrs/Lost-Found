package reclaim;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.Serial;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;

/** Swing user interface for browsing reports and managing lost-item claims. */
public final class ReClaimApp extends JFrame {
    @Serial
    private static final long serialVersionUID = 1L;

    // The manager owns application rules and records; table models adapt them for Swing tables.
    private final LostAndFoundManager manager;
    private final ItemTableModel itemTableModel = new ItemTableModel();
    private final ClaimTableModel claimTableModel = new ClaimTableModel();
    private final JTable itemTable = new JTable(itemTableModel);
    private final JTable claimTable = new JTable(claimTableModel);
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"All", "Lost", "Found"});
    private final JComboBox<String> statusFilter =
            new JComboBox<>(new String[]{"All", "LOST", "FOUND", "CLAIM_PENDING", "RETURNED"});
    private final JLabel dashboardSummary = new JLabel();

    /** Creates the main window and populates its tables from the manager. */
    private ReClaimApp(LostAndFoundManager manager) {
        super("ReClaim | Lost Item Management System");
        this.manager = manager;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 580));
        setSize(1120, 700);
        setLocationRelativeTo(null);
        buildInterface();
        refreshTables();
    }

    private void buildInterface() {
        JPanel header = new JPanel(new BorderLayout(12, 4));
        header.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        JLabel title = new JLabel("ReClaim");
        title.setFont(title.getFont().deriveFont(25f));
        header.add(title, BorderLayout.WEST);
        dashboardSummary.setHorizontalAlignment(JLabel.RIGHT);
        header.add(dashboardSummary, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Items", buildItemsPanel());
        tabs.addTab("Claims", buildClaimsPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildItemsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Search (ID, item, location):"));
        filters.add(searchField);
        filters.add(new JLabel("Type:"));
        filters.add(typeFilter);
        filters.add(new JLabel("Status:"));
        filters.add(statusFilter);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(event -> refreshItems());
        filters.add(searchButton);
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(event -> {
            searchField.setText("");
            typeFilter.setSelectedItem("All");
            statusFilter.setSelectedItem("All");
            refreshItems();
        });
        filters.add(clearButton);
        panel.add(filters, BorderLayout.NORTH);

        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.setFillsViewportHeight(true);
        panel.add(new JScrollPane(itemTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addAction(actions, "Report lost item", () -> openReportForm(false));
        addAction(actions, "Register found item", () -> openReportForm(true));
        addAction(actions, "View details", this::showSelectedItem);
        addAction(actions, "Update status", this::updateSelectedStatus);
        addAction(actions, "Create claim", this::createClaim);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildClaimsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        claimTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        claimTable.setFillsViewportHeight(true);
        panel.add(new JScrollPane(claimTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addAction(actions, "View item details", this::showSelectedClaimItem);
        addAction(actions, "Process selected claim", this::processSelectedClaim);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private static void addAction(JPanel panel, String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    /** Collects form input, validates dates, and creates the correct Item subtype. */
    private void openReportForm(boolean found) {
        JTextField name = new JTextField();
        JTextArea description = new JTextArea(3, 22);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JTextField location = new JTextField();
        JTextField reportedBy = new JTextField();
        JTextField dateReported = new JTextField(LocalDate.now().toString());
        JTextField person = new JTextField();
        JTextField eventDate = new JTextField(LocalDate.now().toString());
        JTextField eventLocation = new JTextField();

        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 7));
        fields.add(new JLabel("Item name *")); fields.add(name);
        fields.add(new JLabel("Description *")); fields.add(new JScrollPane(description));
        fields.add(new JLabel(found ? "Found location *" : "Current/last known location *")); fields.add(location);
        fields.add(new JLabel("Reported by *")); fields.add(reportedBy);
        fields.add(new JLabel("Date reported (YYYY-MM-DD) *")); fields.add(dateReported);
        fields.add(new JLabel(found ? "Finder name *" : "Owner name *")); fields.add(person);
        fields.add(new JLabel(found ? "Date found (YYYY-MM-DD) *" : "Date lost (YYYY-MM-DD) *")); fields.add(eventDate);
        fields.add(new JLabel(found ? "Specific found location *" : "Last-seen location *")); fields.add(eventLocation);

        int result = JOptionPane.showConfirmDialog(this, fields,
                found ? "Register found item" : "Report lost item",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            LocalDate reportDate = parseDate(dateReported.getText(), "Date reported");
            LocalDate specificDate = parseDate(eventDate.getText(), found ? "Date found" : "Date lost");
            String id = manager.createItemID();
            Item item;
            // Both subclasses share the common fields, then add their own type-specific details.
            if (found) {
                item = new FoundItem(id, name.getText(), description.getText(), location.getText(),
                        reportDate, reportedBy.getText(), person.getText(), specificDate, eventLocation.getText());
            } else {
                item = new LostItem(id, name.getText(), description.getText(), location.getText(),
                        reportDate, reportedBy.getText(), person.getText(), specificDate, eventLocation.getText());
            }
            manager.addItem(item);
            refreshTables();
            JOptionPane.showMessageDialog(this, "Report saved. Item ID: " + id, "Report submitted",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            showError(ex.getMessage());
        } catch (java.io.IOException ex) {
            showError("Could not save the report: " + ex.getMessage());
        }
    }

    /** Submits a claim for the currently selected found item. */
    private void createClaim() {
        Item item = selectedItem();
        if (item == null) return;
        JTextField claimant = new JTextField();
        JTextField contact = new JTextField();
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Claimant name *")); fields.add(claimant);
        fields.add(new JLabel("Contact number *")); fields.add(contact);
        int result = JOptionPane.showConfirmDialog(this, fields, "Claim " + item.getItemName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            ClaimRecord claim = manager.createClaim(item.getItemID(), claimant.getText(), contact.getText());
            refreshTables();
            JOptionPane.showMessageDialog(this, "Claim " + claim.getClaimID()
                    + " submitted for staff review.", "Claim submitted", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (java.io.IOException ex) {
            showError("Could not save the claim: " + ex.getMessage());
        }
    }

    /** Lets the user choose a valid next status for the selected item. */
    private void updateSelectedStatus() {
        Item item = selectedItem();
        if (item == null) return;
        String[] choices = item instanceof LostItem
                ? new String[]{"LOST", "FOUND", "RETURNED"}
                : new String[]{"FOUND", "RETURNED"};
        JComboBox<String> statuses = new JComboBox<>(choices);
        statuses.setSelectedItem(item.getStatus());
        int result = JOptionPane.showConfirmDialog(this, statuses, "Update item status",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            manager.updateStatus(item.getItemID(), Item.Status.valueOf((String) statuses.getSelectedItem()));
            refreshTables();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (java.io.IOException ex) {
            showError("Could not save the status: " + ex.getMessage());
        }
    }

    /** Confirms and applies an approve/reject decision to the selected pending claim. */
    private void processSelectedClaim() {
        int row = claimTable.getSelectedRow();
        if (row < 0) {
            showError("Select a claim first.");
            return;
        }
        ClaimRecord claim = claimTableModel.getClaim(row);
        if (claim.getStatus() != ClaimRecord.Status.PENDING) {
            showError("This claim has already been processed.");
            return;
        }
        Object[] choices = {"Approve", "Reject", "Cancel"};
        int result = JOptionPane.showOptionDialog(this,
                "Process claim " + claim.getClaimID() + " for " + claim.getClaimantName() + "?",
                "Review claim", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, choices, choices[0]);
        if (result < 0 || result == 2) return;
        try {
            manager.processClaim(claim.getClaimID(), result == 0);
            refreshTables();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (java.io.IOException ex) {
            showError("Could not save the claim decision: " + ex.getMessage());
        }
    }

    private void showSelectedItem() {
        Item item = selectedItem();
        if (item != null) JOptionPane.showMessageDialog(this, item.displayDetails(), "Item details",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSelectedClaimItem() {
        int row = claimTable.getSelectedRow();
        if (row < 0) {
            showError("Select a claim first.");
            return;
        }
        String itemID = claimTableModel.getClaim(row).getItemID();
        manager.getItems().stream().filter(item -> item.getItemID().equals(itemID)).findFirst()
                .ifPresentOrElse(item -> JOptionPane.showMessageDialog(this, item.displayDetails(),
                                "Claimed item details", JOptionPane.INFORMATION_MESSAGE),
                        () -> showError("The item associated with this claim could not be found."));
    }

    private Item selectedItem() {
        int row = itemTable.getSelectedRow();
        if (row < 0) {
            showError("Select an item first.");
            return null;
        }
        return itemTableModel.getItem(row);
    }

    /** Refreshes the visible tables and summary after records change. */
    private void refreshTables() {
        refreshItems();
        claimTableModel.setClaims(manager.getClaims());
        long lost = manager.getItems().stream().filter(item -> item instanceof LostItem).count();
        long found = manager.getItems().stream().filter(item -> item instanceof FoundItem).count();
        dashboardSummary.setText("Lost: " + lost + "    Found: " + found
                + "    Claims: " + manager.getClaims().size());
    }

    private void refreshItems() {
        itemTableModel.setItems(manager.searchItems(searchField.getText(),
                (String) typeFilter.getSelectedItem(), (String) statusFilter.getSelectedItem()));
    }

    /** Converts the form's ISO date text to a LocalDate or reports a useful validation error. */
    private static LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must use YYYY-MM-DD format.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "ReClaim", JOptionPane.ERROR_MESSAGE);
    }

    /** Starts Swing on its event-dispatch thread and shows an error if startup fails. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                LostAndFoundManager manager = new LostAndFoundManager();
                new ReClaimApp(manager).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "ReClaim could not start: " + ex.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /** Adapts Item objects into the columns displayed by the item JTable. */
    private static final class ItemTableModel extends AbstractTableModel {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String[] columns = {"Item ID", "Type", "Item", "Location", "Date reported", "Reported by", "Status"};
        private List<Item> items = List.of();

        void setItems(List<Item> items) {
            this.items = List.copyOf(items);
            fireTableDataChanged();
        }

        Item getItem(int row) { return items.get(row); }
        @Override public int getRowCount() { return items.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            Item item = items.get(row);
            return switch (column) {
                case 0 -> item.getItemID();
                case 1 -> item.getItemType();
                case 2 -> item.getItemName();
                case 3 -> item.getLocation();
                case 4 -> item.getDateReported();
                case 5 -> item.getReportedBy();
                case 6 -> item.getStatus();
                default -> "";
            };
        }
    }

    /** Adapts ClaimRecord objects into the columns displayed by the claims JTable. */
    private static final class ClaimTableModel extends AbstractTableModel {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String[] columns = {"Claim ID", "Item ID", "Claimant", "Contact", "Date", "Status"};
        private List<ClaimRecord> claims = List.of();

        void setClaims(List<ClaimRecord> claims) {
            this.claims = List.copyOf(claims);
            fireTableDataChanged();
        }

        ClaimRecord getClaim(int row) { return claims.get(row); }
        @Override public int getRowCount() { return claims.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            ClaimRecord claim = claims.get(row);
            return switch (column) {
                case 0 -> claim.getClaimID();
                case 1 -> claim.getItemID();
                case 2 -> claim.getClaimantName();
                case 3 -> claim.getContactNumber();
                case 4 -> claim.getDateCreated();
                case 5 -> claim.getStatus();
                default -> "";
            };
        }
    }
}
