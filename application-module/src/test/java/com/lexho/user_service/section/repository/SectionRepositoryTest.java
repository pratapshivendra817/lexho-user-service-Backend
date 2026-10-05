package com.lexho.user_service.section.repository;

import com.lexho.user_service.section.entity.Section;
import com.lexho.user_service.section.entity.SectionItem;
import com.lexho.user_service.section.enums.SectionType;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = SectionRepositoryTest.JpaConfig.class)
class SectionRepositoryTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SectionItemRepository itemRepository;

    @Test
    void sectionAndItemQueriesRespectActiveFlagsAndPriorityOrdering() {
        Section trust = sectionRepository.save(section("Trust", SectionType.TRUST, 1, true));
        Section stats = sectionRepository.save(section("Stats", SectionType.STATS, 1, true));
        sectionRepository.save(section("Hidden", SectionType.HIGHLIGHTS, 5, false));

        SectionItem lowPriority = itemRepository.save(item(trust, "Second", 2, true));
        SectionItem highPriority = itemRepository.save(item(trust, "First", 1, true));
        itemRepository.save(item(trust, "Disabled", 3, false));

        assertThat(sectionRepository.findByActiveTrueOrderByPriorityAscCreatedAtDescIdAsc())
                .extracting(Section::getTitle)
                .containsExactly("Stats", "Trust");
        assertThat(itemRepository.findBySectionIdOrderByPriorityAscIdAsc(trust.getId()))
                .extracting(SectionItem::getTitle)
                .containsExactly("First", "Second", "Disabled");
        assertThat(itemRepository.findBySectionIdAndActiveTrueOrderByPriorityAscIdAsc(trust.getId()))
                .extracting(SectionItem::getId)
                .containsExactly(highPriority.getId(), lowPriority.getId());
    }

    private Section section(String title, SectionType type, int priority, boolean active) {
        Section section = new Section();
        section.setTitle(title);
        section.setSubtitle(title + " subtitle");
        section.setIcon(title.toLowerCase());
        section.setType(type);
        section.setPriority(priority);
        section.setActive(active);
        return section;
    }

    private SectionItem item(Section section, String title, int priority, boolean active) {
        SectionItem item = new SectionItem();
        item.setSection(section);
        item.setTitle(title);
        item.setSubtitle(title + " subtitle");
        item.setValue(title + " value");
        item.setIcon(title.toLowerCase());
        item.setPriority(priority);
        item.setActive(active);
        return item;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {Section.class, SectionItem.class})
    @EnableJpaRepositories(
            basePackageClasses = {SectionRepository.class, SectionItemRepository.class},
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SectionRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SectionItemRepository.class)
            }
    )
    static class JpaConfig {
    }
}

