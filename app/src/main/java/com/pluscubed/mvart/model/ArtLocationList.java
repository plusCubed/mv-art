package com.pluscubed.mvart.model;

import java.util.ArrayList;
import java.util.List;

/**
 * List of Art Locations
 */
public class ArtLocationList {
    public static ArtLocationList instance = null;

    private List<ArtLocation> mArtLocations;

    protected ArtLocationList() {
        mArtLocations = new ArrayList<>();
    }

    public static ArtLocationList getInstance() {
        if (instance == null) {
            instance = new ArtLocationList();
        }
        return instance;
    }

    public List<ArtLocation> getList() {
        return new ArrayList<>(mArtLocations);
    }

    public void addAll(List<ArtLocation> artLocationList) {
        mArtLocations.addAll(artLocationList);
    }

    public ArtLocation get(int index) {
        return mArtLocations.get(index);
    }

    public void clear() {
        mArtLocations.clear();
    }

    public int indexOf(ArtLocation location) {
        return mArtLocations.indexOf(location);
    }
}
