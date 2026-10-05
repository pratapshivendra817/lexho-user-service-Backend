package com.lexho.user_service.lexhopartner.service;

final class PartnerServiceText {

    static final String AUDIT_ADMIN = "ADMIN";
    static final String AUDIT_PARTNER = "PARTNER";
    static final String AUDIT_PARTNER_WARNING = "PARTNER_WARNING";
    static final String AUDIT_PENALTY_UPDATED = "PENALTY_UPDATED";
    static final String AUDIT_WARNING_CREATED = "WARNING_CREATED";
    static final String AUDIT_WARNING_DISPUTED = "WARNING_DISPUTED";

    static final String EVENT_TICKET_CREATED = "ticket-created";
    static final String EVENT_TICKET_MESSAGE = "ticket-message";
    static final String EVENT_TICKET_STATUS = "ticket-status";
    static final String EVENT_WARNING_ACKNOWLEDGED = "warning-acknowledged";
    static final String EVENT_WARNING_COMMENT = "warning-comment";
    static final String EVENT_WARNING_CREATED = "warning-created";
    static final String EVENT_WARNING_DISPUTED = "warning-disputed";

    static final String MSG_ATTACHMENT_ADDED = "Attachment added";
    static final String MSG_BOOKING_NOT_FOUND = "Booking not found";
    static final String MSG_CLOSED_TICKETS_CANNOT_BE_REPLIED =
            "Closed tickets cannot be replied to. Reopen the ticket first.";
    static final String MSG_COMMENT_OR_ATTACHMENTS_REQUIRED = "Comment message or attachmentUrls is required";
    static final String MSG_DISPUTE_ALREADY_ACTIVE = "A dispute is already active for this warning";
    static final String MSG_DISPUTE_REASON_REQUIRED = "Dispute reason is required";
    static final String MSG_FOLLOW_ADMIN_GUIDANCE = "Follow admin guidance.";
    static final String MSG_INVALID_BOOKING_ID = "Invalid bookingId";
    static final String MSG_INVALID_PENALTY_STATUS = "Invalid penalty status";
    static final String MSG_INVALID_TICKET_ID = "Invalid ticket id";
    static final String MSG_LEAVE_NOT_FOUND = "Leave not found";
    static final String MSG_LINK_BOOKING_FORBIDDEN = "You are not allowed to link this booking";
    static final String MSG_ONLY_CLOSED_OR_RESOLVED_REOPEN = "Only closed or resolved tickets can be reopened";
    static final String MSG_PINCODE_REQUIRED = "pincode is required";
    static final String MSG_REVIEW_ISSUE_GUIDANCE = "Review the issue and avoid repeating it on future jobs.";
    static final String MSG_SERVICE_NOT_FOUND = "Service not found";
    static final String MSG_STATUS_REQUIRED = "status is required";
    static final String MSG_STATUS_OPEN_OR_CLOSED = "Status must be OPEN or CLOSED";
    static final String MSG_STREAM_NOT_AVAILABLE = "Support realtime stream is not available";
    static final String MSG_TICKET_ALREADY_CLOSED = "Ticket is already closed";
    static final String MSG_TICKET_ID_REQUIRED = "Ticket id is required";
    static final String MSG_TICKET_NOT_FOUND = "Ticket not found";
    static final String MSG_TO_DATE_AFTER_FROM_DATE = "toDate must be greater than or equal to fromDate";
    static final String MSG_WARNING_ACCOUNT = "A new warning has been issued to your account. Tap to view the details.";
    static final String MSG_WARNING_NOT_FOUND = "Warning not found";
    static final String MSG_WARNING_NOTIFICATION_TITLE = "New Warning Issued";
    static final String MSG_WARNING_TITLE = "WARNING";
    static final String MSG_WARNING_WORK_GUIDANCE = "Contact support or admin before taking new jobs.";

    static final String SENDER_PARTNER = "PARTNER";
    static final String SENDER_SUPPORT = "SUPPORT";

    static final String STATUS_ACTIVE = "ACTIVE";
    static final String STATUS_BLOCKED = "BLOCKED";
    static final String STATUS_CANCELLED = "CANCELLED";
    static final String STATUS_CLOSED = "CLOSED";
    static final String STATUS_COMPLETED = "COMPLETED";
    static final String STATUS_INACTIVE = "INACTIVE";
    static final String STATUS_MEDIUM = "MEDIUM";
    static final String STATUS_NONE = "NONE";
    static final String STATUS_OPEN = "OPEN";
    static final String STATUS_REQUESTED = "REQUESTED";
    static final String STATUS_RESOLVED = "RESOLVED";
    static final String STATUS_SCHEDULED = "SCHEDULED";
    static final String STATUS_SUSPENSION = "SUSPENSION";
    static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    static final String STATUS_WARNING = "WARNING";

    private PartnerServiceText() {
    }
}
