package com.hotel.model;

public class Customer {

    /** Suggestions for the nationality box; any value can still be typed in. */
    public static final String[] COMMON_NATIONALITIES = {
            "Sierra Leonean", "Guinean", "Liberian", "Nigerian", "Ghanaian", "Gambian",
            "Ivorian", "Senegalese", "British", "American", "Chinese", "Lebanese", "Other"
    };

    private int id;
    private String fullName = "";
    private String phone = "";
    private String email = "";
    private String idNumber = "";
    private String nationality = "";

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = nullToEmpty(fullName); }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = nullToEmpty(phone); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = nullToEmpty(email); }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = nullToEmpty(idNumber); }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nullToEmpty(nationality); }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return fullName + " (" + phone + ")";
    }
}
