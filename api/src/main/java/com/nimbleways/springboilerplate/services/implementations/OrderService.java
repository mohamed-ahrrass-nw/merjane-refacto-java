package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class OrderService {
    @Autowired
    private ProductRepository pr;

    @Autowired
    private ProductService ps;

    @Autowired
    NotificationService ns;

    public void processOrderProducts(Set<Product> products) {
        for (Product p : products) {
            if (p.getType().equals("NORMAL")) {
                if (p.getAvailable() > 0) {
                    p.setAvailable(p.getAvailable() - 1);
                    pr.save(p);
                } else {
                    int leadTime = p.getLeadTime();
                    if (leadTime > 0) {
                        ps.notifyDelay(leadTime, p);
                    }
                }
            } else if (p.getType().equals("SEASONAL")) {
                // Add new season rules
                if ((LocalDate.now().isAfter(p.getSeasonStartDate()) && LocalDate.now().isBefore(p.getSeasonEndDate())
                        && p.getAvailable() > 0)) {
                    p.setAvailable(p.getAvailable() - 1);
                    pr.save(p);
                } else {
                    this.handleSeasonalProduct(p);
                }
            }
        }
    }

    @Transactional
    public void handleSeasonalProduct(Product p) {
        if (LocalDate.now().plusDays(p.getLeadTime()).isAfter(p.getSeasonEndDate())) {
            ns.sendOutOfStockNotification(p.getName());
            p.setAvailable(0);
            pr.save(p);
        } else if (p.getSeasonStartDate().isAfter(LocalDate.now())) {
            ns.sendOutOfStockNotification(p.getName());
            pr.save(p);
        } else {
            ps.notifyDelay(p.getLeadTime(), p);
        }
    }
}
