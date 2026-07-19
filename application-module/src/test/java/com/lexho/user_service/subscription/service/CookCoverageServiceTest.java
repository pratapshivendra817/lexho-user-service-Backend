package com.lexho.user_service.subscription.service;

import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.subscription.dto.CookCoverageItemRequest;
import com.lexho.user_service.subscription.dto.CookCoverageRequest;
import com.lexho.user_service.subscription.dto.CookCoverageResponse;
import com.lexho.user_service.subscription.entity.CookCoverage;
import com.lexho.user_service.subscription.entity.CookCoverageItem;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.repository.CookCoverageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CookCoverageServiceTest {

    private final CookCoverageRepository cookCoverageRepository = mock(CookCoverageRepository.class);
    private final AdminServiceEntityRepository adminServiceEntityRepository = mock(AdminServiceEntityRepository.class);
    private final CookCoverageService service = new CookCoverageService(cookCoverageRepository, adminServiceEntityRepository);

    @BeforeEach
    void setUp() {
        when(cookCoverageRepository.save(any(CookCoverage.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void customerListUsesFamilySpecificItemsAndQuantities() {
        CookCoverage small = coverage(11L, FamilySize.SMALL, "1-2", item("Dal", 1), item("Roti", 6));
        CookCoverage medium = coverage(11L, FamilySize.MEDIUM, "3-4", item("Paneer", 1), item("Roti", 10));
        when(cookCoverageRepository.findByServiceIdAndActiveTrueOrderBySortOrderAscFamilySizeAsc(11L))
                .thenReturn(List.of(small, medium));

        List<CookCoverageResponse> responses = service.customerList(11L, FamilySize.MEDIUM);

        assertThat(responses).hasSize(1);
        CookCoverageResponse response = responses.get(0);
        assertThat(response.familySize()).isEqualTo("MEDIUM");
        assertThat(response.members()).isEqualTo("3-4");
        assertThat(response.items()).extracting(item -> item.name(), item -> item.quantity())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Paneer", 1),
                        org.assertj.core.groups.Tuple.tuple("Roti", 10)
                );
    }

    @Test
    void createAllowsSameServiceAndFamilySizeWhenOnlyDeletedCoverageExists() {
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(85L)).thenReturn(Optional.of(mock()));
        when(cookCoverageRepository.findByServiceIdAndFamilySizeAndActiveTrue(85L, FamilySize.SMALL))
                .thenReturn(Optional.empty());

        service.create(request(85L, FamilySize.SMALL));

        ArgumentCaptor<CookCoverage> captor = ArgumentCaptor.forClass(CookCoverage.class);
        verify(cookCoverageRepository).save(captor.capture());
        assertThat(captor.getValue().getServiceId()).isEqualTo(85L);
        assertThat(captor.getValue().getFamilySize()).isEqualTo(FamilySize.SMALL);
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void createRejectsSameServiceAndFamilySizeWhenActiveCoverageExists() {
        CookCoverage existing = coverage(85L, FamilySize.SMALL, "1-2", item("Dal", 1));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(85L)).thenReturn(Optional.of(mock()));
        when(cookCoverageRepository.findByServiceIdAndFamilySizeAndActiveTrue(85L, FamilySize.SMALL))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(request(85L, FamilySize.SMALL)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cook coverage already exists for this service and family size");
    }

    private CookCoverage coverage(Long serviceId, FamilySize familySize, String members, ItemSpec... itemSpecs) {
        CookCoverage coverage = new CookCoverage();
        coverage.setServiceId(serviceId);
        coverage.setFamilySize(familySize);
        coverage.setMembers(members);
        coverage.setFamilySizeLabel(members + " Members");
        coverage.setReference("Standard home cooking");
        coverage.setSortOrder(switch (familySize) {
            case SMALL -> 10;
            case MEDIUM -> 20;
            case LARGE -> 30;
            case XLARGE -> 40;
        });
        for (int i = 0; i < itemSpecs.length; i++) {
            CookCoverageItem item = new CookCoverageItem();
            item.setName(itemSpecs[i].name());
            item.setIcon(itemSpecs[i].name().toLowerCase());
            item.setSortOrder(i + 1);
            item.setQuantity(itemSpecs[i].quantity());
            coverage.addItem(item);
        }
        return coverage;
    }

    private ItemSpec item(String name, int quantity) {
        return new ItemSpec(name, quantity);
    }

    private CookCoverageRequest request(Long serviceId, FamilySize familySize) {
        return new CookCoverageRequest(
                serviceId,
                familySize,
                "1-2",
                "1-2 Members",
                "Standard home cooking",
                10,
                true,
                List.of(new CookCoverageItemRequest("Dal", "dal", 1, 1))
        );
    }

    private record ItemSpec(String name, int quantity) {
    }
}
