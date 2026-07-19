package com.lexho.user_service.service;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.config.AppProperties;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {
    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock PartnerRepository partnerRepository;
    private AppProperties properties;
    private HomeService service;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        service = new HomeService(userRepository, categoryRepository, partnerRepository, properties);
    }

    @Test
    void buildsHomeDataAndSkipsEmptyCategorySection() {
        User user = new User(); user.setFullName("Asha");
        ServiceCategory cleaning = category(1L, "Cleaning");
        ServiceCategory cooking = category(2L, "Cooking");
        Partner partner = partner(10L, "Expert", 500.0, 4.8);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(categoryRepository.findAll()).thenReturn(List.of(cleaning, cooking));
        when(partnerRepository.findTop6ByAvailableTrueOrderByRatingDesc()).thenReturn(List.of(partner));
        when(partnerRepository.findTop6ByCategoryIdAndAvailableTrueOrderByRatingDesc(1L)).thenReturn(List.of(partner));
        when(partnerRepository.findTop6ByCategoryIdAndAvailableTrueOrderByRatingDesc(2L)).thenReturn(List.of());

        var result = service.getHomeData(7L);

        assertEquals("Hi, Asha", result.getGreeting());
        assertEquals(3, result.getCategories().size());
        assertEquals(2, result.getSections().size());
        assertEquals(600.0, result.getSections().get(0).getData().get(0).getOriginalPrice());
        assertEquals("Limited Offer", result.getBanners().get(0).getLabel());
    }

    @Test
    void usesFallbackGreetingForBlankName() {
        User user = new User(); user.setFullName(" ");
        properties.getHome().setFallbackGreeting("Welcome");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(partnerRepository.findTop6ByAvailableTrueOrderByRatingDesc()).thenReturn(List.of());
        assertEquals("Welcome", service.getHomeData(7L).getGreeting());
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getHomeData(99L));
        verifyNoInteractions(categoryRepository, partnerRepository);
    }

    private ServiceCategory category(Long id, String name) {
        ServiceCategory category = new ServiceCategory();
        category.setId(id); category.setName(name); category.setIcon("icon");
        return category;
    }

    private Partner partner(Long id, String name, Double price, Double rating) {
        Partner partner = new Partner();
        partner.setId(id); partner.setName(name); partner.setPrice(price); partner.setRating(rating);
        partner.setExperience(5); partner.setImage("image");
        return partner;
    }
}
