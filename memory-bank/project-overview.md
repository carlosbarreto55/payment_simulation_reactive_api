# Project Overview

## Name
Reactive Checkout & Payment Platform (Payment Simulation Reactive API)

## Purpose
A reactive, non-blocking REST API for payment processing simulation, built with Spring WebFlux and R2DBC. Handles user authentication, customer management, product catalog, and payment billing integration with external Payment Service Providers (PSP).

## Domain Context
E-commerce checkout and payment domain, focused on:
- User registration and JWT-based authentication with refresh token rotation
- Customer profile management with document validation (CPF/CNPJ/CNH)
- Product catalog with stock management and SKU-based lookup
- Payment intent creation and processing via external PSP (AbacatePay)
- Idempotency guarantees for payment operations

## Development Status
- Overall: ~70% complete
- User/Auth: ~95%
- Product: ~95%
- Customer: ~70%
- Payment: ~60% (only billing creation; webhooks, refunds missing)
- Infrastructure: Monitoring active (Actuator + Prometheus)
- Not implemented: Risk analysis, Outbox pattern, Audit logging, Webhooks

## Language
Java 17+

## Build Tool
Maven (with wrapper `mvnw`)

## Repository
GitHub-hosted with GitHub Actions CI/CD and GitHub Container Registry
