package com.capstone.contractmanagement.services.file_process;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfSignatureLocatorService extends PDFTextStripper implements IPdfSignatureLocatorService {

    private static class MatchInfo {
        int page;
        float x;
        float y;
    }

    private final List<MatchInfo> daiDienBenAList = new ArrayList<>();
    private final List<MatchInfo> kyGhiRoHoTenList = new ArrayList<>();

    public PdfSignatureLocatorService() throws Exception {
        super.setSortByPosition(true);
    }

    private String normalizeText(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase();
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) {
        String normalizedText = normalizeText(text);
        int currentPage = getCurrentPageNo();

        System.out.printf("PAGE %d: \"%s\" -> normalized: \"%s\"%n", currentPage, text, normalizedText);

        if (normalizedText.contains("DIEN BEN A")) {
            TextPosition pos = textPositions.get(0);
            MatchInfo info = new MatchInfo();
            info.page = currentPage;
            info.x = pos.getXDirAdj();
            info.y = pos.getYDirAdj();
            daiDienBenAList.add(info);
        }

        if (normalizedText.contains("KY VA GHI RO HO TEN")) {
            TextPosition pos = textPositions.get(0);
            MatchInfo info = new MatchInfo();
            info.page = currentPage;
            info.x = pos.getXDirAdj();
            info.y = pos.getYDirAdj();
            kyGhiRoHoTenList.add(info);
        }
    }

    @Override
    public SignatureCoordinates findCoordinates(InputStream inputStream) throws Exception {
        daiDienBenAList.clear();
        kyGhiRoHoTenList.clear();

        try (PDDocument document = PDDocument.load(inputStream)) {
            this.setStartPage(1);
            this.setEndPage(document.getNumberOfPages());
            this.getText(document);
        }

        if (daiDienBenAList.isEmpty() || kyGhiRoHoTenList.isEmpty()) {
            return null;
        }

        MatchInfo benA = daiDienBenAList.get(0);

        // Tìm "ký và ghi rõ họ tên" nằm cùng trang hoặc sau trang của "ĐẠI DIỆN BÊN A"
        for (MatchInfo ky : kyGhiRoHoTenList) {
            boolean samePageBelow = ky.page == benA.page && ky.y > benA.y;
            boolean onNextPage = ky.page > benA.page;

            if (samePageBelow || onNextPage) {
                return new SignatureCoordinates(
                        ky.x,
                        ky.y - 50,
                        ky.x + 200,
                        ky.y - 20,
                        ky.page
                );
            }
        }

        return null;
    }

    public static class SignatureCoordinates {
        public float llx, lly, urx, ury;
        public int page;

        public SignatureCoordinates(float llx, float lly, float urx, float ury, int page) {
            this.llx = llx;
            this.lly = lly;
            this.urx = urx;
            this.ury = ury;
            this.page = page;
        }
    }
}
