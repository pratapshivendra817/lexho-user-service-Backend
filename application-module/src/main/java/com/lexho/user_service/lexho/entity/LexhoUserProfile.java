package com.lexho.user_service.lexho.entity;

import com.lexho.common.enums.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "lexho_user_profiles")
public class LexhoUserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String mobile;

    @Column(length = 120)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 512)
    private String profileImage;

    @Column(length = 20)
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    public Long getId() { return id; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
