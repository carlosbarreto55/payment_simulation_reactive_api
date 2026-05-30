package checkout.config.infrastructure;

import checkout.boundedcontext.customer.domain.Document;
import checkout.common.enums.DocumentType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
class DocumentWriteConverter implements Converter<Document, String> {

    @Override
    public String convert(Document source) {
        return source.documentType().name() + ":" + source.documentNumber().replaceAll("\\D", "");
    }
}

@ReadingConverter
class DocumentReadConverter implements Converter<String, Document> {

    @Override
    public Document convert(String source) {
        int colonIndex = source.indexOf(':');
        if (colonIndex <= 0 || colonIndex >= source.length() - 1) {
            throw new IllegalArgumentException("Invalid document format, explicit type required");
        }
        String typeStr = source.substring(0, colonIndex);
        String number = source.substring(colonIndex + 1);
        DocumentType type = DocumentType.valueOf(typeStr);
        return Document.from(type, number);
    }
}
