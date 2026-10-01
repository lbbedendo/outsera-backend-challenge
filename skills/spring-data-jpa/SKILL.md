---
name: spring-data-jpa
description: >
  Use when generating or refactoring Spring Boot 4 JPA entities, repositories, queries, projections,
  persistence tests, entity relationships, embeddables, IDs, or Hibernate mappings. Covers Jakarta
  Persistence 3.2 imports, Hibernate 7 entity modeling, new-state detection, N+1 prevention,
  projections, keyset pagination, batch writes, and common agent mistakes.
---

# Spring Data JPA (Boot 4 / Hibernate 7)

Spring Boot 4 manages Jakarta Persistence 3.2, Jakarta Validation 3.1, and Hibernate ORM 7.x. Use
Boot dependency management and import `jakarta.persistence.*` / `jakarta.validation.*`. Do not add
explicit Hibernate, JPA, or Validator versions unless the project has a deliberate override policy.

## Entity Model Rules

Use an `@Entity` only for persistent state with identity and lifecycle. Use records for DTOs,
commands, and read models. Use `@Embeddable` for values stored inside an entity table.

Rules:

- Use `jakarta.persistence.*`, never `javax.persistence.*`.
- Keep entities non-final with a protected no-arg constructor so Hibernate can instantiate/proxy them.
- Do not use Java records for ordinary entities. Records are good DTOs and sometimes embeddables.
- Prefer behavior methods and static factories over public setters/constructors.
- Initialize collections inline. JPA collection fields should not be null.
- Use `@Enumerated(EnumType.STRING)` with explicit column length. Never use `ORDINAL`.
- Validate request DTOs at the boundary; enforce entity invariants inside behavior methods.
- Never expose entities from controllers. Map entities to response records:

```java
public record OrderResponse(UUID id, String status, BigDecimal total, Instant createdAt) {
    static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getStatus().name(),
            order.getTotal().amount(),
            order.getCreatedAt());
    }
}
```

## equals and hashCode

Do not generate entity equality with Lombok `@Data`. It includes mutable fields and associations,
which can trigger lazy loading, recursion, and hash changes.

Preferred options:

- If the entity has a stable natural key, base equality on that key and enforce a unique database
  constraint.
- Never include collections, mutable fields, or associations in `equals`, `hashCode`, or `toString`.
- Use `instanceof`, not `getClass()`, when equality must work with Hibernate proxies.

```java
@Override
public boolean equals(Object other) {
    return other instanceof Customer that
        && email != null
        && email.equals(that.getEmail());
}

@Override
public int hashCode() {
    return email == null ? 0 : email.hashCode();
}
```

## Repositories and Query Patterns

```java
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByCustomerIdAndStatus(UUID customerId, OrderStatus status);

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findById(UUID id);

    @Query("""
        select o
        from Order o
        where o.status = :status
        order by o.createdAt desc, o.id desc
        """)
    List<Order> findRecentByStatus(OrderStatus status, Limit limit);
}
```

Use:

- Derived queries for simple filters.
- `@Query` for explicit joins, keyset pagination, and complex predicates.

Avoid:

- `findAll()` in endpoints.
- Returning entities for read-only list views when a projection is enough.

## N+1 Prevention

Identify N+1 by looking for lazy association access inside loops or JSON serialization of entities.

```java
@EntityGraph(attributePaths = {"items", "items.product"})
Optional<Order> findWithItemsAndProductsById(UUID id);

public interface OrderSummary {
    UUID getId();
    UUID getCustomerId();
    OrderStatus getStatus();
    Instant getCreatedAt();
}

List<OrderSummary> findByStatus(OrderStatus status);
```

## Pagination

Use `Pageable` for normal list screens:

```java
Page<Order> findByStatus(OrderStatus status, Pageable pageable);
```

Use keyset pagination for deep or infinite-scroll lists. `OFFSET` pagination scans and discards
skipped rows.

```java
@Query("""
    select o
    from Order o
    where o.status = :status
      and (o.createdAt < :lastCreatedAt
           or (o.createdAt = :lastCreatedAt and o.id < :lastId))
    order by o.createdAt desc, o.id desc
    """)
List<Order> findNextPage(OrderStatus status, Instant lastCreatedAt, Long lastId, Limit limit);
```

## Gotchas

- Agent imports `javax.persistence.*` - Boot 4 uses `jakarta.persistence.*`.
- Agent creates entity records - use records for DTOs/embeddables, not ordinary entities.
- Agent puts `@Data` on entities - generates setters and unsafe equality; use targeted `@Getter`.
- Agent makes entities `final` or constructors private - breaks Hibernate proxy/instantiation.
- Agent uses `FetchType.EAGER` - use `LAZY` on to-one and many-to-many relationships.
- Agent uses `@Enumerated(EnumType.ORDINAL)` - use `STRING`.
- Agent uses primitive `long version` - use nullable wrapper `Long`.
- Agent omits `@Version` on editable aggregates - lost updates are not detected.
- Agent returns entities from controllers - map to DTO records.
- Agent calls `findAll()` for list endpoints - require `Pageable`, `Limit`, or a projection query.
- Agent uses `OFFSET` pagination on huge tables - switch to keyset for deep pages.
- Agent includes lazy associations in equality or `toString` - causes lazy loads and recursion.
- Agent maps every relationship bidirectionally - add back-references only when required.
- Agent uses `@ManyToMany` for business links with attributes - model the join row as an entity.