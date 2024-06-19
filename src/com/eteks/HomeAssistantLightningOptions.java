package com.eteks;

import com.eteks.sweethome3d.j3d.AbstractPhotoRenderer.Quality;

public class HomeAssistantLightningOptions {
    private final String path;
    private final int imageWidth;
    private final int imageHeight;
    private final Quality quality;
    private final String haPath;

    public HomeAssistantLightningOptions(String path, int imageWidth, int imageHeight, Quality quality, String haPath) {
        super();
        this.path = path;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.quality = quality;
        this.haPath = haPath;
    }

    public String getPath() {
        return path;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public Quality getQuality() {
        return quality;
    }

    public String getHaPath() {
        return haPath;
    }

    @Override
    public String toString() {
        return "HomeAssistantLightningOptions [path=" + path + ", imageWidth=" + imageWidth + ", imageHeight="
                + imageHeight + ", quality=" + quality + ", haPath=" + haPath + "]";  // Modified this line
    }
}
