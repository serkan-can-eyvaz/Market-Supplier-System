package com.example.marketsupplier.agent.actions;

import com.example.marketsupplier.service.CustomerContext;

public interface ActionHandler {
    String getActionName();
    boolean canHandle(String action);
    String handle(String phone, String text, CustomerContext context);
}


