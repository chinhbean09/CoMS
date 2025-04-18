package com.capstone.contractmanagement.services.file_process;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PdfSignatureLocatorService extends PDFTextStripper implements IPdfSignatureLocatorService {

    private static class MatchInfo {
        int page;
        float x;
        float y;
    }

    private final Map<Integer, List<TextPosition>> pageTextPositions = new HashMap<>();
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
        int page = getCurrentPageNo();
        pageTextPositions.computeIfAbsent(page, k -> new ArrayList<>()).addAll(textPositions);
    }

    private List<TextPosition> orderTextPositions(List<TextPosition> positions) {
        Map<Float, List<TextPosition>> lines = new TreeMap<>(Collections.reverseOrder());
        float threshold = 2.0f;
        for (TextPosition tp : positions) {
            float y = tp.getYDirAdj();
            Optional<Float> keyOpt = lines.keySet().stream()
                    .filter(k -> Math.abs(k - y) < threshold)
                    .findFirst();
            float key = keyOpt.orElse(y);
            lines.computeIfAbsent(key, k -> new ArrayList<>()).add(tp);
        }
        return lines.values().stream()
                .map(line -> {
                    line.sort(Comparator.comparing(TextPosition::getXDirAdj));
                    return line;
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private void findMatches(int page, List<TextPosition> orderedPositions, String phrase, List<MatchInfo> matchList) {
        StringBuilder sb = new StringBuilder();
        for (TextPosition tp : orderedPositions) {
            sb.append(tp.getUnicode());
        }
        String pageText = sb.toString();
        String normalizedPageText = normalizeText(pageText);
        String normalizedPhrase = normalizeText(phrase);
        int index = normalizedPageText.indexOf(normalizedPhrase);
        while (index >= 0) {
            MatchInfo info = new MatchInfo();
            info.page = page;
            TextPosition startPos = orderedPositions.get(index);
            info.x = startPos.getXDirAdj();
            info.y = startPos.getYDirAdj();
            matchList.add(info);
            index = normalizedPageText.indexOf(normalizedPhrase, index + 1);
        }
    }

    @Override
    public SignatureCoordinates findCoordinates(InputStream inputStream) throws Exception {
        daiDienBenAList.clear();
        kyGhiRoHoTenList.clear();
        pageTextPositions.clear();

        float pageHeight = 0;
        try (PDDocument document = PDDocument.load(inputStream)) {
            this.setStartPage(1);
            this.setEndPage(document.getNumberOfPages());
            this.getText(document);
            pageHeight = document.getPage(1).getMediaBox().getHeight();
        }

        for (Map.Entry<Integer, List<TextPosition>> entry : pageTextPositions.entrySet()) {
            int page = entry.getKey();
            List<TextPosition> positions = orderTextPositions(entry.getValue());
            findMatches(page, positions, "ĐẠI DIỆN BÊN A", daiDienBenAList);
            findMatches(page, positions, "KÝ VÀ GHI RÕ HỌ TÊN", kyGhiRoHoTenList);
        }

        if (daiDienBenAList.isEmpty() || kyGhiRoHoTenList.isEmpty()) {
            return null;
        }

        kyGhiRoHoTenList.sort((a, b) -> {
            if (a.page != b.page) {
                return Integer.compare(a.page, b.page);
            }
            return Float.compare(a.y, b.y);
        });

        MatchInfo lastDaiDienBenA = daiDienBenAList.stream()
                .max(Comparator.comparingInt(a -> a.page))
                .orElse(null);

        if (lastDaiDienBenA == null) {
            return null;
        }

        MatchInfo bestKy = null;
        float closestYDistance = Float.MAX_VALUE;
        float xThreshold = 50.0f;

        for (MatchInfo ky : kyGhiRoHoTenList) {
            if (ky.page == lastDaiDienBenA.page && ky.y > lastDaiDienBenA.y) {
                float yDistance = ky.y - lastDaiDienBenA.y;
                float xDistance = Math.abs(ky.x - lastDaiDienBenA.x);
                if (yDistance < closestYDistance && xDistance < xThreshold) {
                    closestYDistance = yDistance;
                    bestKy = ky;
                }
            } else if (ky.page == lastDaiDienBenA.page + 1) {
                if (bestKy == null || ky.y < bestKy.y) {
                    bestKy = ky;
                    closestYDistance = Float.MAX_VALUE;
                }
            }
        }

        if (bestKy == null) {
            float llyFallback = pageHeight - (lastDaiDienBenA.y + 10);
            float uryFallback = pageHeight - (lastDaiDienBenA.y + 60);
            float llxFallback = lastDaiDienBenA.x - 30;
            float heightFallback = llyFallback - uryFallback;
            float widthFallback = (4.0f / 3.0f) * heightFallback;
            return new SignatureCoordinates(
                    Math.round(llxFallback),
                    Math.round(llyFallback),
                    Math.round(llxFallback + widthFallback),
                    Math.round(uryFallback),
                    lastDaiDienBenA.page
            );
        }

        float lly = bestKy.y + 10;
        float ury = bestKy.y + 60;
        float llyConverted = pageHeight - lly;
        float uryConverted = pageHeight - ury;
        float llxAdjusted = bestKy.x - 30;
        float urxAdjusted = 277;

        return new SignatureCoordinates(
                Math.round(llxAdjusted),
                Math.round(llyConverted),
                Math.round(urxAdjusted),
                Math.round(uryConverted),
                bestKy.page
        );
    }

    @Override
    public int getPdfPageCountFromBase64(String base64EncodedPdf) throws IOException {
        // Decode the Base64 string to byte array
        byte[] pdfBytes = Base64.getDecoder().decode(base64EncodedPdf);

        // Create a PDDocument instance from the byte array
        try (ByteArrayInputStream bis = new ByteArrayInputStream(pdfBytes);
             PDDocument document = PDDocument.load(bis)) {

            // Get the number of pages in the PDF
            PDPageTree pages = document.getPages();
            return pages.getCount();
        }
    }

    public static class SignatureCoordinates {
        public int llx, lly, urx, ury;
        public int page;

        public SignatureCoordinates(int llx, int lly, int urx, int ury, int page) {
            this.llx = llx;
            this.lly = lly;
            this.urx = urx;
            this.ury = ury;
            this.page = page;
        }
    }
}