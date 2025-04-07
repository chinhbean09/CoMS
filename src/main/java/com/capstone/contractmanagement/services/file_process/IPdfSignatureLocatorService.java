package com.capstone.contractmanagement.services.file_process;

import java.io.InputStream;

public interface IPdfSignatureLocatorService {
    PdfSignatureLocatorService.SignatureCoordinates findCoordinates(InputStream inputStream) throws Exception;
}
