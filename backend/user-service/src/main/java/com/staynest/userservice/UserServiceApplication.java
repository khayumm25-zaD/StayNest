package com.staynest.userservice;
import org.springframework.boot.SpringApplication;import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@SpringBootApplication @EnableDiscoveryClient public class UserServiceApplication{public static void main(String[] a){SpringApplication.run(UserServiceApplication.class,a);}}