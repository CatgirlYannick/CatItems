package dev.catgirlyannick.catitems.feature;

public enum FeatureStatus {
    LIVE("live"),
    FOUNDATION("foundation"),
    PLANNED("planned");

    private final String label;

    FeatureStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
