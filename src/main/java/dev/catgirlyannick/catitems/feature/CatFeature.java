package dev.catgirlyannick.catitems.feature;

public record CatFeature(
        String id,
        String title,
        FeatureStatus status,
        String summary
) {
}
