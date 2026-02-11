package com.dataloader.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCsvRow {
    private String customerCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String dateOfBirth;   // String for flexible parsing
    private String country;
    private String city;
    private String address;
    private String postalCode;
    private String loyaltyPoints;
    private String isActive;
}
