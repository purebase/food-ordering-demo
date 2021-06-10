package io.axoniq.foodordering.domain.aggregates;

import io.axoniq.foodordering.domain.ConfirmOrderCommand;
import io.axoniq.foodordering.domain.CreateFoodCartCommand;
import io.axoniq.foodordering.domain.DeselectProductCommand;
import io.axoniq.foodordering.domain.FoodCartCreatedEvent;
import io.axoniq.foodordering.domain.OrderConfirmedEvent;
import io.axoniq.foodordering.domain.ProductDeselectedEvent;
import io.axoniq.foodordering.domain.ProductDeselectionException;
import io.axoniq.foodordering.domain.ProductSelectedEvent;
import io.axoniq.foodordering.domain.SelectProductCommand;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Aggregate
class FoodOrderingAggregate {

    private static final Logger logger = LoggerFactory.getLogger(FoodOrderingAggregate.class);

    @AggregateIdentifier
    private UUID foodCartId;
    private Map<UUID, Integer> selectedProducts;
    private boolean confirmed;

    public FoodOrderingAggregate() {
        // Required by Axon
    }

    @CommandHandler
    public FoodOrderingAggregate(CreateFoodCartCommand command) {
        AggregateLifecycle.apply(new FoodCartCreatedEvent(command.getFoodCartId()));
    }

    @CommandHandler
    public void on(SelectProductCommand command) {
        AggregateLifecycle.apply(new ProductSelectedEvent(foodCartId, command.getProductId(), command.getQuantity()));
    }

    @CommandHandler
    public void on(DeselectProductCommand command) throws ProductDeselectionException {
        UUID productId = command.getProductId();
        int quantity = command.getQuantity();

        if (!selectedProducts.containsKey(productId)) {
            throw new ProductDeselectionException(
                    "Cannot deselect a product which has not been selected for this Food Cart"
            );
        }
        if (selectedProducts.get(productId) - quantity < 0) {
            throw new ProductDeselectionException(
                    "Cannot deselect more products of ID [" + productId + "] than have been selected initially"
            );
        }

        AggregateLifecycle.apply(new ProductDeselectedEvent(foodCartId, productId, quantity));
    }

    @CommandHandler
    public void on(ConfirmOrderCommand command) {
        if (confirmed) {
            logger.warn("Cannot confirm a Food Cart order which is already confirmed");
            return;
        }

        AggregateLifecycle.apply(new OrderConfirmedEvent(foodCartId));
    }

    @EventSourcingHandler
    public void on(FoodCartCreatedEvent event) {
        foodCartId = event.getFoodCartId();
        selectedProducts = new HashMap<>();
        confirmed = false;
    }

    @EventSourcingHandler
    public void on(ProductSelectedEvent event) {
        selectedProducts.merge(event.getProductId(), event.getQuantity(), Integer::sum);
    }

    @EventSourcingHandler
    public void on(ProductDeselectedEvent event) {
        selectedProducts.computeIfPresent(
                event.getProductId(),
                (productId, quantity) -> quantity -= event.getQuantity()
        );
    }

    @EventSourcingHandler
    public void on(OrderConfirmedEvent event) {
        confirmed = true;
    }
}
