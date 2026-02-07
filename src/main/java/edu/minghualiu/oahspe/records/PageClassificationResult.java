package edu.minghualiu.oahspe.records;
import edu.minghualiu.oahspe.enums.PageType;

public record PageClassificationResult(PageType pageType, boolean needsGeometry) {}