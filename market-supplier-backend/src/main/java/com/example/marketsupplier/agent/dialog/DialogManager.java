package com.example.marketsupplier.agent.dialog;

import com.example.marketsupplier.agent.model.Item;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.ArrayList;

@Component
public class DialogManager {
    
    public PlannedAction planAction(String intent, List<Item> items) {
        return new PlannedAction(intent, items);
    }
    
    public static class PlannedAction {
        private final String intent;
        private final List<Item> items;
        
        public PlannedAction(String intent, List<Item> items) {
            this.intent = intent;
            this.items = items != null ? items : new ArrayList<>();
        }
        
        public String getIntent() { return intent; }
        public List<Item> getItems() { return items; }
    }
}