package com.lexho.user_service.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class SubmitReviewRequest {

    @NotBlank(message = "bookingId is required")
    private String bookingId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 500, message = "reviewText must be at most 500 characters")
    private String reviewText;

    @Size(max = 500, message = "reviewText must be at most 500 characters")
    private String reviewComment;

    private List<String> reviewImages;

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId == null ? null : bookingId.toString();
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public void setReviewImages(List<String> reviewImages) {
        this.reviewImages = reviewImages;
    }
}
