package com.micro.auth.config;

import com.micro.auth.entity.User;
import com.micro.auth.enums.Role;
import com.micro.auth.repository.UserRepository;
import com.micro.category.entity.Category;
import com.micro.category.repository.CategoryRepository;
import com.micro.product.dto.ProductRequest;
import com.micro.product.entity.Product;
import com.micro.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Configuration
public class DataInitializer {
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdmin(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;

        return args -> {
            createUsers();
            createCategories();
            createProducts();
        };
    }

    private void createUsers() {
        createUserIfNotExists(
                "admin@example.com",
                "Super",
                "Admin",
                "Admin@123",
                Role.ADMIN
        );

        createUserIfNotExists(
                "user@example.com",
                "Demo",
                "User",
                "Password123",
                Role.USER
        );
    }

    private void createUserIfNotExists(
            String email,
            String firstName,
            String lastName,
            String password,
            Role role
    ) {
        this.userRepository.findByEmail(email)
                .orElseGet(() -> {
                            User user = User.builder()
                                    .firstName(firstName)
                                    .lastName(lastName)
                                    .email(email)
                                    .password(this.passwordEncoder.encode(password))
                                    .role(role)
                                    .timeZone("Asia/Kolkata")
                                    .verified(true)
                                    .createdAt(Instant.now())
                                    .build();

                            System.out.println(role + " created");
                            return this.userRepository.save(user);
                        }
                );
    }

    private void createCategories() {
        List<Category> categories = List.of(
                Category.builder()
                        .name("Tech")
                        .description("Technology and electronic products")
                        .build(),

                Category.builder()
                        .name("Accessories")
                        .description("Daily use accessories and add-ons")
                        .build(),

                Category.builder()
                        .name("Home")
                        .description("Home and living products")
                        .build(),

                Category.builder()
                        .name("Apparel")
                        .description("Clothing and fashion products")
                        .build()
        );

        List<Category> newCategories = categories.stream()
                .filter(c -> !this.categoryRepository.existsByNameIgnoreCase(c.getName()))
                .toList();

        if (!newCategories.isEmpty()) {
            this.categoryRepository.saveAll(newCategories);
            System.out.println(newCategories.size() + " categories created");
        }
    }

    private void createProducts() {
        List<ProductRequest> products = List.of(
                new ProductRequest(
                        "Apex Wireless Noise-Cancelling Headphones",
                        "Experience premium sound with industry-leading hybrid active noise cancelling technology.",
                        1L,
                        BigDecimal.valueOf(299),
                        10
                ),
                new ProductRequest(
                        "Titan Smartwatch Series 5",
                        "Track your health, receive notifications, and enjoy a gorgeous 1.9-inch AMOLED display.",
                        1L,
                        BigDecimal.valueOf(199),
                        10
                ),
                new ProductRequest(
                        "AeroGrip Mechanical Gaming Keyboard",
                        "Ultra-responsive brown switches with fully customizable dynamic RGB backlighting.",
                        1L,
                        BigDecimal.valueOf(129),
                        10
                ),
                new ProductRequest(
                        "Nomad Full-Grain Leather Travel Duffle",
                        "Handcrafted travel duffle bag designed with durable waterproof zippers and dedicated shoe pocket.",
                        2L,
                        BigDecimal.valueOf(89),
                        10
                ),
                new ProductRequest(
                        "Vanguard Minimalist Carbon Fiber Wallet",
                        "RFID-blocking slim cardholder that holds up to 12 cards without stretching out.",
                        2L,
                        BigDecimal.valueOf(45),
                        10
                ),
                new ProductRequest(
                        "OmniFit Ergonomic Breathable Office Chair",
                        "Complete adjustable lumbo-sacral support system with high-density mesh and reclining locks.",
                        3L,
                        BigDecimal.valueOf(349),
                        10
                ),
                new ProductRequest(
                        "Prime Cotton Oversized Fit Tee",
                        "Super soft 240GSM heavyweight cotton fabric with dropping shoulder silhouette.",
                        4L,
                        BigDecimal.valueOf(28),
                        10
                ),
                new ProductRequest(
                        "Aura Ceramic Essential Oil Diffuser",
                        "Minimalist ceramic ultrasonic diffuser with ambient warm-light glow modes.",
                        3L,
                        BigDecimal.valueOf(38),
                        10
                )
        );

        products.forEach(productInfo -> {
            if (this.productRepository.findByName(productInfo.name()).isPresent()) {
                return;
            }

            Category category = this.categoryRepository
                    .findById(productInfo.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found : " + productInfo.categoryId()));

            Product product = Product.builder()
                    .name(productInfo.name())
                    .description(productInfo.description())
                    .category(category)
                    .price(productInfo.price())
                    .stock(productInfo.stock())
                    .build();
            productRepository.save(product);
        });

        System.out.println(products.size() + " Products created successfully");
    }
}
