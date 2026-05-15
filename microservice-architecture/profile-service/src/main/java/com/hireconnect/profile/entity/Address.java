package com.hireconnect.profile.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int addressId;
    
    private String houseNo;
    private String street;
    private String city;
    private String state;
    private int pincode;
}