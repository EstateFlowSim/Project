package com.example.home.domain.house.client;

import com.example.home.domain.house.dto.AptDealResponse;
import com.example.home.global.exception.BusinessException;
import com.example.home.global.exception.docs.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MolitApiClient {

    private static final String MOLIT_HOST = "apis.data.go.kr";
    private static final String MOLIT_PATH =
            "/1613000/RTMSDataSvcAptTradeDev/getRTMSDataSvcAptTradeDev";
    private static final int NUM_OF_ROWS = 100;

    @Value("${molit.api.service-key}")
    private String serviceKey;

    private final WebClient webClient;

    public MolitApiClient(WebClient.Builder webClientBuilder) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.webClient = webClientBuilder.exchangeStrategies(strategies).build();
    }

    public List<AptDealResponse> fetchDeals(String regionCode, String yearMonth) {
        try {
            // 1페이지 먼저 호출해서 totalCount 파악
            ParsedPage first = fetchPage(regionCode, yearMonth, 1);
            List<AptDealResponse> result = new ArrayList<>(first.items());

            int totalPages = (int) Math.ceil((double) first.totalCount() / NUM_OF_ROWS);
            for (int page = 2; page <= totalPages; page++) {
                result.addAll(fetchPage(regionCode, yearMonth, page).items());
            }

            log.info("국토부 API 조회 완료: regionCode={}, yearMonth={}, total={}, pages={}",
                    regionCode, yearMonth, first.totalCount(), totalPages);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("국토부 API 호출 실패: regionCode={}, yearMonth={}", regionCode, yearMonth, e);
            throw new BusinessException(ErrorCode.MOLIT_API_ERROR);
        }
    }

    private ParsedPage fetchPage(String regionCode, String yearMonth, int pageNo) {
        int pageNum = pageNo;
        String xml = webClient.get()
                .uri(b -> b.scheme("https").host(MOLIT_HOST).path(MOLIT_PATH)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("LAWD_CD", regionCode)
                        .queryParam("DEAL_YMD", yearMonth)
                        .queryParam("pageNo", pageNum)
                        .queryParam("numOfRows", NUM_OF_ROWS)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return parseXml(xml, regionCode);
    }

    private ParsedPage parseXml(String xml, String regionCode) {
        try {
            String sanitized = xml.replaceAll("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)", "&amp;");
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(sanitized)));

            int totalCount = toInt(textFromDoc(doc, "totalCount"));

            NodeList items = doc.getElementsByTagName("item");
            List<AptDealResponse> list = new ArrayList<>(items.getLength());
            for (int i = 0; i < items.getLength(); i++) {
                Element el = (Element) items.item(i);
                list.add(new AptDealResponse(
                        regionCode,
                        text(el, "umdNm"),
                        text(el, "aptNm"),
                        text(el, "jibun"),
                        toInt(text(el, "dealYear")),
                        toInt(text(el, "dealMonth")),
                        toInt(text(el, "dealDay")),
                        toDouble(text(el, "excluUseAr")),
                        toInt(text(el, "floor")),
                        toLong(text(el, "dealAmount").replaceAll("[,\\s]", "")),
                        toInt(text(el, "buildYear"))
                ));
            }
            return new ParsedPage(totalCount, list);
        } catch (Exception e) {
            log.error("MOLIT XML 파싱 실패", e);
            throw new BusinessException(ErrorCode.MOLIT_API_ERROR);
        }
    }

    private String textFromDoc(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : "0";
    }

    private String text(Element el, String tag) {
        NodeList nodes = el.getElementsByTagName(tag);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : "";
    }

    private int toInt(String s) {
        try { return s.isEmpty() ? 0 : Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private long toLong(String s) {
        try { return s.isEmpty() ? 0L : Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }

    private double toDouble(String s) {
        try { return s.isEmpty() ? 0.0 : Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }

    private record ParsedPage(int totalCount, List<AptDealResponse> items) {}
}
