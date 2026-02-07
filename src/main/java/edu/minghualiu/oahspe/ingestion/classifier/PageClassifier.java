package edu.minghualiu.oahspe.ingestion.classifier;
import edu.minghualiu.oahspe.entities.PageContent;
import edu.minghualiu.oahspe.records.PageClassificationResult;

public interface PageClassifier {
    PageClassificationResult classify(PageContent pc);
}