package com.capstone.contractmanagement.responses;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ChangeDateResponse {
    private String date;

    public ChangeDateResponse(LocalDate localDate) {
        this.date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE); // "yyyy-MM-dd"
    }

    // Getter
    public String getDate() {
        return date;
    }
}