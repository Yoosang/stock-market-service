package com.usang.stockmarket.infra.kis;

import com.usang.stockmarket.application.stock.StockMasterRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * KIS(한국투자증권)가 매일 배포하는 종목 마스터파일(코스피/코스닥) 다운로드 + 파싱.
 * KIS 인증(approval_key)이 필요 없는 순수 정적 파일 다운로드라 KisAuthClient와는 무관하다.
 * 고정폭 파싱 오프셋은 실제 파일을 직접 받아 바이트 단위로 검증한 값이다:
 * 한 줄 = Part1(가변폭: 단축코드 9자 + 표준코드 12자 + 한글명) + Part2(고정폭 접미부).
 * Java의 readLine()은 줄바꿈을 제거하므로, 공식 예제(Python, 줄바꿈 포함 228/222자 기준)보다
 * 1자 적은 227자(코스피)/221자(코스닥)를 접미부 폭으로 써야 필드가 정확히 맞는다.
 */
@Component
@Slf4j
public class KisStockMasterClient {

    private static final String KOSPI_URL = "https://new.real.download.dws.co.kr/common/master/kospi_code.mst.zip";
    private static final String KOSDAQ_URL = "https://new.real.download.dws.co.kr/common/master/kosdaq_code.mst.zip";
    private static final int KOSPI_SUFFIX_WIDTH = 227;
    private static final int KOSDAQ_SUFFIX_WIDTH = 221;
    private static final int NAME_START = 21;
    // 증권그룹구분코드: ST=주권(일반주식), EF=ETF만 포함한다 (ETN 등 그 외 상품은 제외).
    private static final Set<String> INCLUDED_GROUP_CODES = Set.of("ST", "EF");
    private static final Charset MST_CHARSET = Charset.forName("MS949");

    private final RestClient restClient;

    public KisStockMasterClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<StockMasterRow> fetchKospi() {
        return fetch(KOSPI_URL, KOSPI_SUFFIX_WIDTH, "KOSPI");
    }

    public List<StockMasterRow> fetchKosdaq() {
        return fetch(KOSDAQ_URL, KOSDAQ_SUFFIX_WIDTH, "KOSDAQ");
    }

    private List<StockMasterRow> fetch(String url, int suffixWidth, String market) {
        byte[] zipBytes = restClient.get().uri(url).retrieve().body(byte[].class);
        if (zipBytes == null) {
            log.warn("Empty response downloading KIS stock master from {}", url);
            return List.of();
        }

        List<StockMasterRow> rows = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(zip, MST_CHARSET));
                String line;
                while ((line = reader.readLine()) != null) {
                    parseLine(line, suffixWidth, market).ifPresent(rows::add);
                }
            }
        } catch (IOException e) {
            log.error("Failed to download/parse KIS stock master from {}: {}", url, e.getMessage());
        }
        log.info("Fetched {} stocks from {}", rows.size(), market);
        return rows;
    }

    private Optional<StockMasterRow> parseLine(String line, int suffixWidth, String market) {
        if (line.length() <= suffixWidth + NAME_START) {
            return Optional.empty();
        }

        int nameEnd = line.length() - suffixWidth;
        String symbol = line.substring(0, 9).trim();
        String name = line.substring(NAME_START, nameEnd).trim();
        String groupCode = line.substring(nameEnd, nameEnd + 2);

        if (!INCLUDED_GROUP_CODES.contains(groupCode)) {
            return Optional.empty();
        }
        return Optional.of(new StockMasterRow(symbol, name, market));
    }
}
