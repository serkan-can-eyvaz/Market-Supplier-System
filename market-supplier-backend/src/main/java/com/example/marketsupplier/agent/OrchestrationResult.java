package com.example.marketsupplier.agent;

import java.io.Serializable;

public class OrchestrationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String intent;
    private final String response;
    private final boolean isProductList;


    public OrchestrationResult(String intent, String response) {
        this.intent = intent;
        this.response = response;
        this.isProductList = false;
    }

    public OrchestrationResult(String intent, String response, boolean isProductList) {
        this.intent = intent;
        this.response = response;
        this.isProductList = isProductList;
    }

    public String getIntent() {
        return intent;
    }

    public String getResponse() {
        return response;
    }

    public boolean isProductList() {
        return isProductList;
    }
}
