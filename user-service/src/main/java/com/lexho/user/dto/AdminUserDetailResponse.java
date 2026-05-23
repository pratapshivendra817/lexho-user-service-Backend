package com.lexho.user.dto;

public class AdminUserDetailResponse extends AdminUserResponse {
    private BookingSummaryDto bookingSummary;

    public BookingSummaryDto getBookingSummary() {
        return bookingSummary;
    }

    public void setBookingSummary(BookingSummaryDto bookingSummary) {
        this.bookingSummary = bookingSummary;
    }
}
