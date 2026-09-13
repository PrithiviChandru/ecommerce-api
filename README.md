# E-Commerce Monolith

A Spring Boot e-commerce application initially developed using a monolithic architecture.

## Architecture

```text
                         ┌─────────────────┐
                         │     Client      │
                         └────────┬────────┘
                                  │
                                  ▼
                    ┌─────────────────────────┐
                    │   E-Commerce Monolith   │
                    │      Spring Boot        │
                    └────────────┬────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
          ▼                      ▼                      ▼
   ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
   │     Auth     │      │   Category   │      │   Product    │
   └──────────────┘      └──────────────┘      └──────────────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                       ┌─────────┴─────────┐
                       │                   │
                       ▼                   ▼
                ┌──────────────┐    ┌──────────────┐
                │    Order     │    │   Payment    │
                └──────────────┘    └──────────────┘
                                 │
                                 ▼
                         ┌──────────────┐
                         │    MySQL     │
                         └──────────────┘
```

## Modules

* Authentication
* Category
* Product
* Order
* Payment

## Tech Stack

* Java
* Spring Boot
* Spring Security
* Spring Data JPA
* MySQL
* JWT
* Swagger
