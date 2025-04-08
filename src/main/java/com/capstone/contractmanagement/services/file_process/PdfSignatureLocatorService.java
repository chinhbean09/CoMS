package com.capstone.contractmanagement.services.file_process;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

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

        // Đọc PDF và thu thập text positions
        float pageHeight = 0;
        try (PDDocument document = PDDocument.load(inputStream)) {
            this.setStartPage(1);
            this.setEndPage(document.getNumberOfPages());
            this.getText(document);
            // Lấy chiều cao của trang 2
            pageHeight = document.getPage(1).getMediaBox().getHeight(); // Trang 2 (chỉ số bắt đầu từ 0)
            System.out.println("Chiều cao của trang 2: " + pageHeight);
        }

        // Xử lý từng trang để tìm cụm từ
        for (Map.Entry<Integer, List<TextPosition>> entry : pageTextPositions.entrySet()) {
            int page = entry.getKey();
            List<TextPosition> positions = orderTextPositions(entry.getValue());
            findMatches(page, positions, "ĐẠI DIỆN BÊN A", daiDienBenAList);
            findMatches(page, positions, "KÝ VÀ GHI RÕ HỌ TÊN", kyGhiRoHoTenList);
        }

        if (daiDienBenAList.isEmpty() || kyGhiRoHoTenList.isEmpty()) {
            return null;
        }

        // Log các vị trí của "ĐẠI DIỆN BÊN A"
        System.out.println("Các vị trí của ĐẠI DIỆN BÊN A:");
        for (MatchInfo daiDien : daiDienBenAList) {
            System.out.println("Page: " + daiDien.page + ", x: " + daiDien.x + ", y: " + daiDien.y);
        }

        // Log các vị trí của "KÝ VÀ GHI RÕ HỌ TÊN" trước khi sắp xếp
        System.out.println("Các vị trí của KÝ VÀ GHI RÕ HỌ TÊN (trước sắp xếp):");
        for (MatchInfo ky : kyGhiRoHoTenList) {
            System.out.println("Page: " + ky.page + ", x: " + ky.x + ", y: " + ky.y);
        }

        // Sắp xếp kyGhiRoHoTenList theo thứ tự từ dưới lên trên (y tăng dần)
        kyGhiRoHoTenList.sort((a, b) -> {
            if (a.page != b.page) {
                return Integer.compare(a.page, b.page);
            }
            return Float.compare(a.y, b.y); // Sắp xếp y tăng dần (từ dưới lên trên)
        });

        // Log các vị trí của "KÝ VÀ GHI RÕ HỌ TÊN" sau khi sắp xếp
        System.out.println("Các vị trí của KÝ VÀ GHI RÕ HỌ TÊN (sau sắp xếp):");
        for (MatchInfo ky : kyGhiRoHoTenList) {
            System.out.println("Page: " + ky.page + ", x: " + ky.x + ", y: " + ky.y);
        }

        // Tìm "ĐẠI DIỆN BÊN A" ở trang cuối cùng
        MatchInfo lastDaiDienBenA = daiDienBenAList.stream()
                .max(Comparator.comparingInt(a -> a.page))
                .orElse(null);

        if (lastDaiDienBenA == null) {
            return null;
        }

        System.out.println("ĐẠI DIỆN BÊN A được chọn - Page: " + lastDaiDienBenA.page + ", x: " + lastDaiDienBenA.x + ", y: " + lastDaiDienBenA.y);

        // Tìm "KÝ VÀ GHI RÕ HỌ TÊN" phù hợp
        MatchInfo bestKy = null;
        float closestYDistance = Float.MAX_VALUE; // Khoảng cách gần nhất đến "ĐẠI DIỆN BÊN A"
        float xThreshold = 50.0f; // Ngưỡng khoảng cách x để xác định "KÝ VÀ GHI RÕ HỌ TÊN" của bên A

        for (MatchInfo ky : kyGhiRoHoTenList) {
            // Trường hợp 1: Cùng trang với "ĐẠI DIỆN BÊN A"
            if (ky.page == lastDaiDienBenA.page && ky.y > lastDaiDienBenA.y) { // y lớn hơn nghĩa là bên dưới
                float yDistance = ky.y - lastDaiDienBenA.y;
                float xDistance = Math.abs(ky.x - lastDaiDienBenA.x);
                if (yDistance < closestYDistance && xDistance < xThreshold) { // Chỉ chọn nếu x gần với "ĐẠI DIỆN BÊN A"
                    closestYDistance = yDistance;
                    bestKy = ky;
                }
            }
            // Trường hợp 2: Trên trang tiếp theo
            else if (ky.page == lastDaiDienBenA.page + 1) {
                if (bestKy == null || ky.y < bestKy.y) { // Ưu tiên y nhỏ nhất trên trang tiếp theo (đầu trang)
                    bestKy = ky;
                    closestYDistance = Float.MAX_VALUE; // Reset khoảng cách vì ưu tiên trang tiếp theo
                }
            }
        }

        if (bestKy == null) {
            // Nếu không tìm thấy "KÝ VÀ GHI RÕ HỌ TÊN" phù hợp, trả về tọa độ trên "ĐẠI DIỆN BÊN A"
            System.out.println("Không tìm thấy KÝ VÀ GHI RÕ HỌ TÊN phù hợp, trả về tọa độ trên ĐẠI DIỆN BÊN A");
            float llyFallback = pageHeight - (lastDaiDienBenA.y + 10); // Dịch xuống 10 đơn vị
            float uryFallback = pageHeight - (lastDaiDienBenA.y + 60); // Dịch xuống 60 đơn vị
            float llxFallback = lastDaiDienBenA.x - 30; // Di chuyển sang trái 30 đơn vị
            float heightFallback = llyFallback - uryFallback; // Chiều cao
            float widthFallback = (4.0f / 3.0f) * heightFallback; // Chiều rộng theo tỉ lệ 3:4
            return new SignatureCoordinates(
                    llxFallback,
                    llyFallback,
                    llxFallback + widthFallback,
                    uryFallback,
                    lastDaiDienBenA.page
            );
        }

        System.out.println("KÝ VÀ GHI RÕ HỌ TÊN được chọn - Page: " + bestKy.page + ", x: " + bestKy.x + ", y: " + bestKy.y);

        // Tọa độ ban đầu (trong hệ tọa độ PDFBox: y = 0 ở đầu trang)
        float lly = bestKy.y + 10; // Dịch xuống 10 đơn vị
        float ury = bestKy.y + 60; // Dịch xuống 60 đơn vị (chiều cao 50 đơn vị)

        // Chuyển đổi tọa độ sang hệ tọa độ ngược (y = 0 ở đáy trang, y tăng khi đi lên)
        float llyConverted = pageHeight - lly;
        float uryConverted = pageHeight - ury;

        // Di chuyển sang trái 30 đơn vị
        float llxAdjusted = bestKy.x - 30;

        // Tính chiều cao và chiều rộng theo tỉ lệ 3:4
        float height = llyConverted - uryConverted; // Chiều cao trong hệ tọa độ ngược
        float width = (4.0f / 3.0f) * height; // Chiều rộng theo tỉ lệ 3:4
        float urxAdjusted = 277;

        // Trả về tọa độ đã điều chỉnh
        return new SignatureCoordinates(
                llxAdjusted,
                llyConverted,
                urxAdjusted,
                uryConverted,
                bestKy.page
        );
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