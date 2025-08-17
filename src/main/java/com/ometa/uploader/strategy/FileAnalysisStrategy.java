package com.ometa.uploader.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FileAnalysisStrategy {
    List<Map<String, String>> inferSchema(InputStream inputStream, int linesToScan) throws IOException;
}
