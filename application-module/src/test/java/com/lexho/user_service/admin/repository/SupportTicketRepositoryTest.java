package com.lexho.user_service.admin.repository;

import com.lexho.admin.entity.Admin;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.entity.SupportTicketReply;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = SupportTicketRepositoryTest.JpaConfig.class)
class SupportTicketRepositoryTest {

    @Autowired
    private SupportTicketRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void searchByRequesterTypeFiltersPartnerTicketsAndPersistsReplies() {
        SupportTicket partnerTicket = ticket(101L, "Partner payout issue", "Need settlement details", "OPEN", "PARTNER:BILLING", "HIGH");
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(partnerTicket);
        reply.setMessage("We are reviewing it");
        partnerTicket.getReplies().add(reply);
        repository.saveAndFlush(partnerTicket);

        SupportTicket customerTicket = ticket(101L, "Address update", "Please change address", "OPEN", "ADDRESS", "LOW");
        repository.saveAndFlush(customerTicket);

        entityManager.clear();

        Page<SupportTicket> partnerPage = repository.searchByRequesterType(
                "payout",
                "open",
                "billing",
                "high",
                true,
                PageRequest.of(0, 10)
        );

        assertThat(partnerPage.getContent()).hasSize(1);
        SupportTicket loaded = partnerPage.getContent().get(0);
        assertThat(loaded.getSubject()).isEqualTo("Partner payout issue");
        assertThat(loaded.getReplies()).hasSize(1);
        assertThat(loaded.getReplies().get(0).getSenderType()).isEqualTo("SUPPORT");

        Page<SupportTicket> customerPage = repository.searchByRequesterType(
                "address",
                "open",
                "address",
                "low",
                false,
                PageRequest.of(0, 10)
        );

        assertThat(customerPage.getContent()).extracting(SupportTicket::getSubject)
                .containsExactly("Address update");
    }

    @Test
    void findByUserIdAndStatusIgnoreCaseAndCategoryPrefixReturnsNewestFirst() {
        SupportTicket older = ticket(500L, "Old request", "Old message", "CLOSED", "PARTNER:OPS", "MEDIUM");
        repository.saveAndFlush(older);
        entityManager.createNativeQuery("UPDATE support_tickets SET created_at = ? WHERE id = ?")
                .setParameter(1, Timestamp.valueOf(LocalDateTime.now().minusHours(2)))
                .setParameter(2, older.getId())
                .executeUpdate();

        SupportTicket newer = ticket(500L, "New request", "Fresh message", "closed", "PARTNER:OPS", "MEDIUM");
        repository.saveAndFlush(newer);
        entityManager.createNativeQuery("UPDATE support_tickets SET created_at = ? WHERE id = ?")
                .setParameter(1, Timestamp.valueOf(LocalDateTime.now().minusHours(1)))
                .setParameter(2, newer.getId())
                .executeUpdate();
        entityManager.clear();

        List<SupportTicket> results = repository
                .findByUserIdAndStatusIgnoreCaseAndCategoryIdStartingWithOrderByCreatedAtDesc(
                        500L,
                        "CLOSED",
                        "PARTNER:",
                        PageRequest.of(0, 10)
                )
                .getContent();

        assertThat(results).extracting(SupportTicket::getSubject)
                .containsExactly("New request", "Old request");

        assertThat(repository.findByUserIdAndIdAndCategoryIdStartingWith(500L, newer.getId(), "PARTNER:"))
                .isPresent();
        assertThat(repository.findByUserIdAndIdAndCategoryIdStartingWith(500L, newer.getId(), "CUSTOMER:"))
                .isEmpty();
    }

    @Test
    void searchTreatsSqlMetaCharactersAsLiteralInput() {
        repository.saveAndFlush(ticket(700L, "Billing issue", "Need payout update", "OPEN", "PARTNER:BILLING", "HIGH"));
        repository.saveAndFlush(ticket(701L, "Address update", "Please update profile", "OPEN", "ADDRESS", "LOW"));

        Page<SupportTicket> searchResults = repository.search(
                "' OR '1'='1",
                "open' OR '1'='1",
                "PARTNER:BILLING' OR '1'='1",
                "HIGH' OR '1'='1",
                PageRequest.of(0, 10)
        );

        Page<SupportTicket> requesterTypeResults = repository.searchByRequesterType(
                "' UNION SELECT * FROM support_tickets --",
                null,
                "billing' OR '1'='1",
                null,
                true,
                PageRequest.of(0, 10)
        );

        assertThat(searchResults.getContent()).isEmpty();
        assertThat(requesterTypeResults.getContent()).isEmpty();
    }

    private SupportTicket ticket(Long userId,
                                 String subject,
                                 String message,
                                 String status,
                                 String categoryId,
                                 String priority) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(userId);
        ticket.setSubject(subject);
        ticket.setMessage(message);
        ticket.setStatus(status);
        ticket.setCategoryId(categoryId);
        ticket.setPriority(priority);
        ticket.setAttachmentUrls("[\"one.png\"]");
        return ticket;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {SupportTicket.class, SupportTicketReply.class, Admin.class})
    @EnableJpaRepositories(
            basePackageClasses = SupportTicketRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SupportTicketRepository.class)
    )
    static class JpaConfig {
    }
}

