package com.capstone.contractmanagement.services.translation;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import org.springframework.stereotype.Service;

@Service
public class TranslationService {
    private final Translate translate;

    public TranslationService() {
        // Khởi tạo client Google Translate
        this.translate = TranslateOptions.getDefaultInstance().getService();
    }

    public String translateText(String text, String targetLanguage) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Translation translation = translate.translate(
                text,
                Translate.TranslateOption.targetLanguage(targetLanguage)
        );
        return translation.getTranslatedText();
    }
}
