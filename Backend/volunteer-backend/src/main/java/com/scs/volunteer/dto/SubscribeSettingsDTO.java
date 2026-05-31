package com.scs.volunteer.dto;

import java.util.List;

public class SubscribeSettingsDTO {
    private Boolean enabled;
    private List<String> categories;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
}
