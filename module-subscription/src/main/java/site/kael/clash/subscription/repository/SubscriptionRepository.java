package site.kael.clash.subscription.repository;

import site.kael.clash.subscription.model.Subscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(String id);
    List<Subscription> findAll();
    void deleteById(String id);
}
